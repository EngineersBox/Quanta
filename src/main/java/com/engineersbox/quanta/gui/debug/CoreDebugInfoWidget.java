package com.engineersbox.quanta.gui.debug;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.debug.OpenGLInfo;
import com.engineersbox.quanta.debug.PipelineStatistics;
import com.engineersbox.quanta.debug.VariableHooksState;
import com.engineersbox.quanta.debug.hooks.HookBinding;
import com.engineersbox.quanta.debug.hooks.InstanceIdentifierProvider;
import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.gui.IndentManager;
import com.engineersbox.quanta.gui.format.ColouredString;
import com.engineersbox.quanta.gui.format.GUITextColour;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.reflect.VarHandleUtils;
import imgui.ImGui;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;

public class CoreDebugInfoWidget implements IGUIInstance {

    private static final float INDENT_SIZE = 10.0f;
    private final OpenGLInfo openGLInfo;
    private final PipelineStatistics pipelineStatistics;
    private final Camera camera;
    private final IndentManager indentManager;

    public CoreDebugInfoWidget(final OpenGLInfo openGLInfo,
                               final PipelineStatistics pipelineStatistics,
                               final Camera camera) {
        this.openGLInfo = openGLInfo;
        this.pipelineStatistics = pipelineStatistics;
        this.camera = camera;
        this.indentManager = new IndentManager();
    }

    @Override
    public void drawGUI() {
        ImGui.begin("Core Debug Info");
        drawOpenGLContext();
        drawEngineProperties();
        drawVariableHookStates();
        drawCameraState();
        drawPipelineStats();
        ImGui.end();
    }

    private void drawOpenGLContext() {
        if (!ImGui.collapsingHeader("OpenGL Context")) {
            return;
        }
        this.indentManager.push(CoreDebugInfoWidget.INDENT_SIZE);
        ColouredString.renderFormattedString(
                GUITextColour.NORMAL.withFormat(
                        "Version: %s%n",
                        this.openGLInfo.version()
                ),
                GUITextColour.NORMAL.withFormat(
                        "GLSL Version: %s%n",
                        this.openGLInfo.glslVersion()
                ),
                GUITextColour.NORMAL.withFormat(
                        "Vendor: %s%n",
                        this.openGLInfo.vendor()
                ),
                GUITextColour.NORMAL.withFormat(
                        "Renderer: %s%n",
                        this.openGLInfo.renderer()
                ),
                GUITextColour.NORMAL.withFormat(
                        "Extensions: %s%n",
                        this.openGLInfo.extensions()
                )
        );
        if (!ImGui.collapsingHeader("Supported Extensions")) {
            this.indentManager.pop();
            return;
        }
        this.indentManager.push(CoreDebugInfoWidget.INDENT_SIZE);
        ColouredString.renderFormattedString(
                (Object) IntStream.range(0, this.openGLInfo.extensions())
                        .mapToObj((final int extensionId) -> glGetStringi(
                                GL_EXTENSIONS,
                                extensionId
                        )).map((final String extensionName) -> GUITextColour.NORMAL.withFormat(
                                "%s%n",
                                extensionName
                        )).toArray(ColouredString[]::new)
        );
        this.indentManager.popN(2);
    }

    private ColouredString booleanValue(final boolean value) {
        return (value ? GUITextColour.GREEN : GUITextColour.RED).withFormat(
                "%s%n",
                value
        );
    }

    private ColouredString[] formatBooleanProperty(final String name,
                                                   final boolean value) {
        return new ColouredString[]{
                GUITextColour.NORMAL.with(name),
                booleanValue(value)
        };
    }

    private void drawEngineProperties() {
        if (!ImGui.collapsingHeader("Engine Properties")) {
            return;
        }
        this.indentManager.push(CoreDebugInfoWidget.INDENT_SIZE);
        ColouredString.renderFormattedString(
                formatBooleanProperty("Cull faces: ", ConfigHandler.CONFIG.engine.glOptions.cullface),
                formatBooleanProperty("Show triangles: ", ConfigHandler.CONFIG.engine.glOptions.showTrianges),
                formatBooleanProperty("Compatibility profile: ", ConfigHandler.CONFIG.engine.glOptions.compatProfile),
                formatBooleanProperty("Antialiasing: ", ConfigHandler.CONFIG.engine.glOptions.antialiasing),
                GUITextColour.NORMAL.withFormat(
                        "Antialiasing samples: %d",
                        ConfigHandler.CONFIG.engine.glOptions.aaSamples
                )
        );
        this.indentManager.pop();
    }

    private void drawVariableHookStates() {
        if (!ImGui.collapsingHeader("Variable Hooks")) {
            return;
        }
        this.indentManager.push(CoreDebugInfoWidget.INDENT_SIZE);
        final Object[] colouredStringObjects = VariableHooksState.HOOKS.getLeafNodes()
                .stream()
                .map((final DefaultMutableTreeNode node) -> (HookBinding) node.getUserObject())
                .flatMap((final HookBinding hookBinding) -> {
                    final Field field = hookBinding.field();
                    final VariableHook annotation = field.getAnnotation(VariableHook.class);
                    if (Modifier.isStatic(field.getModifiers())) {
                        final Object value = VarHandleUtils.getValue(
                                hookBinding.varHandle(),
                                null,
                                false
                        );
                        return Stream.of(
                                GUITextColour.NORMAL.withFormat(
                                        "[%s] %s: ",
                                        field.getType().getSimpleName(),
                                        annotation.displayName().isBlank() ? annotation.name() : annotation.displayName()
                                ),
                                field.getType().isAssignableFrom(boolean.class)
                                        ? booleanValue((boolean) value)
                                        : GUITextColour.NORMAL.withFormat("%s%n", value)
                        );
                    }
                    final Stream<ColouredString> instancedVariableValues = (VariableHooksState.FIELD_INSTANCE_MAPPINGS.get(field)
                            .stream()
                            .flatMap((final Object instance) -> {
                                final Object value = VarHandleUtils.getValue(
                                        hookBinding.varHandle(),
                                        instance,
                                        true
                                );
                                return Stream.of(
                                        GUITextColour.NORMAL.withFormat(" - %s: ", InstanceIdentifierProvider.deriveInstanceID(instance)),
                                        field.getType().isAssignableFrom(boolean.class)
                                                ? booleanValue((boolean) value)
                                                : GUITextColour.NORMAL.withFormat("%s%n", value)
                                );
                            }));
                    return Stream.concat(
                            Stream.of(GUITextColour.NORMAL.withFormat(
                                    "[%s] %s:%n",
                                    field.getType().getSimpleName(),
                                    annotation.displayName().isBlank() ? annotation.name() : annotation.displayName()
                            )),
                            instancedVariableValues
                    );
                }).toArray(Object[]::new);
        ColouredString.renderFormattedString(colouredStringObjects);
        this.indentManager.pop();
    }

    private void drawCameraState() {
        if (!ImGui.collapsingHeader("Camera")) {
            return;
        }
        this.indentManager.push(CoreDebugInfoWidget.INDENT_SIZE);
        ColouredString.renderFormattedString(
                GUITextColour.NORMAL.withFormat(
                        "Near/far: %f/%f%n",
                        ConfigHandler.CONFIG.render.camera.zNear,
                        ConfigHandler.CONFIG.render.camera.zFar
                ),
                GUITextColour.NORMAL.withFormat(
                        "FOV: %f%n",
                        ConfigHandler.CONFIG.render.camera.fov
                ),
                GUITextColour.NORMAL.withFormat("Position: [%n"),
                new ColouredString[]{
                        GUITextColour.NORMAL.withFormat(
                                "  X: %f%n",
                                this.camera.getPosition().x
                        ),
                        GUITextColour.NORMAL.withFormat(
                                "  Y: %f%n",
                                this.camera.getPosition().y
                        ),
                        GUITextColour.NORMAL.withFormat(
                                "  Z: %f%n",
                                this.camera.getPosition().z
                        )
                },
                GUITextColour.NORMAL.withFormat("]%n"),
                GUITextColour.NORMAL.withFormat("Rotation: [%n"),
                new ColouredString[]{
                        GUITextColour.NORMAL.withFormat(
                                "  Pitch: %f%n",
                                this.camera.getRotation().x
                        ),
                        GUITextColour.NORMAL.withFormat(
                                "  Yaw: %f%n",
                                this.camera.getRotation().y
                        ),
                        GUITextColour.NORMAL.withFormat(
                                "  Roll: %f%n",
                                this.camera.getRotation().z
                        )
                },
                GUITextColour.NORMAL.withFormat("]%n")
        );
        this.indentManager.pop();
    }

    private void drawRendererState() {
        if (!ImGui.collapsingHeader("Renderer")) {
            return;
        }
        ColouredString.renderFormattedString(
                // TODO: Nothing to show yet
        );
    }

    private void drawPipelineStats() {
        if (!this.pipelineStatistics.extensionAvailable()
            || !ImGui.collapsingHeader("Pipeline")) {
            return;
        }
        this.indentManager.push(CoreDebugInfoWidget.INDENT_SIZE);
        ColouredString.renderFormattedString(
                (Object) Arrays.stream(PipelineStatistics.Stat.values())
                        .map((final PipelineStatistics.Stat stat) -> GUITextColour.NORMAL.withFormat(
                                "%s: %d%n",
                                stat.name(),
                                Integer.toUnsignedLong(this.pipelineStatistics.getResult(stat))
                        )).toArray(ColouredString[]::new)
        );
        this.indentManager.pop();
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        return false;
    }
}
