package com.engineersbox.quanta.resources.object;

import com.engineersbox.quanta.resources.material.Material;
import com.engineersbox.quanta.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {

    private final String id;
    private final List<Entity> entities;
    private final List<Material> materials;

    public Model(final String id,
                 final List<Material> materials) {
        this.id = id;
        this.materials = materials;
        this.entities = new ArrayList<>();
    }

    public List<Entity> getEntities() {
        return this.entities;
    }

    public String getId() {
        return this.id;
    }

    public List<Material> getMaterials() {
        return this.materials;
    }


    public void cleanup() {
        this.materials.forEach(Material::cleanup);
    }

}
