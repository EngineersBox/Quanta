package com.engineersbox.quanta.rendering.renderers.postprocess;

import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.buffers.HDRBuffer;
import com.engineersbox.quanta.resources.assets.object.QuadMesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

@RenderHandler(
        name = BloomRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT,
        stage = ShaderStage.POST_PROCESS
)
public class BloomRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__BLOOM_RENDERER";

    @VariableHook(name = "lighting.bloom.filter_horizontal")
    public static boolean BLUR_HORIZONTAL = true;
    @VariableHook(name = "lighting.bloom.blur_amount")
    private static int BLUR_AMOUNT = 10;
    @VariableHook(name = "lighting.bloom.enable")
    private static boolean BLOOM_ENABLE = true;
    @VariableHook(name = "lighting.hdr.exposure")
    private static float EXPOSURE = 1.0f;

    private final QuadMesh quadMesh;

    public BloomRenderer() {
        super(
                new ShaderProgram(
                        "Bloom Blur",
                        new ShaderModuleData("assets/shaders/postprocessing/blur.vert", ShaderType.VERTEX),
                        new ShaderModuleData("assets/shaders/postprocessing/blur.frag", ShaderType.FRAGMENT)
                ),
                new ShaderProgram(
                        "Bloom Final",
                        new ShaderModuleData("assets/shaders/postprocessing/bloom.vert", ShaderType.VERTEX),
                        new ShaderModuleData("assets/shaders/postprocessing/bloom.frag", ShaderType.FRAGMENT)
                )
        );
        createUniforms();

        this.quadMesh = new QuadMesh();
        super.bind("Bloom Blur");
        super.getUniforms("Bloom Blur").setUniform(
                "image",
                0
        );
        super.unbind("Bloom Blur");

        super.bind("Bloom Final");
        final Uniforms uniforms = super.getUniforms("Bloom Final");
        uniforms.setUniform(
                "scene",
                0
        );
        uniforms.setUniform(
                "bloomBlur",
                1
        );
        super.unbind("Bloom Final");
    }

    private void createUniforms() {
        Uniforms uniforms = super.getUniforms("Bloom Blur");
        Stream.of(
                "image",
                "horizontal"
        ).forEach(uniforms::createUniform);
        uniforms = super.getUniforms("Bloom Final");
        Stream.of(
                "scene",
                "bloomBlur",
                "bloom",
                "exposure"
        ).forEach(uniforms::createUniform);
    }

    @Override
    public void render(final RenderContext context) {
        renderBlur(context);
        renderBloom(context);
    }

    private void renderBloom(final RenderContext context) {
        glBindFramebuffer(GL_FRAMEBUFFER, context.ssaoBuffer().getApplyFboId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        final HDRBuffer buffer = context.hdrBuffer();
        super.bind("Bloom Final");
        final Uniforms uniforms = super.getUniforms("Bloom Final");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, buffer.getColourBuffers()[0]);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, buffer.getPingPongColourBuffers()[!BLUR_HORIZONTAL ? 1 : 0]);
        uniforms.setUniform(
                "bloom",
                BLOOM_ENABLE
        );
        uniforms.setUniform(
                "exposure",
                EXPOSURE
        );
        this.quadMesh.render();
        super.unbind("Bloom Final");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void renderBlur(final RenderContext context) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        boolean horizontal = BloomRenderer.BLUR_HORIZONTAL;
        boolean firstIteration = true;
        final HDRBuffer buffer = context.hdrBuffer();
        super.bind("Bloom Blur");
        final Uniforms uniforms = super.getUniforms("Bloom Blur");
        for (int i = 0; i < BLUR_AMOUNT; i++) {
            glBindFramebuffer(GL_FRAMEBUFFER, buffer.getPingPongFBOs()[horizontal ? 1 : 0]);
            uniforms.setUniform(
                    "horizontal",
                    horizontal
            );
            glBindTexture(
                    GL_TEXTURE_2D,
                    firstIteration
                            ? buffer.getColourBuffers()[1]
                            : buffer.getPingPongColourBuffers()[!horizontal ? 1 : 0]
            );  // bind texture of other framebuffer (or scene if first iteration)
            this.quadMesh.render();
            horizontal = !horizontal;
            if (firstIteration) {
                firstIteration = false;
            }
        }
        super.unbind("Bloom Blur");
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.quadMesh.cleanup();
    }
}
