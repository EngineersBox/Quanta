package com.engineersbox.quanta.scene;

import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;

public class SkyBox {

    private final Entity entity;
    private final Model model;

    public SkyBox(final String modelPath,
                  final TextureCache textureCache) {
        this.model = ModelLoader.loadModel("skybox-model", modelPath, textureCache);
        this.entity = new Entity("skybox-entity", this.model.getId());
    }

    public Entity getEntity() {
        return this.entity;
    }

    public Model getModel() {
        return this.model;
    }

}
