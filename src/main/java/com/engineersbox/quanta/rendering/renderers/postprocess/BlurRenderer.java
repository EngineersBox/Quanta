package com.engineersbox.quanta.rendering.renderers.postprocess;

import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.hdr.HDRBuffer;
import com.engineersbox.quanta.resources.assets.object.QuadMesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

@RenderHandler(
        name = BlurRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT,
        stage = ShaderStage.POST_PROCESS
)
public class BlurRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__BLUR_RENDERER";

    @VariableHook(name = "lighting.bloom.blur_amount")
    private static int BLUR_AMOUNT = 10;

    private final QuadMesh quadMesh;

    public BlurRenderer() {
        super(new ShaderProgram(
                new ShaderModuleData("assets/shaders/postprocessing/blur.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/postprocessing/blur.frag", ShaderType.FRAGMENT)
        ));
        createUniforms();
        this.quadMesh = new QuadMesh();
        super.bind();
        super.uniforms.setUniform(
                "image",
                0
        );
        super.unbind();
    }

    private void createUniforms() {
        Stream.of(
                "image",
                "horizontal"
        ).forEach(super.uniforms::createUniform);
    }

    @Override
    public void render(final RenderContext context) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        boolean horizontal = BloomRenderer.BLUR_HORIZONTAL;
        boolean firstIteration = true;
        final HDRBuffer buffer = context.hdrBuffer();
        super.bind();
        for (int i = 0; i < BLUR_AMOUNT; i++) {
            glBindFramebuffer(GL_FRAMEBUFFER, buffer.getPingPongFBOs()[horizontal ? 1 : 0]);
            super.uniforms.setUniform(
                    "horizontal",
                    horizontal
            );
            glBindTexture(
                    GL_TEXTURE_2D,
                    firstIteration
                            ? buffer.getColourBuffers()[1]
                            : buffer.getPingPongColourBuffers()[!horizontal ? 1 : 0]
            );  // bind texture of other framebuffer (or scene if first iteration)
            glBindVertexArray(this.quadMesh.getVaoId());
            glDrawElements(
                    GL_TRIANGLES,
                    this.quadMesh.getVertexCount(),
                    GL_UNSIGNED_INT,
                    0
            );
            glBindVertexArray(0);
            horizontal = !horizontal;
            if (firstIteration) {
                firstIteration = false;
            }
        }
        super.unbind();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.quadMesh.cleanup();
    }

}
