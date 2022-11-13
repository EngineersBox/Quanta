package com.engineersbox.quanta.scene.lighting;

public class Attenuation {

    private float constant;
    private float exponent;
    private float linear;

    public Attenuation(final float constant,
                       final float linear,
                       final float exponent) {
        this.constant = constant;
        this.linear = linear;
        this.exponent = exponent;
    }

    public float getConstant() {
        return this.constant;
    }

    public float getExponent() {
        return this.exponent;
    }

    public float getLinear() {
        return this.linear;
    }

    public void setConstant(final float constant) {
        this.constant = constant;
    }

    public void setExponent(final float exponent) {
        this.exponent = exponent;
    }

    public void setLinear(final float linear) {
        this.linear = linear;
    }
}
