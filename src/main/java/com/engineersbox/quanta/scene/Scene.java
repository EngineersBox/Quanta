package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.resources.object.Mesh;

import java.util.HashMap;
import java.util.Map;

public class Scene {

    private final Map<String, Mesh> meshes;

    public Scene() {
        this.meshes = new HashMap<>();
    }

    public void addMesh(final String name,
                        final Mesh mesh) {
        this.meshes.put(name, mesh);
    }

    public Map<String, Mesh> getMeshMap() {
        return this.meshes;
    }

    public void cleanup() {
        this.meshes.values()
                .stream()
                .forEach(Mesh::cleanup);
    }

}
