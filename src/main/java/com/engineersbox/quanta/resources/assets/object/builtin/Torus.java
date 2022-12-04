package com.engineersbox.quanta.resources.assets.object.builtin;

import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;

public class Torus extends Model {

    private static final String MODEL_RESOURCE_PATH = "/assets/models/torus.obj";

    public Torus(final String id,
                 final TextureCache textureCache,
                 final MaterialCache materialCache) {
        super(id, Torus.MODEL_RESOURCE_PATH);
        final Model model = ModelLoader.loadModel(
                id,
                Torus.MODEL_RESOURCE_PATH,
                textureCache,
                materialCache,
                false
        );
        super.getMeshData().addAll(model.getMeshData());
        super.setInternalState(true);
    }

}