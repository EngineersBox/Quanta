package com.engineersbox.quanta.resources.assets.object;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.object.animation.Animation;
import com.engineersbox.quanta.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {

    private final String id;
    private final List<Entity> entities;
    private final List<Material> materials;
    private final List<Animation> animations;

    public Model(final String id,
                 final List<Material> materials,
                 final List<Animation> animations) {
        this.id = id;
        this.materials = materials;
        this.entities = new ArrayList<>();
        this.animations = animations;
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

    public List<Animation> getAnimations() {
        return this.animations;
    }

    public void cleanup() {
        this.materials.forEach(Material::cleanup);
    }

}
