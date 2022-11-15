package com.engineersbox.quanta.rendering;

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

public class AnimationRenderer {

    private final ShaderProgram shaderProgram;
    private Uniforms uniforms;

    public AnimationRenderer() {
        this.shaderProgram = new ShaderProgram(
                new ShaderModuleData("assets/shaders/animation/animation.comp", ShaderType.COMPUTE)
        );
        createUniforms();
    }

    public void cleanup() {
        this.shaderProgram.cleanup();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shaderProgram.getProgramId());
        Stream.of(
                "drawParameters.srcOffset",
                "drawParameters.srcSize",
                "drawParameters.weightsOffset",
                "drawParameters.bonesMatricesOffset",
                "drawParameters.dstOffset"
        ).forEach(this.uniforms::createUniform);
    }

    public void render(final Scene scene,
                       final RenderBuffers renderBuffers) {
        this.shaderProgram.bind();
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, renderBuffers.getBindingPosesBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, renderBuffers.getBonesIndicesWeightsBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, renderBuffers.getBonesMatricesBuffer());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, renderBuffers.getDestAnimationBuffer());

        int dstOffset = 0;
        for (final Model model : scene.getModels().values()) {
            if (model.isAnimated()) {
                for (final MeshDrawData meshDrawData : model.getMeshDrawDataList()) {
                    final AnimMeshDrawData animMeshDrawData = meshDrawData.animMeshDrawData();
                    final Entity entity = animMeshDrawData.entity();
                    final AnimatedFrame frame = entity.getAnimationData().getCurrentFrame();
                    final int groupSize = (int) Math.ceil((float) meshDrawData.sizeInBytes() / (14 * 4));
                    this.uniforms.setUniform(
                            "drawParameters.srcOffset",
                            animMeshDrawData.bindingPoseOffset()
                    );
                    this.uniforms.setUniform(
                            "drawParameters.srcSize",
                            meshDrawData.sizeInBytes() / 4
                    );
                    this.uniforms.setUniform(
                            "drawParameters.weightsOffset",
                            animMeshDrawData.weightsOffset()
                    );
                    this.uniforms.setUniform(
                            "drawParameters.bonesMatricesOffset",
                            frame.getOffset()
                    );
                    this.uniforms.setUniform(
                            "drawParameters.dstOffset",
                            dstOffset
                    );
                    glDispatchCompute(groupSize, 1, 1);
                    dstOffset += meshDrawData.sizeInBytes() / 4;
                }
            }
        }
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        this.shaderProgram.unbind();
    }

}
