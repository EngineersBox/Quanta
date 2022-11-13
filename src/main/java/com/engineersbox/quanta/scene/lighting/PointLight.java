package com.engineersbox.quanta.scene.lighting;

import org.joml.Vector3f;

public class PointLight {

    private Attenuation attenuation;
    private Vector3f color;
    private float intensity;
    private final Vector3f position;

    public PointLight(final Vector3f color,
                      final Vector3f position,
                      final float intensity) {
        this.attenuation = new Attenuation(0, 0, 1);
        this.color = color;
        this.position = position;
        this.intensity = intensity;
    }

    public Attenuation getAttenuation() {
        return this.attenuation;
    }

    public Vector3f getColor() {
        return this.color;
    }

    public float getIntensity() {
        return this.intensity;
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public void setAttenuation(final Attenuation attenuation) {
        this.attenuation = attenuation;
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

    public void setPosition(final float x,
                            final float y,
                            final float z) {
        this.position.set(x, y, z);
    }

}
