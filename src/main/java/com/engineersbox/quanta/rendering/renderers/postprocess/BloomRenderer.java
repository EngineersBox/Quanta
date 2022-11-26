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
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

@RenderHandler(
        name = BloomRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT + 1,
        stage = ShaderStage.POST_PROCESS
)
public class BloomRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__BLOOM_RENDERER";

    @VariableHook(name = "bloom.filter_horizontal")
    private static boolean BLUR_HORIZONTAL = true;
    @VariableHook(name = "bloom.enable")
    private static boolean BLOOM_ENABLE = true;
    @VariableHook(name = "hdr.exposure")
    private static float EXPOSURE = 1.0f;

    private final QuadMesh quadMesh;

    public BloomRenderer() {
        super(new ShaderProgram(
                new ShaderModuleData("assets/shaders/postprocessing/bloom.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/postprocessing/bloom.frag", ShaderType.FRAGMENT)
        ));
        createUniforms();
        this.quadMesh = new QuadMesh();
        super.bind();
        super.uniforms.setUniform(
                "scene",
                0
        );
        super.uniforms.setUniform(
                "bloomBlur",
                1
        );
        super.unbind();
    }

    private void createUniforms() {
        Stream.of(
                "scene",
                "bloomBlur",
                "bloom",
                "exposure"
        ).forEach(super.uniforms::createUniform);
    }

    @Override
    public void render(final RenderContext context) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        final HDRBuffer buffer = context.hdrBuffer();
        super.shader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, buffer.getColourBuffers()[0]);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, buffer.getPingPongColourBuffers()[!BLUR_HORIZONTAL ? 1 : 0]);
        super.uniforms.setUniform(
                "bloom",
                BLOOM_ENABLE
        );
        super.uniforms.setUniform(
                "exposure",
                EXPOSURE
        );
        glBindVertexArray(this.quadMesh.getVaoId());
        glDrawElements(
                GL_TRIANGLES,
                this.quadMesh.getVertexCount(),
                GL_UNSIGNED_INT,
                0
        );
        glBindVertexArray(0);
        super.shader.unbind();
    }
}
