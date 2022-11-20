package com.engineersbox.quanta.resources.assets.object;

import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.resources.assets.object.animation.Animation;
import com.engineersbox.quanta.scene.Entity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        this(
                id,
                meshData,
                animations,
                new ArrayList<>()
        );
    }

    @JsonCreator
    public Model(@JsonProperty("id") final String id,
                 @JsonProperty("mesh_data") final List<MeshData> meshData,
                 @JsonProperty("animations") final List<Animation> animations,
                 @JsonProperty("entities") final List<Entity> entities) {
        this.id = id;
        this.meshData = meshData;
        this.meshDrawData = new ArrayList<>();
        this.animations = animations;
        this.entities = entities;
    }

    @JsonProperty("animations")
    public List<Animation> getAnimations() {
        return this.animations;
    }

    @JsonProperty("entities")
    public List<Entity> getEntities() {
        return this.entities;
    }

    @JsonProperty("id")
    public String getId() {
        return this.id;
    }

    @JsonProperty("mesh_data")
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
