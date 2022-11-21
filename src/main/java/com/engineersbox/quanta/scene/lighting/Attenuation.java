package com.engineersbox.quanta.scene.lighting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Attenuation {

    private float constant;
    private float exponent;
    private float linear;

    @JsonCreator
    public Attenuation(@JsonProperty("constant") final float constant,
                       @JsonProperty("linear") final float linear,
                       @JsonProperty("exponent") final float exponent) {
        this.constant = constant;
        this.linear = linear;
        this.exponent = exponent;
    }

    @JsonProperty("constant")
    public float getConstant() {
        return this.constant;
    }

    @JsonProperty("exponent")
    public float getExponent() {
        return this.exponent;
    }

    @JsonProperty("linear")
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
