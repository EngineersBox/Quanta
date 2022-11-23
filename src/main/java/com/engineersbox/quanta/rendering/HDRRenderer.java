package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.resources.assets.object.QuadMesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

@RenderHandler(
        name = HDRRenderer.RENDERER_NAME,
        priority = 2,
        stage = ShaderStage.CORE
)
public class HDRRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__HDR_RENDERER";

    @VariableHook(name = "hdr.enable")
    private static boolean ENABLE_HDR = true;
    @VariableHook(name = "hdr.exposure")
    private static float EXPOSURE = 1.0f;

    private final QuadMesh quadMesh;

    public HDRRenderer() {
        super(new ShaderProgram(
                new ShaderModuleData("assets/shaders/lighting/hdr.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/lighting/hdr.frag", ShaderType.FRAGMENT)
        ));
        createUniforms();
        this.quadMesh = new QuadMesh();
        super.bind();
        super.uniforms.setUniform(
                "hdrBuffer",
                0
        );
        super.unbind();
    }

    private void createUniforms() {
        Stream.of(
                "hdrBuffer",
                "hdr",
                "exposure"
        ).forEach(super.uniforms::createUniform);
    }

    @Override
    public void render(final RenderContext context) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        super.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, context.hdrBuffer().getColourBufferId());
        super.uniforms.setUniform(
                "hdr",
                this.ENABLE_HDR
        );
        super.uniforms.setUniform(
                "exposure",
                this.EXPOSURE
        );
        glBindVertexArray(this.quadMesh.getVaoId());
        glDrawElements(
                GL_TRIANGLES,
                this.quadMesh.getVertexCount(),
                GL_UNSIGNED_INT,
                0
        );
        glBindVertexArray(0);
        super.unbind();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.quadMesh.cleanup();
    }

}
