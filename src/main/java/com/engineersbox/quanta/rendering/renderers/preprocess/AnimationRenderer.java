package com.engineersbox.quanta.rendering.renderers.preprocess;

import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.buffers.GBuffer;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.indirect.AnimMeshDrawData;
import com.engineersbox.quanta.rendering.indirect.AnimationRenderBuffers;
import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimatedFrame;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;

@RenderHandler(
        name = AnimationRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT,
        stage = ShaderStage.PRE_PROCESS
)
public class AnimationRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__ANIMATION_RENDERER";

    public AnimationRenderer() {
        super(new ShaderProgram(
                "Animation",
                new ShaderModuleData("assets/shaders/animation/animation.comp", ShaderType.COMPUTE)
        ));
        final Uniforms uniforms = super.getUniforms("Animation");
        Stream.of(
                "drawParameters.srcOffset",
                "drawParameters.srcSize",
                "drawParameters.weightsOffset",
                "drawParameters.bonesMatricesOffset",
                "drawParameters.dstOffset"
        ).forEach(uniforms::createUniform);
    }

    @Override
    public void setupData(final RenderContext context) {
        final AnimationRenderBuffers animationRenderBuffers = new AnimationRenderBuffers();
        animationRenderBuffers.loadStaticModels(context.scene());
        animationRenderBuffers.loadAnimatedModels(context.scene());
        context.attributes().put(
                "animationRenderBuffers",
                animationRenderBuffers
        );
    }

    @Override
    public void render(final RenderContext context) {
        super.bind("Animation");
        final Uniforms uniforms = super.getUniforms("Animation");
        final AnimationRenderBuffers animationRenderBuffers = (AnimationRenderBuffers) context.attributes().get("animationRenderBuffers");
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, animationRenderBuffers.getBindingPosesBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, animationRenderBuffers.getBonesIndicesWeightsBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, animationRenderBuffers.getBonesMatricesBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, animationRenderBuffers.getDestAnimationBuffer());
        int dstOffset = 0;
        for (final Model model : context.scene().getModels().values()) {
            if (!model.isAnimated()) {
                continue;
            }
            for (final MeshDrawData meshDrawData : model.getMeshDrawData()) {
                final AnimMeshDrawData animMeshDrawData = meshDrawData.animMeshDrawData();
                final Entity entity = animMeshDrawData.entity();
                final AnimatedFrame frame = entity.getAnimationData().getCurrentFrame();
                final int groupSize = (int) Math.ceil((float) meshDrawData.sizeInBytes() / (14 * 4));
                uniforms.setUniform(
                        "drawParameters.srcOffset",
                        animMeshDrawData.bindingPoseOffset()
                );
                uniforms.setUniform(
                        "drawParameters.srcSize",
                        meshDrawData.sizeInBytes() / 4
                );
                uniforms.setUniform(
                        "drawParameters.weightsOffset",
                        animMeshDrawData.weightsOffset()
                );
                uniforms.setUniform(
                        "drawParameters.bonesMatricesOffset",
                        frame.getOffset()
                );
                uniforms.setUniform(
                        "drawParameters.dstOffset",
                        dstOffset
                );
                glDispatchCompute(groupSize, 1, 1);
                dstOffset += meshDrawData.sizeInBytes() / 4;
            }
        }
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        super.unbind("Animation");
    }

    @Override
    public void cleanup(final RenderContext context) {
        super.cleanup(context);
        ((AnimationRenderBuffers) context.attributes().get("animationRenderBuffers")).cleanup();
    }

}
