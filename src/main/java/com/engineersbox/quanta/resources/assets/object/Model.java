package com.engineersbox.quanta.resources.assets.object;

import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.resources.assets.object.animation.Animation;
import com.engineersbox.quanta.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {

    private final String id;
    private final List<Animation> animations;
    private final List<Entity> entities;
    private final List<MeshData> meshData;
    private final List<MeshDrawData> meshDrawData;

    public Model(final String id,
                 final List<MeshData> meshData,
                 final List<Animation> animations) {
        this.entities = new ArrayList<>();
        this.id = id;
        this.meshData = meshData;
        this.animations = animations;
        this.meshDrawData = new ArrayList<>();
    }

    public List<Animation> getAnimations() {
        return this.animations;
    }

    public List<Entity> getEntities() {
        return this.entities;
    }

    public String getId() {
        return this.id;
    }

    public List<MeshData> getMeshData() {
        return this.meshData;
    }

    public List<MeshDrawData> getMeshDrawData() {
        return this.meshDrawData;
    }

    public boolean isAnimated() {
        return this.animations != null && !this.animations.isEmpty();
    }

}
