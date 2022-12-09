package com.engineersbox.quanta.rendering.renderers.postprocess;

import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.buffers.HDRBuffer;
import com.engineersbox.quanta.rendering.buffers.SSAOBuffer;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
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
        name = SSAOApplyRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT + 1,
        stage = ShaderStage.POST_PROCESS
)
public class SSAOApplyRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__SSAO_APPLY_RENDERER";

    private final QuadMesh quadMesh;

    public SSAOApplyRenderer() {
        super(new ShaderProgram(
                "SSAO Apply",
                new ShaderModuleData("assets/shaders/postprocessing/ssao/ssao.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/postprocessing/ssao/ssao_apply.frag", ShaderType.FRAGMENT)
        ));
        this.quadMesh = new QuadMesh();
        createUniforms();
    }

    private void createUniforms() {
        final Uniforms uniforms = super.getUniforms("SSAO Apply");
        Stream.of(
                "scene",
                "ssao"
        ).forEach(uniforms::createUniform);
        super.bind("SSAO Apply");
        uniforms.setUniform(
                "scene",
                0
        );
        uniforms.setUniform(
                "ssao",
                1
        );
        super.unbind("SSAO Apply");
    }

    @Override
    public void render(final RenderContext context) {
        final SSAOBuffer ssaoBuffer = (SSAOBuffer) context.attributes().get("ssaoBuffer");
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, ssaoBuffer.getApplyFboId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        super.bind("SSAO Apply");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ssaoBuffer.getColourBufferApply());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, ssaoBuffer.getColourBufferBlur());
        this.quadMesh.render();
        super.unbind("SSAO Apply");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void cleanup(final RenderContext context) {
        super.cleanup(context);
        this.quadMesh.cleanup();
    }

}
