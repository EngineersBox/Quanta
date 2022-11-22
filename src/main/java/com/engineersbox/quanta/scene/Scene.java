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
import com.engineersbox.quanta.scene.serialization.SceneDeserializer;
import com.engineersbox.quanta.utils.serialization.SerializationUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(using = SceneDeserializer.class)
public class Scene {

    private static final Logger LOGGER = LogManager.getLogger(Scene.class);

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

    @JsonIgnore //TODO: Serialize the gui instance
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

    public void serialize(final String filePath) {
        try {
            Scene.LOGGER.info("Started serializing scene {} to {}", this, filePath);
            final long start = System.currentTimeMillis();
            SerializationUtils.OBJECT_MAPPER.writeValue(
                    new File(filePath),
                    this
            );
            final long end = System.currentTimeMillis();
            Scene.LOGGER.info(
                    "Finished serializing scene {} to {}, took {}ms",
                    this,
                    filePath,
                    end - start
            );
        } catch (final IOException e) {
            throw new RuntimeException(String.format(
                    "Unable to serialize scene %s to file %s",
                    this,
                    filePath
            ), e);
        }
    }

    public static Scene deserialize(final String filePath) {
        try {
            Scene.LOGGER.info("Started deserializing scene from {}", filePath);
            final long start = System.currentTimeMillis();
            final Scene scene = SerializationUtils.OBJECT_MAPPER.readerFor(Scene.class)
                    .readValue(new File(filePath));
            final long end = System.currentTimeMillis();
            Scene.LOGGER.info(
                    "Finished deserializing scene from {} to {}, took {}ms",
                    filePath,
                    scene,
                    end - start
            );
            return scene;
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                    "Unable to deserialize scene from file %s",
                    filePath
            ), e);
        }
    }
}
