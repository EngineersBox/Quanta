package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.shadow.ShadowCascade;
import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimationData;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.lighting.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class SceneRenderer {

    private static final int MAX_POINT_LIGHTS = 5;
    private static final int MAX_SPOT_LIGHTS = 5;

    private final ShaderProgram shader;
    private Uniforms uniforms;

    public SceneRenderer() {
        this.shader = new ShaderProgram(
                new ShaderModuleData("assets/shaders/scene/scene.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/scene/scene.frag", ShaderType.FRAGMENT)
        );
        this.shader.validate();
        createUniforms();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shader.getProgramId());
        Stream.of(
                "projectionMatrix",
                "viewMatrix",
                "modelMatrix",
                "texSampler",
                "normalSampler",
                "material.diffuse",
                "material.specular",
                "material.reflectance",
                "material.hasNormalMap",
                "bonesMatrices"
        ).forEach(this.uniforms::createUniform);
    }

    public void render(final Scene scene,
                       final GBuffer gBuffer) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gBuffer.getGBufferId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, gBuffer.getWidth(), gBuffer.getHeight());
        glDisable(GL_BLEND);
        this.shader.bind();
        this.uniforms.setUniform(
                "projectionMatrix",
                scene.getProjection().getProjectionMatrix()
        );
        this.uniforms.setUniform(
                "viewMatrix",
                scene.getCamera().getViewMatrix()
        );
        this.uniforms.setUniform(
                "texSampler",
                0
        );
        this.uniforms.setUniform(
                "normalSampler",
                1
        );
        final TextureCache textureCache = scene.getTextureCache();
        for (final Model model : scene.getModels().values()) {
            final List<Entity> entities = model.getEntities();
            for (final Material material : model.getMaterials()) {
                this.uniforms.setUniform(
                        "material.diffuse",
                        material.getDiffuseColor()
                );
                this.uniforms.setUniform(
                        "material.specular",
                        material.getSpecularColor()
                );
                this.uniforms.setUniform(
                        "material.reflectance",
                        material.getReflectance()
                );
                final String normalMapPath = material.getNormalMapPath();
                final boolean hasNormalMapPath = normalMapPath != null;
                this.uniforms.setUniform(
                        "material.hasNormalMap",
                        hasNormalMapPath ? 1 : 0
                );
                final Texture texture = textureCache.getTexture(material.getTexturePath());
                glActiveTexture(GL_TEXTURE0);
                texture.bind();
                if (hasNormalMapPath) {
                    final Texture normalMapTexture = textureCache.getTexture(normalMapPath);
                    glActiveTexture(GL_TEXTURE1);
                    normalMapTexture.bind();
                }
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
        glBindVertexArray(0);
        glEnable(GL_BLEND);
        this.shader.unbind();
    }

    public void cleanup() {
        this.shader.cleanup();
    }

}
