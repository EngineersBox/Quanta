package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.gui.console.hooks.HookValidator;
import com.engineersbox.quanta.gui.console.hooks.InstanceIdentifierProvider;
import com.engineersbox.quanta.gui.console.hooks.RegisterVariableMembers;
import com.engineersbox.quanta.gui.console.hooks.VariableHook;
import com.engineersbox.quanta.gui.console.format.ColouredString;
import com.engineersbox.quanta.gui.console.format.ConsoleColour;
import com.engineersbox.quanta.gui.console.tree.TreeNodeLabel;
import com.engineersbox.quanta.gui.console.tree.TreeUtils;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.scene.Scene;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import javax.swing.*;
import javax.swing.tree.*;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT;

public class Console implements IGUIInstance {

    private static final Logger LOGGER = LogManager.getLogger(Console.class);
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.FieldsAnnotated, Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );

    public static final Map<Field, List<Object>> FIELD_INSTANCE_MAPPINGS = new HashMap<>();
    private static final String ROOT_NODE_VALUE = "@__INTERNAL_TREE_ROOT__";
    private static final DefaultTreeModel HOOKS = new DefaultTreeModel(new DefaultMutableTreeNode(ROOT_NODE_VALUE));

    static {
        final Map<VariableHook, Pair<Field, Boolean>> variableHooks = Console.resolveVariableHooks();
        final Map<String, Method> hookValidators = Console.resolveHookValidators();
        int count = 0;
        for (final Map.Entry<VariableHook, Pair<Field, Boolean>> entry : variableHooks.entrySet()) {
            final String name = entry.getKey().name();
            final Pair<Field, Boolean> hookValue = entry.getValue();
            if (name.isBlank()) {
                Console.LOGGER.warn("Invalid variable hook name for field [{}]", hookValue.getLeft().getName());
                continue;
            }
            final String[] path = entry.getKey().name().split("\\.");
            final String validatorName = entry.getKey().hookValidator();
            Method hookValidator = null;
            if (!Objects.equals(validatorName, "")) {
                hookValidator = hookValidators.get(validatorName);
            }
            if (hookValidator != null && !Console.validateValidatorArgs(
                    name,
                    validatorName,
                    hookValidator
            )) {
                continue;
            }
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) Console.HOOKS.getRoot();
            for (final String pathNode : path) {
                final TreeNodeLabel label = new TreeNodeLabel(pathNode);
                final DefaultMutableTreeNode next = new DefaultMutableTreeNode(label);
                current.add(next);
                current = next;
            }
            current.add(new DefaultMutableTreeNode(ImmutablePair.of(
                    hookValue,
                    hookValidator
            )));
            count++;
        }
        Console.LOGGER.debug("Resolved {} variable hooks", count);
    }

    private static boolean validateValidatorArgs(final String varHookName,
                                                 final String validatorName,
                                                 final Method method) {
        final int parameterCount = method.getParameterCount();
        if (parameterCount != 1) {
            Console.LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected 1 parameter, got {}",
                    validatorName,
                    varHookName,
                    parameterCount
            );
            return false;
        }
        final Class<?> parameterType = method.getParameterTypes()[0];
        if (!parameterType.isAssignableFrom(String.class)) {
            Console.LOGGER.error(
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

    private static Map<VariableHook, Pair<Field, Boolean>> resolveVariableHooks() {
        return Console.REFLECTIONS.getFieldsAnnotatedWith(VariableHook.class)
                .stream()
                .filter((final Field field) -> {
                    final VariableHook annotation = field.getAnnotation(VariableHook.class);
                    if (annotation.isStatic() && !Modifier.isStatic(field.getModifiers())) {
                        return false;
                    }
                    return Console.hasConstructorWithRegistrationWrapper(field);
                }).collect(Collectors.toMap(
                        (final Field field) -> field.getAnnotation(VariableHook.class),
                        (final Field field) -> ImmutablePair.of(
                                field,
                                !Modifier.isStatic(field.getModifiers()) && Console.hasConstructorWithRegistrationWrapper(field)
                        )
                ));
    }

    private static boolean hasConstructorWithRegistrationWrapper(final Field field) {
        final Constructor<?>[] constructors = field.getDeclaringClass().getDeclaredConstructors();
        if (constructors.length < 1) {
            return false;
        }
        return Arrays.stream(constructors)
                .anyMatch((final Constructor<?> constructor) -> constructor.isAnnotationPresent(RegisterVariableMembers.class));
    }

    private static Map<String, Method> resolveHookValidators() {
        return Console.REFLECTIONS.getMethodsAnnotatedWith(HookValidator.class)
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
                        Console.LOGGER.error(
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
    private static final String VARIABLE_PATH_DELIMITER = ".";
    private static final int INPUT_FIELD_FLAGS = ImGuiInputTextFlags.CallbackHistory
            | ImGuiInputTextFlags.CallbackCharFilter
            | ImGuiInputTextFlags.CallbackCompletion
            | ImGuiInputTextFlags.EnterReturnsTrue
            | ImGuiInputTextFlags.CallbackAlways;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss.SSSS");
    private final ImString rawConsoleInput;
    private String consoleInput;
    private final LinkedBlockingDeque<ExecutedCommand> commandLog;
    private boolean show;
    private boolean tildePressed;
    private boolean scrollToBottom;
    private boolean wasPrevFrameTabCompletion;
    private final List<String> commandSuggestions;

    public Console() {
        this.rawConsoleInput = new ImString();
        this.consoleInput = "";
        this.commandLog = new LinkedBlockingDeque<>(Console.COMMAND_LOG_SIZE);
        this.show = false;
        this.wasPrevFrameTabCompletion = false;
        this.scrollToBottom = false;
        this.tildePressed = false;
        this.commandSuggestions = new ArrayList<>();
    }

    private void submitCommand(final ExecutedCommand executedCommand) {
        if (this.commandLog.size() >= Console.COMMAND_LOG_SIZE) {
            this.commandLog.pop();
        }
        this.commandLog.addLast(executedCommand);
    }

    @SuppressWarnings("java:S3011")
    private Pair<ValidationState, Object> invokeValidator(final Method method,
                                                          final String value) {
        if (method == null) {
            return ImmutablePair.of(
                    null,
                    value
            );
        }
        method.setAccessible(true);
        Object result = null;
        try {
            if ((result = method.invoke(null, value)) == null) {
                return ImmutablePair.of(
                        new ValidationState(
                                false,
                                new ColouredString[]{
                                        new ColouredString(ConsoleColour.RED, "Invalid variable value: "),
                                        new ColouredString(ConsoleColour.NORMAL, value)
                                }
                        ), null
                );
            }
        } catch (final InvocationTargetException | IllegalAccessException e) {
            Console.LOGGER.error("Unable to invoke validator {}", method.getName(), e);
        }
        return ImmutablePair.of(null, result);
    }

    private String formatPathString(final String corePath) {
        return ROOT_NODE_VALUE + VARIABLE_PATH_DELIMITER + corePath;
    }

    @SuppressWarnings({"unchecked"})
    private ValidationState updateVariableValue(final String path,
                                                final String value) {
        final String[] instanceTarget = path.split(Console.VARIABLE_INSTANCE_TARGET_DELIMITER);
        final ValidationState invalidState = new ValidationState(
                false,
                new ColouredString[]{
                        new ColouredString(ConsoleColour.RED, "Unknown variable: "),
                        new ColouredString(ConsoleColour.YELLOW, path)
                }
        );
        final TreePath treePath = TreeUtils.getTreePath(
                Console.HOOKS,
                formatPathString(instanceTarget[0]),
                VARIABLE_PATH_DELIMITER
        );
        if (treePath == null) {
            return invalidState;
        }
        final DefaultMutableTreeNode selectedComponent = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (selectedComponent.isLeaf()) {
            return invalidState;
        }
        final DefaultMutableTreeNode child;
        try {
            child = (DefaultMutableTreeNode) selectedComponent.getFirstChild();
        } catch (final NoSuchElementException e) {
            return invalidState;
        }
        final Pair<Pair<Field, Boolean>, Method> hook = (Pair<Pair<Field, Boolean>, Method>) child.getUserObject();
        final Pair<ValidationState, Object> state = invokeValidator(hook.getValue(), value);
        if (state.getKey() != null) {
            return state.getKey();
        }
        final Pair<Field, Boolean> hookField = hook.getKey();
        final boolean requiresInstance = Boolean.TRUE.equals(hookField.getRight());
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
            final Optional<Object> potentialMatchingInstance = getFieldParentInstance(hookField.getLeft(), instanceTarget[1]);
            if (potentialMatchingInstance.isEmpty()) {
                return new ValidationState(
                        false,
                        new ColouredString[]{
                                new ColouredString(ConsoleColour.RED, "Variable "),
                                new ColouredString(ConsoleColour.YELLOW, path),
                                new ColouredString(ConsoleColour.RED, " is not registered to an instance with ID "),
                                new ColouredString(ConsoleColour.YELLOW, instanceTarget[1]),
                        }
                );
            }
            matchingInstance = potentialMatchingInstance.get();
        }
        synchronized (this) {
            try {
                FieldUtils.writeField(
                        hookField.getLeft(),
                        requiresInstance ? matchingInstance : null,
                        state.getRight(),
                        true
                );
            } catch (final IllegalAccessException e) {
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
                        new ColouredString(ConsoleColour.GREEN, path),
                        new ColouredString(ConsoleColour.NORMAL, " updated to "),
                        new ColouredString(ConsoleColour.YELLOW, value)
                }
        );
    }

    private Optional<Object> getFieldParentInstance(final Field field,
                                                    final String target) {
        final List<Object> instances = Console.FIELD_INSTANCE_MAPPINGS.get(field);
        return instances.stream()
                .filter((final Object instance) -> InstanceIdentifierProvider.deriveInstanceID(instance).equals(target))
                .findFirst();
    }

    private ColouredString[] listAllVars() {
        return Console.REFLECTIONS.getFieldsAnnotatedWith(VariableHook.class)
                .stream()
                .filter((final Field field) -> {
                    final VariableHook annotation = field.getAnnotation(VariableHook.class);
                    if (annotation.isStatic() && !Modifier.isStatic(field.getModifiers())) {
                        return false;
                    }
                    return Console.hasConstructorWithRegistrationWrapper(field);
                }).flatMap((final Field field) -> {
                    final VariableHook annotation = field.getAnnotation(VariableHook.class);
                    if (annotation.isStatic()) {
                        return Stream.of(new ColouredString(
                                ConsoleColour.NORMAL,
                                annotation.name()
                        ));
                    }
                    return Console.FIELD_INSTANCE_MAPPINGS.get(field)
                            .stream()
                            .map((final Object instance) -> new ColouredString(
                                    ConsoleColour.NORMAL,
                                    String.format(
                                            "%s::%s",
                                            annotation.name(),
                                            InstanceIdentifierProvider.deriveInstanceID(instance)
                                    )
                            ));
                }).toArray(ColouredString[]::new);
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
        final Runnable invalidAction = () -> submitCommand(ExecutedCommand.from(
                ConsoleColour.NORMAL.with(this.consoleInput),
                ConsoleColour.RED.with("Unknown command: " + this.consoleInput)
        ));
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
                    invalidAction.run();
                }
                submitCommand(ExecutedCommand.from(
                        ConsoleColour.GREEN.with(command),
                        listAllVars()
                ));
            }
            default -> invalidAction.run();
        }
    }

    private void renderFormattedString(final ColouredString[] colouredStrings) {
        boolean notFirst = false;
        boolean pushed = false;
        for (final ColouredString colouredString : colouredStrings) {
            if (notFirst) {
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
            }
            notFirst = true;
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
            if (Console.SHOW_TIMESTAMP) {
                ImGui.popTextWrapPos();
                ImGui.sameLine(ImGui.getColumnWidth(-1) - timestampWidth);
                ImGui.pushStyleColor(ImGuiCol.Text, ImColor.floatToColor(0f, 0.5f, 0.5f));
                ImGui.text(this.dateFormatter.format(command.date()));
                ImGui.popStyleColor();
            }
            renderFormattedString(command.result());
        }
        ImGui.popTextWrapPos();
        if (this.scrollToBottom && (ImGui.getScrollY() >= ImGui.getScrollMaxY() || Console.COMMAND_LOG_AUTO_SCROLL)) {
            ImGui.setScrollHereY(1.0f);
        }
        this.scrollToBottom = false;
        ImGui.endChild();
    }

    private void inputField() {
        boolean reclaimFocus = false;
        ImGui.pushItemWidth(-ImGui.getStyle().getItemSpacingX() * 7);
        if (ImGui.inputTextWithHint("##", "Type \"help\" for help", this.rawConsoleInput, Console.INPUT_FIELD_FLAGS)) {
            if (!this.rawConsoleInput.isEmpty()) {
                this.consoleInput = this.rawConsoleInput.get();
                Console.LOGGER.info("COMMAND EXECUTED: {}", this.consoleInput);
                handleCommand();
                this.scrollToBottom = true;
            }
            reclaimFocus = true;
            this.rawConsoleInput.clear();
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
        if (!this.show) {
            ImGui.newFrame();
            ImGui.endFrame();
            ImGui.render();
            return;
        }
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(450, 400);
        ImGui.begin("Console");
        executedCommandLog();
        ImGui.separator();
        inputField();
        ImGui.end();
        ImGui.endFrame();
        ImGui.render();
    }

    private int inputCallback() {
        // TODO: Figure out how to register callbacks to inputs
        return 0;
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        if (window.isKeyPressed(GLFW_KEY_GRAVE_ACCENT) && !this.tildePressed) {
            this.show = !this.show;
            this.tildePressed = true;
        }
        if (window.isKeyReleased(GLFW_KEY_GRAVE_ACCENT) && this.tildePressed) {
            this.tildePressed = false;
        }
        final ImGuiIO imGuiIO = ImGui.getIO();
        final MouseInput mouseInput = window.getMouseInput();
        final Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.setMousePos(mousePos.x, mousePos.y);
        imGuiIO.setMouseDown(0, mouseInput.isLeftButtonPressed());
        imGuiIO.setMouseDown(1, mouseInput.isRightButtonPressed());
        final boolean consumed = imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
        if (consumed) {
            // TODO: DO STUFF
        }
        return consumed;
    }
}
