package com.engineersbox.quanta.gui.debug;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.debug.OpenGLInfo;
import com.engineersbox.quanta.debug.PipelineStatistics;
import com.engineersbox.quanta.debug.VariableHooksState;
import com.engineersbox.quanta.debug.hooks.HookBinding;
import com.engineersbox.quanta.debug.hooks.InstanceIdentifierProvider;
import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.gui.format.ColouredString;
import com.engineersbox.quanta.gui.format.GUITextColour;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.reflect.VarHandleUtils;
import imgui.ImGui;
import org.apache.commons.lang3.stream.Streams;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreStatsWidget implements IGUIInstance {

    /*
     * LAYOUT
     * =========================
     * [OPENGL CONTEXT]
     *  - Version
     *  - GLSL Version
     *  - Vendor
     *  - Renderer
     *  - Extensions
     * [ENGINE PROPERTIES]
     *  - Cull faces
     *  - Show triangles
     *  - Compatibility profile
     *  - Frustum culling
     * [DEBUG]
     *  - Shadows only
     *  - Depth only
     *  - Normals only
     *  - Show shadow cascades
     *  - Flat
     * [CAMERA]
     *  - Near/far
     *  - FOV
     *  - Position: [
     *    - X
     *    - Y
     *    - Z
     *   ]
     *  - Rotation: [
     *    - X
     *    - Y
     *    - Z
     *   ]
     * [RENDERER]
     *  - Pre-cull scene elements
     *   - Instanced
     *   - Non-instanced
     *  - Culled scene elements
     *   - Instanced
     *   - Non-instanced
     * [PIPELINE]
     *  - VERTICES_SUBMITTED
     *  - TRIANGLES_SUBMITTED
     *  - PRIMITIVES_SUBMITTED
     *  - VERTEX_SHADER_INVOCATIONS
     *  - TESS_CONTROL_SHADER_PATCHES
     *  - TESS_EVALUATION_SHADER_INVOCATIONS
     *  - GEOMETRY_SHADER_INVOCATIONS
     *  - GEOMETRY_SHADER_PRIMITIVES_EMITTED
     *  - FRAGMENT_SHADER_INVOCATIONS
     *  - COMPUTE_SHADER_INVOCATIONS
     *  - CLIPPING_INPUT_PRIMITIVES
     *  - CLIPPING_OUTPUT_PRIMITIVES
     * =========================
     */

    private final OpenGLInfo openGLInfo;
    private final PipelineStatistics pipelineStatistics;
    private final Camera camera;

    public CoreStatsWidget(final OpenGLInfo openGLInfo,
                           final PipelineStatistics pipelineStatistics,
                           final Camera camera) {
        this.openGLInfo = openGLInfo;
        this.pipelineStatistics = pipelineStatistics;
        this.camera = camera;
    }

    @Override
    public void drawGUI() {
        ImGui.begin("Pipeline Stats");
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
    }

    private ColouredString booleanValue(final boolean value) {
        return (value ? GUITextColour.GREEN : GUITextColour.RED).withFormat(
                "%s%n",
                value
        );
    }

    private ColouredString[] formatBooleanProperty(final String name,
                                                   final boolean value) {
        return new ColouredString[] {
                GUITextColour.NORMAL.with(name),
                booleanValue(value)
        };
    }

    private void drawEngineProperties() {
        if (!ImGui.collapsingHeader("Engine Properties")) {
            return;
        }
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
    }

    private void drawVariableHookStates() {
        if (!ImGui.collapsingHeader("Variable Hooks")) {
            return;
        }
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
                                        GUITextColour.NORMAL.withFormat(" - %s: ", instance),
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
    }

    private void drawCameraState() {
        if (!ImGui.collapsingHeader("Camera")) {
            return;
        }
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
        if (!ImGui.collapsingHeader("Pipeline")) {
            return;
        }
        ColouredString.renderFormattedString(
                (Object) Arrays.stream(PipelineStatistics.Stat.values())
                        .map((final PipelineStatistics.Stat stat) -> GUITextColour.NORMAL.withFormat(
                                "%s: %d%n",
                                stat.name(),
                                this.pipelineStatistics.getResult(stat)
                        )).toArray(ColouredString[]::new)
        );
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        return false;
    }
}
