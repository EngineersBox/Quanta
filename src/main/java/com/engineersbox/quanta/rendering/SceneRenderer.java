package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.resources.material.Material;
import com.engineersbox.quanta.resources.material.Texture;
import com.engineersbox.quanta.resources.material.TextureCache;
import com.engineersbox.quanta.resources.object.Mesh;
import com.engineersbox.quanta.resources.object.Model;
import com.engineersbox.quanta.resources.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.shader.ShaderProgram;
import com.engineersbox.quanta.resources.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;

import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class SceneRenderer {

    private final ShaderProgram shader;
    private Uniforms uniforms;

    public SceneRenderer() {
        final List<ShaderModuleData> modules = List.of(
                new ShaderModuleData("assets/shaders/scene/scene.vert", GL_VERTEX_SHADER),
                new ShaderModuleData("assets/shaders/scene/scene.frag", GL_FRAGMENT_SHADER)
        );
        this.shader = new ShaderProgram(modules);
        createUniforms();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shader.getProgramId());
        Stream.of(
                "projectionMatrix",
                "viewMatrix",
                "modelMatrix",
                "texSampler"
        ).forEach(this.uniforms::createUniform);
    }

    public void render(final Window window,
                       final Scene scene) {
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
        final TextureCache textureCache = scene.getTextureCache();
        for (final Model model : scene.getModels().values()) {
            final List<Entity> entities = model.getEntities();

            for (final Material material : model.getMaterials()) {
                final Texture texture = textureCache.getTexture(material.getTexturePath());
                glActiveTexture(GL_TEXTURE0);
                texture.bind();

                for (final Mesh mesh : material.getMeshes()) {
                    glBindVertexArray(mesh.getVaoId());
                    for (final Entity entity : entities) {
                        this.uniforms.setUniform("modelMatrix", entity.getModelMatrix());
                        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
                    }
                }
            }
        }
        glBindVertexArray(0);
        ShaderProgram.unbind();
    }

    public void cleanup() {
        this.shader.cleanup();
    }

}
