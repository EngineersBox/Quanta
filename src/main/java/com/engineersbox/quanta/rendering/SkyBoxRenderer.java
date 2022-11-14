package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.SkyBox;
import org.joml.Matrix4f;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class SkyBoxRenderer {

    private final ShaderProgram shader;
    private Uniforms uniforms;
    private final Matrix4f viewMatrix;

    public SkyBoxRenderer() {
        this.shader = new ShaderProgram(
                new ShaderModuleData("assets/shaders/scene/skybox.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/scene/skybox.frag", ShaderType.FRAGMENT)
        );
        this.shader.validate();
        this.viewMatrix = new Matrix4f();
        createUniforms();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shader.getProgramId());
        Stream.of(
                "projectionMatrix",
                "viewMatrix",
                "modelMatrix",
                "diffuse",
                "texSampler",
                "hasTexture"
        ).forEach(this.uniforms::createUniform);
    }

    public void render(final Scene scene) {
        final SkyBox skyBox = scene.getSkyBox();
        if (skyBox == null) {
            return;
        }
        this.shader.bind();
        this.uniforms.setUniform(
                "projectionMatrix",
                scene.getProjection().getProjectionMatrix()
        );
        this.viewMatrix.set(scene.getCamera().getViewMatrix());
        this.viewMatrix.m30(0);
        this.viewMatrix.m31(0);
        this.viewMatrix.m32(0);
        this.uniforms.setUniform(
                "viewMatrix",
                this.viewMatrix
        );
        this.uniforms.setUniform(
                "texSampler",
                0
        );
        final Model skyBoxModel = skyBox.getModel();
        final Entity skyBoxEntity = skyBox.getEntity();
        final TextureCache textureCache = scene.getTextureCache();
        for (final Material material : skyBoxModel.getMaterials()) {
            final Texture texture = textureCache.getTexture(material.getTexturePath());
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
            this.uniforms.setUniform("diffuse", material.getDiffuseColor());
            this.uniforms.setUniform("hasTexture", texture.getPath().equals(TextureCache.DEFAULT_TEXTURE) ? 0 : 1);
            for (final Mesh mesh : material.getMeshes()) {
                glBindVertexArray(mesh.getVaoId());
                this.uniforms.setUniform("modelMatrix", skyBoxEntity.getModelMatrix());
                glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
            }
        }
        glBindVertexArray(0);
        this.shader.unbind();
    }

    public void cleanup() {
        this.shader.cleanup();
    }

}
