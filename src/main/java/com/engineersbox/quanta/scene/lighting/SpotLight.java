package com.engineersbox.quanta.scene.lighting;

import com.engineersbox.quanta.utils.serialization.JsonDeserializeExternalizable;
import com.engineersbox.quanta.utils.serialization.JsonSerializeExternalizable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3f;

public class SpotLight {

    private Vector3f coneDirection;
    private float cutOff;
    private float cutOffAngle;
    private PointLight pointLight;

    @JsonCreator
    public SpotLight(@JsonProperty("point_light") final PointLight pointLight,
                     @JsonProperty("cone_direction") @JsonDeserializeExternalizable final Vector3f coneDirection,
                     @JsonProperty("cut_off_angle") final float cutOffAngle) {
        this.pointLight = pointLight;
        this.coneDirection = coneDirection;
        this.cutOffAngle = cutOffAngle;
        setCutOffAngle(cutOffAngle);
    }

    @JsonProperty("cone_direction")
    @JsonSerializeExternalizable
    public Vector3f getConeDirection() {
        return this.coneDirection;
    }

    @JsonIgnore
    public float getCutOff() {
        return this.cutOff;
    }

    @JsonProperty("cut_off_angle")
    public float getCutOffAngle() {
        return this.cutOffAngle;
    }

    @JsonProperty("point_light")
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
