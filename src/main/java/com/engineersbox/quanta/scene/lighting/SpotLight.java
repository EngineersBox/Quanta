package com.engineersbox.quanta.scene.lighting;

import org.joml.Vector3f;

public class SpotLight {

    private Vector3f coneDirection;
    private float cutOff;
    private float cutOffAngle;
    private PointLight pointLight;

    public SpotLight(final PointLight pointLight,
                     final Vector3f coneDirection,
                     final float cutOffAngle) {
        this.pointLight = pointLight;
        this.coneDirection = coneDirection;
        this.cutOffAngle = cutOffAngle;
        setCutOffAngle(cutOffAngle);
    }

    public Vector3f getConeDirection() {
        return this.coneDirection;
    }

    public float getCutOff() {
        return this.cutOff;
    }

    public float getCutOffAngle() {
        return this.cutOffAngle;
    }

    public PointLight getPointLight() {
        return this.pointLight;
    }

    public void setConeDirection(final float x,
                                 final float y,
                                 final float z) {
        this.coneDirection.set(x, y, z);
    }

    public void setConeDirection(final Vector3f coneDirection) {
        this.coneDirection = coneDirection;
    }

    public final void setCutOffAngle(final float cutOffAngle) {
        this.cutOffAngle = cutOffAngle;
        this.cutOff = (float) Math.cos(Math.toRadians(cutOffAngle));
    }

    public void setPointLight(final PointLight pointLight) {
        this.pointLight = pointLight;
    }

}
