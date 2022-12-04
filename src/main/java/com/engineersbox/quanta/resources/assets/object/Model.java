package com.engineersbox.quanta.resources.assets.object;

import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.resources.assets.object.animation.Animation;
import com.engineersbox.quanta.resources.assets.object.builtin.Terrain;
import com.engineersbox.quanta.resources.assets.object.serialization.ModelDeserializer;
import com.engineersbox.quanta.scene.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(
        @JsonSubTypes.Type(
                value = Terrain.class,
                name = "terrain"
        )
)
@JsonDeserialize(using = ModelDeserializer.class)
public class Model {

    private final String id;
    private final String path;
    protected final List<Animation> animations;
    protected final List<Entity> entities;
    protected final List<MeshData> meshData;
    protected final List<MeshDrawData> meshDrawData;
    private boolean isInternal;

    public Model(final String id,
                 final String path) {
        this(
                id,
                path,
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public Model(final String id,
                 final String path,
                 final List<MeshData> meshData,
                 final List<Animation> animations) {
        this.id = id;
        this.path = path;
        this.meshData = meshData;
        this.animations = animations;
        this.entities = new ArrayList<>();
        this.meshDrawData = new ArrayList<>();
        this.isInternal = false;
    }

    @JsonIgnore
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

    @JsonProperty("path")
    public String getPath() {
        return this.path;
    }

    @JsonIgnore
    public List<MeshData> getMeshData() {
        return this.meshData;
    }

    @JsonIgnore
    public List<MeshDrawData> getMeshDrawData() {
        return this.meshDrawData;
    }

    @JsonProperty("is_animated")
    public boolean isAnimated() {
        return this.animations != null && !this.animations.isEmpty();
    }

    @JsonProperty("is_internal")
    public boolean getIsInternal() {
        return this.isInternal;
    }

    @JsonIgnore
    protected void setInternalState(final boolean isInternal) {
        this.isInternal = isInternal;
    }

}
