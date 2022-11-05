package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.rendering.view.Projection;
import com.engineersbox.quanta.resources.object.Mesh;

import java.util.HashMap;
import java.util.Map;

public class Scene {

    private final Map<String, Mesh> meshes;
    private final Projection projection;

    public Scene(final int width,
                 final int height) {
        this.meshes = new HashMap<>();
        this.projection = new Projection(width, height);
    }

    public void addMesh(final String name,
                        final Mesh mesh) {
        this.meshes.put(name, mesh);
    }

    public Map<String, Mesh> getMeshMap() {
        return this.meshes;
    }

    public Projection getProjection() {
        return this.projection;
    }

    public void resize(final int width,
                       final int height) {
        this.projection.updateProjectionMatrix(width, height);
    }

    public void cleanup() {
        this.meshes.values()
                .stream()
                .forEach(Mesh::cleanup);
    }

}
