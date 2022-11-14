package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.MeshData;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;

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
        final MeshData meshData = this.skyBoxModel.getMeshDataList().get(0);
        this.material = materialCache.getMaterial(meshData.getMaterialIdx());
        this.mesh = new Mesh(meshData);
        this.skyBoxModel.getMeshDataList().clear();
        this.skyBoxEntity = new Entity("skyBoxEntity-entity", this.skyBoxModel.getId());
    }

    public void cleanuo() {
        this.mesh.cleanup();
    }

    public Material getMaterial() {
        return this.material;
    }

    public Mesh getMesh() {
        return this.mesh;
    }

    public Entity getEntity() {
        return this.skyBoxEntity;
    }

    public Model getModel() {
        return this.skyBoxModel;
    }

}
