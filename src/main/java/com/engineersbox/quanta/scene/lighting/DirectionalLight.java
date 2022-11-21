package com.engineersbox.quanta.scene.lighting;

import com.engineersbox.quanta.utils.serialization.JsonDeserializeExternalizable;
import com.engineersbox.quanta.utils.serialization.JsonSerializeExternalizable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3f;

public class DirectionalLight {

    private Vector3f color;

    private Vector3f direction;

    private float intensity;

    @JsonCreator
    public DirectionalLight(@JsonProperty("colour") @JsonDeserializeExternalizable final Vector3f color,
                            @JsonProperty("direction") @JsonDeserializeExternalizable final Vector3f direction,
                            @JsonProperty("intensity") final float intensity) {
        this.color = color;
        this.direction = direction;
        this.intensity = intensity;
    }

    @JsonProperty("colour")
    @JsonSerializeExternalizable
    public Vector3f getColor() {
        return this.color;
    }

    @JsonProperty("direction")
    @JsonSerializeExternalizable
    public Vector3f getDirection() {
        return this.direction;
    }

    @JsonProperty("intensity")
    public float getIntensity() {
        return this.intensity;
    }

    public void setColor(final Vector3f color) {
        this.color = color;
    }

    public void setColor(final float r,
                         final float g,
                         final float b) {
        this.color.set(r, g, b);
    }

    public void setDirection(final Vector3f direction) {
        this.direction = direction;
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
    }

    public void setPosition(final float x,
                            final float y,
                            final float z) {
        this.direction.set(x, y, z);
    }

}
