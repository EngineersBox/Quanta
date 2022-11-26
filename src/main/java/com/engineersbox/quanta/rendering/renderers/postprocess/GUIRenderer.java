package com.engineersbox.quanta.rendering.renderers.postprocess;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.debug.hooks.HookValidationException;
import com.engineersbox.quanta.debug.hooks.HookValidator;
import com.engineersbox.quanta.debug.hooks.RegisterInstanceVariableHooks;
import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.resources.assets.gui.GUIMesh;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImInt;
import org.joml.Vector2f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

@RenderHandler(
        name = GUIRenderer.RENDERER_NAME,
        priority = RenderPriority.SOFT_MAX, // Put it way ahead, but leave some room if for some reason we need to render after GUI
        stage = ShaderStage.POST_PROCESS
)
public class GUIRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__GUI_RENDERER";

    private GUIMesh guiMesh;
    private final Vector2f scale;
    private Texture texture;
    private ImGuiImplGlfw imGuiImplGlfw;

    public GUIRenderer() {
        super(new ShaderProgram(
                new ShaderModuleData("assets/shaders/gui/gui.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/gui/gui.frag", ShaderType.FRAGMENT)
        ));
        this.uniforms.createUniform("scale");
        this.scale = new Vector2f();
    }

    private void createUIResources(final Window window) {
        if (this.imGuiImplGlfw != null) {
            return;
        }
        ImGui.createContext();
        this.imGuiImplGlfw = new ImGuiImplGlfw();
        this.imGuiImplGlfw.init(window.getHandle(), true);
        final ImGuiIO imGuiIO = imgui.ImGui.getIO();
        imGuiIO.setIniFilename(null);
        imGuiIO.setDisplaySize(
                window.getWidth(),
                window.getHeight()
        );
        final ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
        final ImInt width = new ImInt();
        final ImInt height = new ImInt();
        final ByteBuffer data = fontAtlas.getTexDataAsRGBA32(width, height);
        this.texture = new Texture(width.get(), height.get(), data);
        this.guiMesh = new GUIMesh();
    }

    @Override
    public void setupData(final RenderContext context) {
        createUIResources(context.window());
    }

    @Override
    public void render(final RenderContext context) {
        final IGUIInstance guiInstance = context.scene().getGUIInstance();
        if (guiInstance == null) {
            return;
        }
        guiInstance.drawGUI();
        super.bind();
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        if (ConfigHandler.CONFIG.engine.glOptions.geometryFaceCulling) {
            glDisable(GL_CULL_FACE);
        }
        glBindVertexArray(this.guiMesh.getVaoId());
        glBindBuffer(GL_ARRAY_BUFFER, this.guiMesh.getVerticesVBO());
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.guiMesh.getIndicesVBO());
        final ImGuiIO io = ImGui.getIO();
        this.scale.x = 2.0f / io.getDisplaySizeX();
        this.scale.y = -2.0f / io.getDisplaySizeY();
        super.uniforms.setUniform("scale", this.scale);
        final ImDrawData drawData = ImGui.getDrawData();
        final int numLists = drawData.getCmdListsCount();
        for (int i = 0; i < numLists; i++) {
            glBufferData(GL_ARRAY_BUFFER, drawData.getCmdListVtxBufferData(i), GL_STREAM_DRAW);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, drawData.getCmdListIdxBufferData(i), GL_STREAM_DRAW);
            final int numCmds = drawData.getCmdListCmdBufferSize(i);
            for (int j = 0; j < numCmds; j++) {
                final int elemCount = drawData.getCmdListCmdBufferElemCount(i, j);
                final int idxBufferOffset = drawData.getCmdListCmdBufferIdxOffset(i, j);
                final int indices = idxBufferOffset * ImDrawData.SIZEOF_IM_DRAW_IDX;

                this.texture.bind();
                glActiveTexture(GL_TEXTURE0);
                glDrawElements(GL_TRIANGLES, elemCount, GL_UNSIGNED_SHORT, indices);
            }
        }
        glEnable(GL_DEPTH_TEST);
        if (ConfigHandler.CONFIG.engine.glOptions.geometryFaceCulling) {
            glEnable(GL_CULL_FACE);
        }
        glDisable(GL_BLEND);
        super.unbind();
    }

    @Override
    public void resize(final int width,
                       final int height) {
        final ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(width, height);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.texture.cleanup();
    }
}
