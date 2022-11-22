package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.indirect.AnimMeshDrawData;
import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.rendering.indirect.RenderBuffers;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimatedFrame;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;

@RenderHandler(
        name = AnimationRenderer.RENDERER_NAME,
        priority = 0,
        stage = ShaderStage.PRE_PROCESS
)
public class AnimationRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__ANIMATION_RENDERER";

    public AnimationRenderer() {
        super(new ShaderProgram(
                new ShaderModuleData("assets/shaders/animation/animation.comp", ShaderType.COMPUTE)
        ));
        Stream.of(
                "drawParameters.srcOffset",
                "drawParameters.srcSize",
                "drawParameters.weightsOffset",
                "drawParameters.bonesMatricesOffset",
                "drawParameters.dstOffset"
        ).forEach(super.uniforms::createUniform);
    }

    public void render(final RenderContext context) {
        super.bind();
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, context.renderBuffers().getBindingPosesBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, context.renderBuffers().getBonesIndicesWeightsBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, context.renderBuffers().getBonesMatricesBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, context.renderBuffers().getDestAnimationBuffer());
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
                super.uniforms.setUniform(
                        "drawParameters.srcOffset",
                        animMeshDrawData.bindingPoseOffset()
                );
                super.uniforms.setUniform(
                        "drawParameters.srcSize",
                        meshDrawData.sizeInBytes() / 4
                );
                super.uniforms.setUniform(
                        "drawParameters.weightsOffset",
                        animMeshDrawData.weightsOffset()
                );
                super.uniforms.setUniform(
                        "drawParameters.bonesMatricesOffset",
                        frame.getOffset()
                );
                super.uniforms.setUniform(
                        "drawParameters.dstOffset",
                        dstOffset
                );
                glDispatchCompute(groupSize, 1, 1);
                dstOffset += meshDrawData.sizeInBytes() / 4;
            }
        }
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        super.unbind();
    }

}
