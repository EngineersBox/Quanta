package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.rendering.view.Projection;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.SceneLights;

import java.util.HashMap;
import java.util.Map;

public class Scene {

    private final Map<String, Model> models;
    private final Projection projection;
    private final TextureCache textureCache;
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

    public void cleanup() {
        this.models.values().forEach(Model::cleanup);
    }

    public Map<String, Model> getModels() {
        return this.models;
    }

    public Projection getProjection() {
        return this.projection;
    }

    public void resize(final int width, final int height) {
        this.projection.updateProjectionMatrix(width, height);
    }

    public TextureCache getTextureCache() {
        return this.textureCache;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public IGUIInstance getGUIInstance() {
        return this.guiInstance;
    }

    public void setGUIInstance(final IGUIInstance guiInstance) {
        this.guiInstance = guiInstance;
    }

    public SceneLights getSceneLights() {
        return this.sceneLights;
    }

    public void setSceneLights(final SceneLights sceneLights) {
        this.sceneLights = sceneLights;
    }

    public SkyBox getSkyBox() {
        return this.skyBox;
    }

    public void setSkyBox(final SkyBox skyBox) {
        this.skyBox = skyBox;
    }

    public Fog getFog() {
        return this.fog;
    }

    public void setFog(final Fog fog) {
        this.fog = fog;
    }
}
