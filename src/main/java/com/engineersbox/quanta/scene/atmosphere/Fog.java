package com.engineersbox.quanta.scene.atmosphere;

import com.engineersbox.quanta.utils.serialization.JsonDeserializeExternalizable;
import com.engineersbox.quanta.utils.serialization.JsonSerializeExternalizable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3f;

public class Fog {

    private boolean active;
    private Vector3f color;
    private float density;

    public Fog() {
        this.active = false;
        this.color = new Vector3f();
    }

    @JsonCreator
    public Fog(@JsonProperty("active") final boolean active,
               @JsonProperty("colour") @JsonDeserializeExternalizable final Vector3f color,
               @JsonProperty("density") final float density) {
        this.color = color;
        this.density = density;
        this.active = active;
    }

    @JsonProperty("colour")
    @JsonSerializeExternalizable
    public Vector3f getColor() {
        return this.color;
    }

    @JsonProperty("density")
    public float getDensity() {
        return this.density;
    }

    @JsonProperty("active")
    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public void setColor(final Vector3f color) {
        this.color = color;
    }

    public void setDensity(final float density) {
        this.density = density;
    }

}
