package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.gui.console.format.ColouredString;
import com.engineersbox.quanta.gui.console.format.ConsoleColour;
import com.engineersbox.quanta.gui.console.hooks.*;
import com.engineersbox.quanta.gui.console.tree.VariableTree;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.TypeConversionUtils;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStorage;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.internal.flag.ImGuiItemFlags;
import imgui.type.ImString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import sun.misc.Unsafe;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.*;

public class ConsoleWidget implements IGUIInstance {

    private static final Logger LOGGER = LogManager.getLogger(ConsoleWidget.class);
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.FieldsAnnotated, Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );

    public static final Map<Field, List<Object>> FIELD_INSTANCE_MAPPINGS = new HashMap<>();
    private static final String VARIABLE_PATH_DELIMITER = ".";
    private static final VariableTree<HookBinding> HOOKS = new VariableTree<>(ConsoleWidget.VARIABLE_PATH_DELIMITER);

    static {
        final Map<VariableHook, Pair<Field, Boolean>> variableHooks = ConsoleWidget.resolveVariableHooks();
        final Map<String, Method> hookValidators = ConsoleWidget.resolveHookValidators();
        int count = 0;
        for (final Map.Entry<VariableHook, Pair<Field, Boolean>> entry : variableHooks.entrySet()) {
            final String name = entry.getKey().name();
            final Pair<Field, Boolean> hookValue = entry.getValue();
            if (name.isBlank()) {
                ConsoleWidget.LOGGER.warn("Invalid variable hook name for field [{}]", hookValue.getLeft().getName());
                continue;
            }
            final String validatorName = entry.getKey().hookValidator();
            Method hookValidator = null;
            if (!Objects.equals(validatorName, "")) {
                hookValidator = hookValidators.get(validatorName);
            }
            if (hookValidator != null && !ConsoleWidget.validateValidatorArgs(
                    name,
                    validatorName,
                    hookValidator
            )) {
                continue;
            }
            ConsoleWidget.HOOKS.insert(
                    entry.getKey().name(),
                    new HookBinding(
                            hookValue.getKey(),
                            hookValue.getValue(),
                            hookValidator
                    )
            );
            count++;
        }
        ConsoleWidget.LOGGER.debug("Resolved {} variable hook(s)", count);
    }

    private static boolean validateValidatorArgs(final String varHookName,
                                                 final String validatorName,
                                                 final Method method) {
        final int parameterCount = method.getParameterCount();
        if (parameterCount != 1) {
            ConsoleWidget.LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected 1 parameter, got {}",
                    validatorName,
                    varHookName,
                    parameterCount
            );
            return false;
        }
        final Class<?> parameterType = method.getParameterTypes()[0];
        if (!parameterType.isAssignableFrom(String.class)) {
            ConsoleWidget.LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected parameter of type {}, got {}",
                    validatorName,
                    varHookName,
                    String.class.getName(),
                    parameterType.getName()
            );
            return false;
        }
        return true;
    }

    private static Stream<Field> resolveVariableHooksFields(final boolean logBadState) {
        return ConsoleWidget.REFLECTIONS.getFieldsAnnotatedWith(VariableHook.class)
                .stream()
                .filter((final Field field) -> {
                    if (!HookBinding.validateField(field)) {
                        if (logBadState) {
                            ConsoleWidget.LOGGER.error(
                                    "Invalid @VariableHook usage. Field [{}] is primitive [{}] and marked as [static final] which is inlined during compilation. Field cannot be altered at runtime, skipping.",
                                    field,
                                    field.getType().getSimpleName()
                            );
                        }
                        return false;
                    } else if (Modifier.isStatic(field.getModifiers())) {
                        return true;
                    }
                    return ConsoleWidget.hasConstructorWithRegistrationWrapper(field);
                });
    }

    private static Map<VariableHook, Pair<Field, Boolean>> resolveVariableHooks() {
        return ConsoleWidget.resolveVariableHooksFields(true)
                .collect(Collectors.toMap(
                        (final Field field) -> field.getAnnotation(VariableHook.class),
                        (final Field field) -> ImmutablePair.of(
                                field,
                                !Modifier.isStatic(field.getModifiers())
                        )
                ));
    }

    private static boolean hasConstructorWithRegistrationWrapper(final Field field) {
        final Constructor<?>[] constructors = field.getDeclaringClass().getDeclaredConstructors();
        if (constructors.length < 1) {
            return false;
        }
        return Arrays.stream(constructors)
                .anyMatch((final Constructor<?> constructor) -> constructor.isAnnotationPresent(RegisterInstanceVariableHooks.class));
    }

    private static Map<String, Method> resolveHookValidators() {
        return ConsoleWidget.REFLECTIONS.getMethodsAnnotatedWith(HookValidator.class)
                .stream()
                .filter((final Method method) -> {
                    final StringBuilder errorBuilder = new StringBuilder();
                    final boolean isStatic = Modifier.isStatic(method.getModifiers());
                    if (!isStatic) {
                        errorBuilder.append("\n\t- Not declared static");
                    }
                    final boolean isBooleanReturn = method.getReturnType().isAssignableFrom(Object.class);
                    if (!isBooleanReturn) {
                        errorBuilder.append("\n\t- Does not return an object");
                    }
                    final boolean acceptsSingleObjectArgument = method.getParameterTypes().length == 1
                            || method.getParameterTypes()[0].isAssignableFrom(String.class);
                    if (!acceptsSingleObjectArgument) {
                        errorBuilder.append("\n\t- Does not accept single string argument");
                    }
                    if (!isStatic || !isBooleanReturn || !acceptsSingleObjectArgument) {
                        ConsoleWidget.LOGGER.error(
                                "Invalid hook validator [{}]:{}",
                                method.getName(),
                                errorBuilder
                        );
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(
                        (final Method method) -> method.getAnnotation(HookValidator.class).name(),
                        Function.identity()
                ));
    }

    private static final int COMMAND_LOG_SIZE = 100;
    private static final boolean COMMAND_LOG_AUTO_SCROLL = true;
    private static final boolean SHOW_TIMESTAMP = true;
    private static final String VARIABLE_INSTANCE_TARGET_DELIMITER = "::";
    private static final int INPUT_FIELD_FLAGS = ImGuiInputTextFlags.CallbackHistory
            | ImGuiInputTextFlags.CallbackCompletion
            | ImGuiInputTextFlags.EnterReturnsTrue;

    private static final ColouredString ERROR_MESSAGE_PREFIX = new ColouredString(ConsoleColour.RED, "Variable validation failed: ");
    private static final ColouredString DEFAULT_NONE_ERROR_MESSAGE = new ColouredString(ConsoleColour.NORMAL, "<NONE>");
    private static final String NEWLINE_CHARACTER = System.getProperty("line.separator");

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss.SSSS");
    private final ImString rawConsoleInput; // TODO: Implement usage of up/down arrow keys to access previously executed commands
    private String consoleInput;
    private final LinkedBlockingDeque<ExecutedCommand> commandLog;
    private List<String> previousCommands;
    private boolean historyTraversable;
    private int previousCommandSelection;
    /**
     * TRUE = forward
     * FALSE = backward
     */
    private boolean previousCommandSelectionDirection;
    private boolean scrollToBottom;
    private boolean wasPrevFrameTabCompletion;
    private final List<String> commandSuggestions; // TODO: Implement suggestions

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
                                    new ColouredString(ConsoleColour.RED, "Error invoking variable validator: "),
                                    new ColouredString(ConsoleColour.NORMAL, message)
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

    @SuppressWarnings({"java:S3011"})
    private ValidationState updateVariableValue(final String path,
                                                final String value) {
        final String[] instanceTarget = path.split(ConsoleWidget.VARIABLE_INSTANCE_TARGET_DELIMITER);
        final ValidationState invalidState = new ValidationState(
                false,
                new ColouredString[]{
                        new ColouredString(ConsoleColour.RED, "Unknown variable: "),
                        new ColouredString(ConsoleColour.YELLOW, path)
                }
        );
        final HookBinding hookBinding;
        try {
            hookBinding = ConsoleWidget.HOOKS.get(instanceTarget[0]);
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
                                new ColouredString(ConsoleColour.RED, "Instance ID is required to update variable: "),
                                new ColouredString(ConsoleColour.YELLOW, path)
                        }
                );
            }
            final Optional<Object> potentialMatchingInstance = getFieldParentInstance(hookBinding.field(), instanceTarget[1]);
            if (potentialMatchingInstance.isEmpty()) {
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(ConsoleColour.RED, "Variable "),
                                new ColouredString(ConsoleColour.YELLOW, instanceTarget[0]),
                                new ColouredString(ConsoleColour.RED, " is not registered to an instance with ID "),
                                new ColouredString(ConsoleColour.YELLOW, instanceTarget[1]),
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
        final Field field = hookBinding.field();
        final Object valueToWrite;
        if (isError) {
            valueToWrite = state.getValue();
        } else {
            try {
                valueToWrite = TypeConversionUtils.tryCoercePrimitive(
                        field.getType(),
                        state.getValue()
                );
            } catch (final IllegalArgumentException e) {
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(ConsoleColour.RED, "Invalid variable value \""),
                                new ColouredString(ConsoleColour.YELLOW, state.getValue().toString()),
                                new ColouredString(ConsoleColour.RED, "\", expected type "),
                                new ColouredString(ConsoleColour.CYAN, field.getType().getSimpleName()),
                        }
                );
            }
        }
        synchronized (this) {
            try {
                lookupAndSetVariable(
                        field,
                        matchingInstance,
                        valueToWrite,
                        requiresInstance
                );
            } catch (final IllegalAccessException | NoSuchFieldException e) {
                ConsoleWidget.LOGGER.error("Unable to update variable", e);
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(ConsoleColour.RED, "Unable to update variable: "),
                                new ColouredString(ConsoleColour.YELLOW, path)
                        }
                );
            }
        }
        return new ValidationState(
                true,
                new ColouredString[]{
                        new ColouredString(ConsoleColour.NORMAL, "Variable "),
                        new ColouredString(ConsoleColour.CYAN, path),
                        new ColouredString(ConsoleColour.NORMAL, " updated to "),
                        new ColouredString(ConsoleColour.YELLOW, value)
                }
        );
    }

    private synchronized void lookupAndSetVariable(final Field field,
                                                   final Object instance,
                                                   final Object value,
                                                   final boolean requiresInstance) throws IllegalAccessException, NoSuchFieldException {
        final MethodHandles.Lookup lookup;
        if (Modifier.isPrivate(field.getModifiers())) {
            lookup = MethodHandles.privateLookupIn(
                    field.getDeclaringClass(),
                    MethodHandles.lookup()
            );
        } else {
            lookup = MethodHandles.lookup().in(field.getDeclaringClass());
        }
        if (requiresInstance) {
            lookup.findVarHandle(
                    field.getDeclaringClass(),
                    field.getName(),
                    field.getType()
            ).set(instance, value);
        } else {
            lookup.findStaticVarHandle(
                    field.getDeclaringClass(),
                    field.getName(),
                    field.getType()
            ).set(value);
        }
    }

    private Optional<Object> getFieldParentInstance(final Field field,
                                                    final String target) {
        final List<Object> instances = ConsoleWidget.FIELD_INSTANCE_MAPPINGS.get(field);
        return instances.stream()
                .filter((final Object instance) -> InstanceIdentifierProvider.deriveInstanceID(instance).equals(target))
                .findFirst();
    }

    private ColouredString[] listAllVars() {
        return ConsoleWidget.resolveVariableHooksFields(false).flatMap((final Field field) -> {
                    final VariableHook annotation = field.getAnnotation(VariableHook.class);
                    if (Modifier.isStatic(field.getModifiers())) {
                        return Stream.of(new ColouredString(
                                ConsoleColour.NORMAL,
                                String.format(
                                        " - [%s] %s%n",
                                        field.getType().getSimpleName(),
                                        annotation.name()
                                )
                        ));
                    }
                    return ConsoleWidget.FIELD_INSTANCE_MAPPINGS.get(field)
                            .stream()
                            .map((final Object instance) -> new ColouredString(
                                    ConsoleColour.NORMAL,
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
                ConsoleColour.NORMAL.with(this.consoleInput),
                new ColouredString[]{
                        ConsoleColour.RED.with("Unknown command: "),
                        ConsoleColour.NORMAL.with(this.consoleInput)
                }
        ));
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
        switch (command) {
            case "set" -> {
                final ValidationState state = updateVariableValue(args[0], args[1]);
                submitCommand(ExecutedCommand.from(
                        new ColouredString[]{
                                ConsoleColour.GREEN.with(command + " "),
                                ConsoleColour.NORMAL.with(String.join(" ", args))
                        },
                        state.message()
                ));
            }
            case "get" -> submitCommand(ExecutedCommand.from(
                    new ColouredString[]{
                            ConsoleColour.GREEN.with(command + " "),
                            ConsoleColour.NORMAL.with(String.join(" ", args))
                    },
                    ConsoleColour.NORMAL.with("get stuff!")
            ));
            case "listvars" -> {
                if (args.length > 1) {
                    handleUnknownCommand();
                }
                submitCommand(ExecutedCommand.from(
                        ConsoleColour.GREEN.with(command),
                        listAllVars()
                ));
            }
            default -> handleUnknownCommand();
        }
    }

    private void renderFormattedString(final ColouredString[] colouredStrings) {
        boolean onSameLine = false;
        boolean pushed = false;
        for (final ColouredString colouredString : colouredStrings) {
            if (onSameLine) {
                ImGui.sameLine(0, 0);
            }
            if (colouredString.colour() != null) {
                if (pushed) {
                    ImGui.popStyleColor();
                    pushed = false;
                }
                ImGui.pushStyleColor(
                        ImGuiCol.Text,
                        colouredString.colour().getColour()
                );
                pushed = true;
            }
            if (colouredString.value() != null) {
                ImGui.text(colouredString.value());
                onSameLine = !colouredString.value().endsWith(NEWLINE_CHARACTER);
            } else {
                onSameLine = true;
            }
        }
        if (pushed) {
            ImGui.popStyleColor();
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
            renderFormattedString(ArrayUtils.addFirst(command.command(), ConsoleColour.CYAN.with("> ")));
            if (ConsoleWidget.SHOW_TIMESTAMP) {
                ImGui.popTextWrapPos();
                ImGui.sameLine(ImGui.getColumnWidth(-1) - timestampWidth);
                ImGui.pushStyleColor(ImGuiCol.Text, ConsoleColour.GRAY.getColour());
                ImGui.text(this.dateFormatter.format(command.date()));
                ImGui.popStyleColor();
            }
            renderFormattedString(command.result());
        }
        ImGui.popTextWrapPos();
        if (this.scrollToBottom && (ImGui.getScrollY() >= ImGui.getScrollMaxY() || ConsoleWidget.COMMAND_LOG_AUTO_SCROLL)) {
            ImGui.setScrollHereY(1.0f);
        }
        this.scrollToBottom = false;
        ImGui.endChild();
    }

    private boolean updateReadOnlyFlag = false;

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
        if (ImGui.inputTextWithHint("##", "Type \"help\" for help", this.rawConsoleInput, ConsoleWidget.INPUT_FIELD_FLAGS | (updateReadOnlyFlag ? ImGuiInputTextFlags.ReadOnly : 0))) {
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

    private boolean upKeyPressed = false;
    private boolean downKeyPressed = false;

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        final ImGuiIO imGuiIO = ImGui.getIO();
        if (!imGuiIO.getWantCaptureMouse() && !imGuiIO.getWantCaptureKeyboard()) {
            return false;
        }
        if (window.isKeyPressed(GLFW_KEY_UP) && !this.upKeyPressed) {
            this.upKeyPressed = true;
            this.previousCommandSelectionDirection = true;
            updatePreviousCommandSelection();
        } else if (window.isKeyPressed(GLFW_KEY_DOWN) && !this.downKeyPressed) {
            this.downKeyPressed = true;
            this.previousCommandSelectionDirection = false;
            updatePreviousCommandSelection();
        } else if (this.updateReadOnlyFlag) {
            this.updateReadOnlyFlag = false;
        }
        if (window.isKeyReleased(GLFW_KEY_UP) && this.upKeyPressed) {
            this.upKeyPressed = false;
        } else if (window.isKeyReleased(GLFW_KEY_DOWN) && this.downKeyPressed) {
            this.downKeyPressed = false;
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
