package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.MeshData;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SkyBox {

    private final Material material;
    private final Mesh mesh;
    private final Entity skyBoxEntity;
    private final Model skyBoxModel;

    public SkyBox(final String skyBoxModelPath,
                  final TextureCache textureCache,
                  final MaterialCache materialCache) {
        this.skyBoxModel = ModelLoader.loadModel(
                "skybox-model",
                skyBoxModelPath,
                textureCache,
                materialCache,
                false
        );
        final MeshData meshData = this.skyBoxModel.getMeshData().get(0);
        this.material = materialCache.getMaterial(meshData.getMaterialIdx());
        this.mesh = new Mesh(meshData);
        this.skyBoxModel.getMeshData().clear();
        this.skyBoxEntity = new Entity("skyBoxEntity-entity", this.skyBoxModel.getId());
    }

    @JsonCreator
    public SkyBox(@JsonProperty("material") final Material material,
                  @JsonProperty("mesh") final Mesh mesh,
                  @JsonProperty("entity") final Entity entity,
                  @JsonProperty("model") final Model model) {
        this.material = material;
        this.mesh = mesh;
        this.skyBoxEntity = entity;
        this.skyBoxModel = model;
    }

    public void cleanup() {
        this.mesh.cleanup();
    }

    @JsonProperty("material")
    public Material getMaterial() {
        return this.material;
    }

    @JsonProperty("mesh")
    public Mesh getMesh() {
        return this.mesh;
    }

    @JsonProperty("entity")
    public Entity getEntity() {
        return this.skyBoxEntity;
    }

    @JsonProperty("model")
    public Model getModel() {
        return this.skyBoxModel;
    }

}
