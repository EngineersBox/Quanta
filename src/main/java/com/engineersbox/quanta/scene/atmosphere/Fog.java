package com.engineersbox.quanta.scene.atmosphere;

import org.joml.Vector3f;

public class Fog {

    private boolean active;
    private Vector3f color;
    private float density;

    public Fog() {
        this.active = false;
        this.color = new Vector3f();
    }

    public Fog(final boolean active,
               final Vector3f color,
               final float density) {
        this.color = color;
        this.density = density;
        this.active = active;
    }

    public Vector3f getColor() {
        return this.color;
    }

    public float getDensity() {
        return this.density;
    }

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
