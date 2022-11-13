package com.engineersbox.quanta.scene.lighting;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SceneLights {

    private final AmbientLight ambientLight;
    private final DirectionalLight directionalLight;
    private final List<PointLight> pointLights;
    private List<SpotLight> spotLights;

    public SceneLights() {
        this.ambientLight = new AmbientLight();
        this.pointLights = new ArrayList<>();
        this.spotLights = new ArrayList<>();
        this.directionalLight = new DirectionalLight(new Vector3f(1, 1, 1), new Vector3f(0, 1, 0), 1.0f);
    }

    public AmbientLight getAmbientLight() {
        return this.ambientLight;
    }

    public DirectionalLight getDirectionalLight() {
        return this.directionalLight;
    }

    public List<PointLight> getPointLights() {
        return this.pointLights;
    }

    public List<SpotLight> getSpotLights() {
        return this.spotLights;
    }

    public void setSpotLights(final List<SpotLight> spotLights) {
        this.spotLights = spotLights;
    }

}
