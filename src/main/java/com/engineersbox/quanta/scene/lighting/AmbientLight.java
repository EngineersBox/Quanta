package com.engineersbox.quanta.scene.lighting;

import org.joml.Vector3f;

public class AmbientLight {

    private Vector3f color;

    private float intensity;

    public AmbientLight(final float intensity,
                        final Vector3f color) {
        this.intensity = intensity;
        this.color = color;
    }

    public AmbientLight() {
        this(1.0f, new Vector3f(1.0f, 1.0f, 1.0f));
    }

    public Vector3f getColor() {
        return this.color;
    }

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

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
    }

}
