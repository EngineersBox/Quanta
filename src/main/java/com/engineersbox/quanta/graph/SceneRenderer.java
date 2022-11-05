package com.engineersbox.quanta.graph;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.resources.object.Mesh;
import com.engineersbox.quanta.resources.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.shader.ShaderProgram;
import com.engineersbox.quanta.scene.Scene;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class SceneRenderer {

    private final ShaderProgram shader;

    public SceneRenderer() {
        final List<ShaderModuleData> modules = List.of(
                new ShaderModuleData("assets/shaders/scene/scene.vert", GL_VERTEX_SHADER),
                new ShaderModuleData("assets/shaders/scene/scene.frag", GL_FRAGMENT_SHADER)
        );
        this.shader = new ShaderProgram(modules);
    }

    public void render(final Window window,
                       final Scene scene) {
        this.shader.bind();
        scene.getMeshMap().values().forEach((final Mesh mesh) -> {
            glBindVertexArray(mesh.getVaoId());
            glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
        });
        glBindVertexArray(0);
        ShaderProgram.unbind();
    }

    public void cleanup() {
        this.shader.cleanup();
    }

}
