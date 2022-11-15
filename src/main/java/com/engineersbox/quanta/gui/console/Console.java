package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.scene.Scene;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.glfw.ImGuiImplGlfw;
import imgui.lwjgl3.glfw.ImGuiImplGlfwNative;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.*;

public class Console implements IGUIInstance {

    private static final Logger LOGGER = LogManager.getLogger(Console.class);
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.FieldsAnnotated, Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );

    private static final TreeModel HOOKS = new DefaultTreeModel(new DefaultMutableTreeNode());

    static {
        final Map<VariableHook, Field> variableHooks = resolveVariableHooks();
        final Map<String, Method> hookValidators = resolveHookValidators();
        int count = 0;
        for (final Map.Entry<VariableHook, Field> entry : variableHooks.entrySet()) {
            final String name = entry.getKey().name();
            if (name.isBlank()) {
                LOGGER.warn("Invalid variable hook name for field [{}]", entry.getValue().getName());
                continue;
            }
            final String[] path = entry.getKey().name().split("\\.");
            final String validatorName = entry.getKey().hookValidator();
            Method hookValidator = null;
            if (!Objects.equals(validatorName, "")) {
                hookValidator = hookValidators.get(validatorName);
            }
            if (hookValidator != null && validateValidatorArgs(
                    name,
                    validatorName,
                    entry.getValue().getType(),
                    hookValidator
            )) {
                continue;
            }
            HOOKS.valueForPathChanged(
                    new TreePath(path),
                    ImmutablePair.of(
                            entry.getValue(),
                            hookValidator
                    )
            );
            count++;
        }
        LOGGER.debug("Resolved {} variable hooks", count);
    }

    private static boolean validateValidatorArgs(final String varHookName,
                                              final String validatorName,
                                              final Class<?> type,
                                              final Method method) {
        final int parameterCount = method.getParameterCount();
        if (parameterCount != 1) {
            LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected 1 parameter, got {}",
                    validatorName,
                    varHookName,
                    parameterCount
            );
            return false;
        }
        final Class<?> parameterType = method.getParameterTypes()[0];
        if (!parameterType.isAssignableFrom(type)) {
            LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected parameter of type {}, got {}",
                    validatorName,
                    varHookName,
                    type.getName(),
                    parameterType.getName()
            );
            return false;
        }
        return true;
    }

    private static Map<VariableHook, Field> resolveVariableHooks() {
        return REFLECTIONS.getFieldsAnnotatedWith(VariableHook.class)
                .stream()
                .collect(Collectors.toMap(
                        (final Field field) -> field.getAnnotation(VariableHook.class),
                        Function.identity()
                ));
    }

    private static Map<String, Method> resolveHookValidators() {
        return REFLECTIONS.getMethodsAnnotatedWith(HookValidator.class)
                .stream()
                .collect(Collectors.toMap(
                        (final Method method) -> method.getAnnotation(HookValidator.class).name(),
                        Function.identity()
                ));
    }

    private static final int COMMAND_LOG_SIZE = 100;
    private static final int COMMAND_LOG_WINDOW_HEIGHT = 10;

    private final ImString rawConsoleInput;
    private String consoleInput;
    private final ImString rawCommandLog;
    private final LinkedBlockingDeque<ExecutedCommand> commandLog;
    private boolean show;

    public Console() {
        this.rawConsoleInput = new ImString();
        this.rawCommandLog = new ImString();
        this.consoleInput = "";
        this.commandLog = new LinkedBlockingDeque<>(COMMAND_LOG_SIZE);
        this.show = false;
    }

    private void submitCommand(final ExecutedCommand executedCommand) {
        if (this.commandLog.size() >= COMMAND_LOG_SIZE) {
            this.commandLog.pop();
        }
        this.commandLog.addLast(executedCommand);
    }

    private void handleCommand() {
        if (this.consoleInput.isBlank()) {
            return;
        }
        final String[] splitCommand = this.consoleInput.split("\\s");
        if (splitCommand.length < 1) {
            return;
        }
        switch (splitCommand[0]) {
            case "set" -> {
                LOGGER.info("Set invoked with args: {}", Arrays.toString(ArrayUtils.subarray(splitCommand, 1, splitCommand.length)));
                submitCommand(new ExecutedCommand(
                        String.join(" ", splitCommand),
                        "set stuff!"
                ));
            }
            case "get" -> {
                LOGGER.info("Get invoked with args: {}", Arrays.toString(ArrayUtils.subarray(splitCommand, 1, splitCommand.length)));
                submitCommand(new ExecutedCommand(
                        String.join(" ", splitCommand),
                        "get stuff!"
                ));
            }
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
        final String items = this.commandLog.stream()
                .flatMap((final ExecutedCommand command) -> Stream.of(command.command(), command.result()))
                .collect(Collectors.joining("\n"));
        this.rawCommandLog.set(items, true);
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(450, 400);
        ImGui.begin("Console");
        ImGui.inputTextMultiline(
                "##",
                this.rawCommandLog,
                0,
                ImGui.getTextLineHeight() * COMMAND_LOG_WINDOW_HEIGHT,
                ImGuiInputTextFlags.ReadOnly
        );
        if (ImGui.inputTextWithHint("##", "Type \"help\" for help", this.rawConsoleInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            this.consoleInput = this.rawConsoleInput.get();
            this.rawConsoleInput.clear();
            LOGGER.info("COMMAND EXECUTED: {}", this.consoleInput);
            handleCommand();
        }
        ImGui.end();
        ImGui.endFrame();
        ImGui.render();
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        if (window.isKeyPressed(GLFW_KEY_GRAVE_ACCENT)) {
            this.show = !this.show;
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
