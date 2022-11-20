package com.engineersbox.quanta.gui.debug;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.scene.Scene;
import imgui.ImGui;

public class PipelineStatsWidget implements IGUIInstance {

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

    public PipelineStatsWidget() {

    }

    @Override
    public void drawGUI() {
        ImGui.begin("Pipeline Stats");
        // TODO: Implement here
        ImGui.end();
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        return false;
    }
}
