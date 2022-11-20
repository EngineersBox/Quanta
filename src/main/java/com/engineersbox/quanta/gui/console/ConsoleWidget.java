package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.debug.VariableHooksState;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.gui.format.ColouredString;
import com.engineersbox.quanta.gui.format.GUITextColour;
import com.engineersbox.quanta.debug.hooks.*;
import com.engineersbox.quanta.input.DebouncedKeyCapture;
import com.engineersbox.quanta.input.KeyState;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.reflect.TypeConversionUtils;
import com.engineersbox.quanta.utils.reflect.VarHandleUtils;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;

public class ConsoleWidget implements IGUIInstance {

    private static final Logger LOGGER = LogManager.getLogger(ConsoleWidget.class);

    private static final int COMMAND_LOG_SIZE = 100;
    private static final boolean COMMAND_LOG_AUTO_SCROLL = true;
    private static final boolean SHOW_TIMESTAMP = true;
    private static final String VARIABLE_INSTANCE_TARGET_DELIMITER = "::";
    private static final int INPUT_FIELD_FLAGS = ImGuiInputTextFlags.CallbackHistory
            | ImGuiInputTextFlags.CallbackCompletion
            | ImGuiInputTextFlags.EnterReturnsTrue;

    private static final ColouredString ERROR_MESSAGE_PREFIX = new ColouredString(GUITextColour.RED, "Variable validation failed: ");
    private static final ColouredString DEFAULT_NONE_ERROR_MESSAGE = new ColouredString(GUITextColour.NORMAL, "<NONE>");

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss.SSSS");
    private final ImString rawConsoleInput;
    private String consoleInput;
    private final LinkedBlockingDeque<ExecutedCommand> commandLog;
    private List<String> previousCommands;
    private boolean historyTraversable;
    private int previousCommandSelection;
    /**
     * TRUE = forward FALSE = backward
     */
    private boolean previousCommandSelectionDirection;
    private boolean scrollToBottom;
    private boolean wasPrevFrameTabCompletion;
    private final List<String> commandSuggestions; // TODO: Implement suggestions
    private boolean updateReadOnlyFlag;
    private final DebouncedKeyCapture upKeyCapture = new DebouncedKeyCapture(GLFW_KEY_UP)
            .withOnPressHandler(() -> {
                this.previousCommandSelectionDirection = true;
                updatePreviousCommandSelection();
            });
    private final DebouncedKeyCapture downKeyCapture = new DebouncedKeyCapture(GLFW_KEY_DOWN)
            .withOnPressHandler(() -> {
                this.previousCommandSelectionDirection = false;
                updatePreviousCommandSelection();
            });

    public ConsoleWidget() {
        this.rawConsoleInput = new ImString();
        this.consoleInput = "";
        this.commandLog = new LinkedBlockingDeque<>(ConsoleWidget.COMMAND_LOG_SIZE);
        this.wasPrevFrameTabCompletion = false;
        this.scrollToBottom = false;
        this.commandSuggestions = new ArrayList<>();
        this.previousCommands = null;
        this.historyTraversable = false;
        this.previousCommandSelection = -1;
        this.previousCommandSelectionDirection = true;
        this.updateReadOnlyFlag = false;
    }

    private void submitCommand(final ExecutedCommand executedCommand) {
        if (this.commandLog.size() >= ConsoleWidget.COMMAND_LOG_SIZE) {
            this.commandLog.pop();
        }
        this.commandLog.addLast(executedCommand);
    }

    private Pair<ValidationState, Object> invokeValidator(final Method method,
                                                          final String value) {
        if (method == null) {
            return ImmutablePair.of(
                    new ValidationState(true, new ColouredString[0]),
                    value
            );
        }
        method.setAccessible(true);
        final Function<Exception, Pair<ValidationState, Object>> exceptionHandler = (final Exception e) -> {
            ConsoleWidget.LOGGER.error("Unable to invoke validator {}", method.getName(), e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().toString();
            }
            return ImmutablePair.of(
                    new ValidationState(
                            false,
                            new ColouredString[]{
                                    new ColouredString(GUITextColour.RED, "Error invoking variable validator: "),
                                    new ColouredString(GUITextColour.NORMAL, message)
                            }
                    ),
                    null
            );
        };
        try {
            final Object result = method.invoke(null, value);
            return ImmutablePair.of(
                    new ValidationState(true, new ColouredString[0]),
                    result
            );
        } catch (final InvocationTargetException e) {
            if (e.getCause() == null || !(e.getCause() instanceof HookValidationException hookValidationException)) {
                return exceptionHandler.apply(e);
            }
            ColouredString[] formattedMessage = hookValidationException.getFormattedMessage();
            if (formattedMessage == null || formattedMessage.length < 1) {
                formattedMessage = new ColouredString[]{ConsoleWidget.DEFAULT_NONE_ERROR_MESSAGE};
            }
            return ImmutablePair.of(
                    new ValidationState(
                            false,
                            ArrayUtils.addFirst(
                                    formattedMessage,
                                    ConsoleWidget.ERROR_MESSAGE_PREFIX
                            )
                    ),
                    null
            );
        } catch (final Exception e) {
            return exceptionHandler.apply(e);
        }
    }

    private ValidationState updateVariableValue(final String path,
                                                final String value) {
        final String[] instanceTarget = path.split(ConsoleWidget.VARIABLE_INSTANCE_TARGET_DELIMITER);
        final ValidationState invalidState = new ValidationState(
                false,
                new ColouredString[]{
                        new ColouredString(GUITextColour.RED, "Unknown variable: "),
                        new ColouredString(GUITextColour.NORMAL, path)
                }
        );
        final HookBinding hookBinding;
        try {
            hookBinding = VariableHooksState.HOOKS.get(instanceTarget[0]);
        } catch (final NoSuchElementException e) {
            return invalidState;
        }
        final boolean requiresInstance = hookBinding.requiresInstance();
        Object matchingInstance = null;
        if (requiresInstance) {
            if (instanceTarget.length != 2) {
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(GUITextColour.RED, "Instance ID is required to update variable: "),
                                new ColouredString(GUITextColour.YELLOW, path)
                        }
                );
            }
            final Optional<Object> potentialMatchingInstance = VariableHooksState.getFieldParentInstance(hookBinding.field(), instanceTarget[1]);
            if (potentialMatchingInstance.isEmpty()) {
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(GUITextColour.RED, "Variable "),
                                new ColouredString(GUITextColour.YELLOW, instanceTarget[0]),
                                new ColouredString(GUITextColour.RED, " is not registered to an instance with ID "),
                                new ColouredString(GUITextColour.YELLOW, instanceTarget[1]),
                        }
                );
            }
            matchingInstance = potentialMatchingInstance.get();
        }
        final Pair<ValidationState, Object> state = invokeValidator(hookBinding.validator(), value);
        final boolean isError = state.getKey().message().length != 0;
        if (!state.getKey().state() && isError) {
            return state.getKey();
        }
        final VarHandle varHandle = hookBinding.varHandle();
        final Object valueToWrite;
        if (isError) {
            valueToWrite = state.getValue();
        } else {
            try {
                valueToWrite = TypeConversionUtils.tryCoercePrimitive(
                        varHandle.varType(),
                        state.getValue()
                );
            } catch (final IllegalArgumentException e) {
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(GUITextColour.RED, "Invalid variable value \""),
                                new ColouredString(GUITextColour.YELLOW, state.getValue().toString()),
                                new ColouredString(GUITextColour.RED, "\", expected type "),
                                new ColouredString(GUITextColour.CYAN, hookBinding.field().getName()),
                        }
                );
            }
        }
        synchronized (this) {
            VarHandleUtils.setValue(
                    varHandle,
                    matchingInstance,
                    valueToWrite,
                    requiresInstance
            );
        }
        return new ValidationState(
                true,
                new ColouredString[]{
                        new ColouredString(GUITextColour.NORMAL, "Variable "),
                        new ColouredString(GUITextColour.CYAN, path),
                        new ColouredString(GUITextColour.NORMAL, " updated to "),
                        new ColouredString(GUITextColour.YELLOW, value)
                }
        );
    }

    private ColouredString[] listAllVars() {
        return VariableHooksState.resolveVariableHooksFields(false).flatMap((final Field field) -> {
            final VariableHook annotation = field.getAnnotation(VariableHook.class);
            if (Modifier.isStatic(field.getModifiers())) {
                return Stream.of(new ColouredString(
                        GUITextColour.NORMAL,
                        String.format(
                                " - [%s] %s%n",
                                field.getType().getSimpleName(),
                                annotation.name()
                        )
                ));
            }
            return VariableHooksState.FIELD_INSTANCE_MAPPINGS.get(field)
                    .stream()
                    .map((final Object instance) -> new ColouredString(
                            GUITextColour.NORMAL,
                            String.format(
                                    " - [%s] %s::%s%n",
                                    field.getType().getSimpleName(),
                                    annotation.name(),
                                    InstanceIdentifierProvider.deriveInstanceID(instance)
                            )
                    ));
        }).toArray(ColouredString[]::new);
    }

    private void handleUnknownCommand() {
        submitCommand(ExecutedCommand.from(
                GUITextColour.NORMAL.with(this.consoleInput),
                new ColouredString[]{
                        GUITextColour.RED.with("Unknown command: "),
                        GUITextColour.NORMAL.with(this.consoleInput)
                }
        ));
    }

    private static final Map<String, Pair<String, TriConsumer<ConsoleWidget, String, String[]>>> COMMAND_HANDLERS = new HashMap<>();

    static {
        // TODO: Improve this to accept arbitrary command registration
        ConsoleWidget.COMMAND_HANDLERS.put("set", ImmutablePair.of("set <variable> <value>", (final ConsoleWidget widget, final String command, final String[] args) -> {
            if (args.length != 2) {
                widget.submitCommand(ExecutedCommand.from(
                        new ColouredString[]{
                                GUITextColour.GREEN.with(command + " "),
                                GUITextColour.NORMAL.with(String.join(" ", args))
                        },
                        new ColouredString[]{
                                GUITextColour.RED.with("Expected 2 arguments, got " + args.length + ": "),
                                GUITextColour.YELLOW.with(Arrays.toString(args))
                        }
                ));
                return;
            }
            final ValidationState state = widget.updateVariableValue(args[0], args[1]);
            widget.submitCommand(ExecutedCommand.from(
                    new ColouredString[]{
                            GUITextColour.GREEN.with(command + " "),
                            GUITextColour.NORMAL.with(String.join(" ", args))
                    },
                    state.message()
            ));
        }));
        ConsoleWidget.COMMAND_HANDLERS.put("get", ImmutablePair.of("get <variable>", (final ConsoleWidget widget, final String command, final String[] args) -> widget.submitCommand(ExecutedCommand.from(
                new ColouredString[]{
                        GUITextColour.GREEN.with(command + " "),
                        GUITextColour.NORMAL.with(String.join(" ", args))
                },
                GUITextColour.NORMAL.with("get stuff!")
        ))));
        ConsoleWidget.COMMAND_HANDLERS.put("listvars", ImmutablePair.of("listvars", (final ConsoleWidget widget, final String command, final String[] args) -> {
            if (args.length > 1) {
                widget.handleUnknownCommand();
            }
            widget.submitCommand(ExecutedCommand.from(
                    GUITextColour.GREEN.with(command),
                    widget.listAllVars()
            ));
        }));
        ConsoleWidget.COMMAND_HANDLERS.put("help", ImmutablePair.of("help", (final ConsoleWidget widget, final String command, final String[] args) -> {
            final ColouredString[] commandsSyntax = ConsoleWidget.COMMAND_HANDLERS.values()
                    .stream()
                    .map(Pair::getLeft)
                    .map((final String commandSyntax) -> String.format(
                            " - %s%s",
                            commandSyntax,
                            ColouredString.NEWLINE_CHARACTER
                    )).map(GUITextColour.NORMAL::with)
                    .toArray(ColouredString[]::new);
            widget.submitCommand(ExecutedCommand.from(
                    GUITextColour.GREEN.with(command),
                    commandsSyntax
            ));
        }));
    }

    private void handleCommand() {
        if (this.consoleInput.isBlank()) {
            return;
        }
        final String[] splitCommand = this.consoleInput.split("\\s");
        if (splitCommand.length < 1) {
            return;
        }
        final String command = splitCommand[0];
        final String[] args = ArrayUtils.subarray(splitCommand, 1, splitCommand.length);
        final Pair<String, TriConsumer<ConsoleWidget, String, String[]>> commandHandler = ConsoleWidget.COMMAND_HANDLERS.get(command);
        if (commandHandler == null) {
            handleUnknownCommand();
        } else {
            commandHandler.getRight().accept(this, command, args);
        }
    }

    private void executedCommandLog() {
        final float footerHeightToReserve = ImGui.getStyle().getItemSpacingY() + ImGui.getFrameHeightWithSpacing();
        if (!ImGui.beginChild("ScrollRegion##", 0, -footerHeightToReserve, false, 0)) {
            return;
        }
        int count = 0;
        ImGui.pushTextWrapPos();
        for (final ExecutedCommand command : this.commandLog) {
            final ImVec2 dimensions = new ImVec2();
            ImGui.calcTextSize(dimensions, "00:00:00:0000");
            final float timestampWidth = dimensions.x;
            ImGui.pushTextWrapPos(ImGui.getColumnWidth() - timestampWidth);
            if (count++ != 0) {
                ImGui.dummy(-1, ImGui.getFontSize());
            }
            ColouredString.renderFormattedString(ArrayUtils.addFirst(command.command(), GUITextColour.CYAN.with("> ")));
            if (ConsoleWidget.SHOW_TIMESTAMP) {
                ImGui.popTextWrapPos();
                ImGui.sameLine(ImGui.getColumnWidth(-1) - timestampWidth);
                ImGui.pushStyleColor(ImGuiCol.Text, GUITextColour.GRAY.getColour());
                ImGui.text(this.dateFormatter.format(command.date()));
                ImGui.popStyleColor();
            }
            ColouredString.renderFormattedString(command.result());
        }
        ImGui.popTextWrapPos();
        if (this.scrollToBottom && (ImGui.getScrollY() >= ImGui.getScrollMaxY() || ConsoleWidget.COMMAND_LOG_AUTO_SCROLL)) {
            ImGui.setScrollHereY(1.0f);
        }
        this.scrollToBottom = false;
        ImGui.endChild();
    }

    private void setHistoryInInput() {
        if (this.previousCommands != null
                && !this.previousCommands.isEmpty()
                && this.previousCommandSelection < this.previousCommands.size()) {
            this.updateReadOnlyFlag = true;
            this.rawConsoleInput.set(this.previousCommands.get(this.previousCommands.size() - 1 - this.previousCommandSelection));
        }
    }

    private void inputField() {
        boolean reclaimFocus = false;
        ImGui.pushItemWidth(-ImGui.getStyle().getItemSpacingX() * 7);
        if (ImGui.inputTextWithHint("##", "Type \"help\" for help", this.rawConsoleInput, ConsoleWidget.INPUT_FIELD_FLAGS | (this.updateReadOnlyFlag ? ImGuiInputTextFlags.ReadOnly : 0))) {
            if (!this.rawConsoleInput.isEmpty()) {
                this.consoleInput = this.rawConsoleInput.get();
                handleCommand();
                this.scrollToBottom = true;
            }
            this.previousCommandSelection = -1;
            this.historyTraversable = false;
            reclaimFocus = true;
            this.rawConsoleInput.clear();
        }
        // Previous command selection
        if (ImGui.isItemActive() && this.historyTraversable && this.previousCommands != null) {
            setHistoryInInput();
        } else {
            this.updateReadOnlyFlag = false;
            this.previousCommands = null;
            this.historyTraversable = false;
            this.previousCommandSelectionDirection = true;
        }
        ImGui.popItemWidth();
        if (ImGui.isItemEdited() && !this.wasPrevFrameTabCompletion) {
            this.commandSuggestions.clear();
        }
        this.wasPrevFrameTabCompletion = false;
        ImGui.setItemDefaultFocus();
        if (reclaimFocus) {
            ImGui.setKeyboardFocusHere(-1);
        }
    }

    @Override
    public void drawGUI() {
        ImGui.begin("Console");
        executedCommandLog();
        ImGui.separator();
        inputField();
        ImGui.end();
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        final ImGuiIO imGuiIO = ImGui.getIO();
        if (!imGuiIO.getWantCaptureMouse() && !imGuiIO.getWantCaptureKeyboard()) {
            return false;
        }
        if (upKeyCapture.update(window) != KeyState.PRESSED
            && downKeyCapture.update(window) != KeyState.PRESSED
            && this.updateReadOnlyFlag) {
            this.updateReadOnlyFlag = false;
        }
        return true;
    }

    private void updatePreviousCommandSelection() {
        this.historyTraversable = true;
        if (this.previousCommands == null) {
            this.previousCommands = this.commandLog.stream()
                    .map(ExecutedCommand::joinedCommand)
                    .toList();
        }
        this.previousCommandSelection = Math.max(Math.min(
                this.previousCommandSelection + (this.previousCommandSelectionDirection ? 1 : -1),
                this.previousCommands.size() - 1
        ), 0);
        setHistoryInInput();
    }
}
