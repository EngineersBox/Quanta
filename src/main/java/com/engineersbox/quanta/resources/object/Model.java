package com.engineersbox.quanta.resources.object;

import java.util.ArrayList;
import java.util.List;

public class Model {

    private final String id;
    private final List<Entity> entities;
    private final List<Mesh> meshes;

    public Model(final String id,
                 final List<Mesh> meshes) {
        this.id = id;
        this.meshes = meshes;
        this.entities = new ArrayList<>();
    }

    public List<Entity> getEntities() {
        return this.entities;
    }

    public String getId() {
        return this.id;
    }

    public List<Mesh> getMeshes() {
        return this.meshes;
    }


    public void cleanup() {
        this.meshes.forEach(Mesh::cleanup);
    }

}
