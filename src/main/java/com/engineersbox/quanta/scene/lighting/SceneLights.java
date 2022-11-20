package com.engineersbox.quanta.scene.lighting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SceneLights {

    private final AmbientLight ambientLight;
    private final DirectionalLight directionalLight;
    private final List<PointLight> pointLights;
    private List<SpotLight> spotLights;

    public SceneLights() {
        this(
                new AmbientLight(),
                new DirectionalLight(new Vector3f(1, 1, 1), new Vector3f(0, 1, 0), 1.0f),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    @JsonCreator
    public SceneLights(@JsonProperty("ambient") final AmbientLight ambientLight,
                       @JsonProperty("directional") final DirectionalLight directionalLight,
                       @JsonProperty("point") final List<PointLight> pointLights,
                       @JsonProperty("spot") final List<SpotLight> spotLights) {
        this.ambientLight = ambientLight;
        this.directionalLight = directionalLight;
        this.pointLights = pointLights;
        this.spotLights = spotLights;
    }

    @JsonProperty("ambient")
    public AmbientLight getAmbientLight() {
        return this.ambientLight;
    }

    @JsonProperty("directional")
    public DirectionalLight getDirectionalLight() {
        return this.directionalLight;
    }

    @JsonProperty("point")
    public List<PointLight> getPointLights() {
        return this.pointLights;
    }

    @JsonProperty("spot")
    public List<SpotLight> getSpotLights() {
        return this.spotLights;
    }

    public void setSpotLights(final List<SpotLight> spotLights) {
        this.spotLights = spotLights;
    }

}
