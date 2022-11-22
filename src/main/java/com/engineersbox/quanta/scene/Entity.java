package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.resources.assets.object.animation.AnimationData;
import com.engineersbox.quanta.utils.serialization.JsonDeserializeExternalizable;
import com.engineersbox.quanta.utils.serialization.JsonSerializeExternalizable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity {

    private final String id;
    private final String modelId;
    private final Matrix4f modelMatrix;
    private final Vector3f position;
    private final Quaternionf rotation;
    private float scale;
    private AnimationData animationData;

    public Entity(final String id,
                  final String modelId) {
        this.id = id;
        this.modelId = modelId;
        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = 1;
    }

    @JsonCreator
    public Entity(@JsonProperty("id") final String id,
                  @JsonProperty("model_id") final String modelId,
                  @JsonProperty("model_matrix") @JsonDeserializeExternalizable final Matrix4f modelMatrix,
                  @JsonProperty("position") @JsonDeserializeExternalizable final Vector3f position,
                  @JsonProperty("rotation") @JsonDeserializeExternalizable final Quaternionf rotation,
                  @JsonProperty("scale") final float scale) {
        this.id = id;
        this.modelId = modelId;
        this.modelMatrix = modelMatrix;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    @JsonProperty("id")
    public String getId() {
        return this.id;
    }

    @JsonProperty("model_id")
    public String getModelId() {
        return this.modelId;
    }

    @JsonProperty("model_matrix")
    @JsonSerializeExternalizable
    public Matrix4f getModelMatrix() {
        return this.modelMatrix;
    }

    @JsonProperty("position")
    @JsonSerializeExternalizable
    public Vector3f getPosition() {
        return this.position;
    }

    @JsonProperty("rotation")
    @JsonSerializeExternalizable
    public Quaternionf getRotation() {
        return this.rotation;
    }

    @JsonProperty("scale")
    public float getScale() {
        return this.scale;
    }

    public void setPosition(final float x, final float y, final float z) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }

    public void setPosition(final Vector3f pos) {
        setPosition(pos.x, pos.y, pos.z);
    }

    public void setRotation(final float x, final float y, final float z, final float angle) {
        this.rotation.fromAxisAngleRad(x, y, z, angle);
    }

    public void setScale(final float scale) {
        this.scale = scale;
    }

    public void updateModelMatrix() {
        this.modelMatrix.translationRotateScale(
                this.position,
                this.rotation,
                this.scale
        );
    }

    @JsonIgnore
    public AnimationData getAnimationData() {
        return this.animationData;
    }

    public void setAnimationData(final AnimationData animationData) {
        this.animationData = animationData;
    }

    @JsonIgnore
    public void update(final Entity other) {
        this.position.set(other.position);
        this.rotation.set(other.rotation);
        this.modelMatrix.set(other.modelMatrix);
        this.scale = other.scale;
    }

}
