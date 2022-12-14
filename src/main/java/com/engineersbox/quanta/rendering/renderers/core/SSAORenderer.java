package com.engineersbox.quanta.rendering.renderers.core;

import com.engineersbox.quanta.device.gpu.texture.MemoryTexture;
import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.buffers.GBuffer;
import com.engineersbox.quanta.rendering.buffers.SSAOBuffer;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.indirect.AnimationRenderBuffers;
import com.engineersbox.quanta.resources.assets.object.QuadMesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import org.joml.Vector3f;

import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

@RenderHandler(
        name = SSAORenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT + 1,
        stage = ShaderStage.CORE
)
public class SSAORenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__SSAO_RENDERER";

    private final QuadMesh quadMesh;

    public SSAORenderer() {
        super(
                new ShaderProgram(
                        "SSAO",
                        new ShaderModuleData("assets/shaders/postprocessing/ssao/ssao.vert", ShaderType.VERTEX),
                        new ShaderModuleData("assets/shaders/postprocessing/ssao/ssao.frag", ShaderType.FRAGMENT)
                ),
                new ShaderProgram(
                        "SSAO Blur",
                        new ShaderModuleData("assets/shaders/postprocessing/ssao/ssao.vert", ShaderType.VERTEX),
                        new ShaderModuleData("assets/shaders/postprocessing/ssao/ssao_blur.frag", ShaderType.FRAGMENT)
                )
        );
        this.quadMesh = new QuadMesh();
        createUniforms();
        super.bind("SSAO");
        Uniforms uniforms = super.getUniforms("SSAO");
        uniforms.setUniform(
                "gPosition",
                0
        );
        uniforms.setUniform(
                "gNormal",
                1
        );
        uniforms.setUniform(
                "texNoise",
                2
        );
        super.unbind("SSAO");
        super.bind("SSAO Blur");
        uniforms = super.getUniforms("SSAO Blur");
        uniforms.setUniform(
                "ssaoInput",
                0
        );
        super.unbind("SSAO Blur");
    }

    private void createUniforms() {
        Uniforms uniforms = super.getUniforms("SSAO");
        Stream.of(
                "gPosition",
                "gNormal",
                "texNoise",
                "projection"
        ).forEach(uniforms::createUniform);
        for (int i = 0; i < SSAOBuffer.KERNEL_SIZE; i++) {
            uniforms.createUniform("samples[" + i + "]");
        }
        uniforms = super.getUniforms("SSAO Blur");
        Stream.of(
                "ssaoInput"
        ).forEach(uniforms::createUniform);
    }

    @Override
    public void setupData(final RenderContext context) {
        context.attributes().put(
                "ssaoBuffer",
                new SSAOBuffer(context.window())
        );
    }

    @Override
    public void render(final RenderContext context) {
        renderSSAO(context);
        renderSSAOBlur(context);
    }

    private void renderSSAO(final RenderContext context) {
        final SSAOBuffer ssaoBuffer = (SSAOBuffer) context.attributes().get("ssaoBuffer");
        final GBuffer gBuffer = (GBuffer) context.attributes().get("gBuffer");
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoBuffer.getFboId());
        glClear(GL_COLOR_BUFFER_BIT);
        super.bind("SSAO");
        final Uniforms uniforms = super.getUniforms("SSAO");
        final List<Vector3f> kernel = ssaoBuffer.getKernel();
        for (int i = 0; i < SSAOBuffer.KERNEL_SIZE; i++) {
            uniforms.setUniform(
                    "samples[" + i + "]",
                    kernel.get(i)
            );
        }
        uniforms.setUniform(
                "projection",
                context.scene().getProjection().getProjectionMatrix()
        );
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getTextureIds()[3]);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getTextureIds()[1]);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, ssaoBuffer.getNoiseTexture());
        this.quadMesh.render();
        super.unbind("SSAO");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void renderSSAOBlur(final RenderContext context) {
        final SSAOBuffer ssaoBuffer = (SSAOBuffer) context.attributes().get("ssaoBuffer");
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoBuffer.getBlurFboId());
        glClear(GL_COLOR_BUFFER_BIT);
        super.bind("SSAO Blur");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ssaoBuffer.getColourBuffer());
        this.quadMesh.render();
        super.unbind("SSAO Blur");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void cleanup(final RenderContext context) {
        super.cleanup(context);
        ((SSAOBuffer) context.attributes().get("ssaoBuffer")).cleanup();
        this.quadMesh.cleanup();
    }

}
