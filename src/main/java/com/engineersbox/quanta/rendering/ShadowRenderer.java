package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.rendering.shadow.ShadowBuffer;
import com.engineersbox.quanta.rendering.shadow.ShadowCascade;
import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimationData;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL30.*;

public class ShadowRenderer {

    private final List<ShadowCascade> cascadeShadows;
    private final ShaderProgram shaderProgram;
    private final ShadowBuffer shadowBuffer;
    private Uniforms uniforms;

    public ShadowRenderer() {
        this.shaderProgram = new ShaderProgram(
                new ShaderModuleData("assets/shaders/shadow/shadow.vert", ShaderType.VERTEX)
        );
        this.shadowBuffer = new ShadowBuffer();
        this.cascadeShadows = new ArrayList<>();
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            final ShadowCascade cascadeShadow = new ShadowCascade();
            this.cascadeShadows.add(cascadeShadow);
        }
        createUniforms();
    }

    public void cleanup() {
        this.shaderProgram.cleanup();
        this.shadowBuffer.cleanup();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shaderProgram.getProgramId());
        this.uniforms.createUniform("modelMatrix");
        this.uniforms.createUniform("projectionViewMatrix");
        this.uniforms.createUniform("bonesMatrices");
    }

    public List<ShadowCascade> getCascadeShadows() {
        return this.cascadeShadows;
    }

    public ShadowBuffer getShadowBuffer() {
        return this.shadowBuffer;
    }

    public void render(final Scene scene) {
        ShadowCascade.updateCascadeShadows(this.cascadeShadows, scene);

        glBindFramebuffer(GL_FRAMEBUFFER, this.shadowBuffer.getDepthMapFBO());
        glViewport(0, 0, ShadowBuffer.SHADOW_MAP_WIDTH, ShadowBuffer.SHADOW_MAP_HEIGHT);
        this.shaderProgram.bind();
        final Collection<Model> models = scene.getModels().values();
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowBuffer.getDepthMapTexture().getIds()[i], 0);
            glClear(GL_DEPTH_BUFFER_BIT);
            final ShadowCascade shadowCascade = this.cascadeShadows.get(i);
            this.uniforms.setUniform("projectionViewMatrix", shadowCascade.getProjectionViewMatrix());
            for (final Model model : models) {
                final List<Entity> entities = model.getEntities();
                for (final Material material : model.getMaterials()) {
                    for (final Mesh mesh : material.getMeshes()) {
                        glBindVertexArray(mesh.getVaoId());
                        for (final Entity entity : entities) {
                            this.uniforms.setUniform("modelMatrix", entity.getModelMatrix());
                            final AnimationData animationData = entity.getAnimationData();
                            this.uniforms.setUniform(
                                    "bonesMatrices",
                                    animationData == null
                                            ? AnimationData.DEFAULT_BONES_MATRICES
                                            : animationData.getCurrentFrame().boneMatrices()
                            );
                            glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
                        }
                    }
                }
            }
        }
        this.shaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
