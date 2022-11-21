package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.rendering.view.Projection;
import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.serialization.ModelDeserializer;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.SceneLights;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;

import java.util.HashMap;
import java.util.Map;

public class Scene {

    private final Map<String, Model> models;
    private final Projection projection;
    private final TextureCache textureCache;
    private final MaterialCache materialCache;
    private final Camera camera;
    private IGUIInstance guiInstance;
    private SceneLights sceneLights;
    private SkyBox skyBox;
    private Fog fog;

    public Scene(final int width,
                 final int height) {
        this.models = new HashMap<>();
        this.projection = new Projection(width, height);
        this.textureCache = new TextureCache();
        this.materialCache = new MaterialCache();
        this.camera = new Camera();
        this.fog = new Fog();
    }

    public void addEntity(final Entity entity) {
        final String modelId = entity.getModelId();
        final Model model = this.models.get(modelId);
        if (model == null) {
            throw new RuntimeException("No such model: " + modelId);
        }
        model.getEntities().add(entity);
    }

    public void addModel(final Model model) {
        this.models.put(model.getId(), model);
    }

    @JsonProperty("models")
    public Map<String, Model> getModels() {
        return this.models;
    }

    @JsonProperty("projection")
    public Projection getProjection() {
        return this.projection;
    }

    public void resize(final int width, final int height) {
        this.projection.updateProjectionMatrix(width, height);
    }

    @JsonIgnore
    public TextureCache getTextureCache() {
        return this.textureCache;
    }

    @JsonIgnore
    public MaterialCache getMaterialCache() {
        return this.materialCache;
    }

    @JsonProperty("camera")
    public Camera getCamera() {
        return this.camera;
    }

    @JsonIgnore
    public IGUIInstance getGUIInstance() {
        return this.guiInstance;
    }

    public void setGUIInstance(final IGUIInstance guiInstance) {
        this.guiInstance = guiInstance;
    }

    @JsonProperty("scene_lights")
    public SceneLights getSceneLights() {
        return this.sceneLights;
    }

    public void setSceneLights(final SceneLights sceneLights) {
        this.sceneLights = sceneLights;
    }

    @JsonProperty("skybox")
    public SkyBox getSkyBox() {
        return this.skyBox;
    }

    public void setSkyBox(final SkyBox skyBox) {
        this.skyBox = skyBox;
    }

    @JsonProperty("fog")
    public Fog getFog() {
        return this.fog;
    }

    public void setFog(final Fog fog) {
        this.fog = fog;
    }
}
