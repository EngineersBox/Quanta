package com.engineersbox.quanta.resources.assets.object.builtin;

import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;

public class Cone extends Model {

    private static final String MODEL_RESOURCE_PATH = "/assets/models/cone.obj";

    public Cone(final String id,
                final TextureCache textureCache,
                final MaterialCache materialCache) {
        super(id, Cone.MODEL_RESOURCE_PATH);
        final Model model = ModelLoader.loadModel(
                id,
                Cone.MODEL_RESOURCE_PATH,
                textureCache,
                materialCache,
                false,
                true
        );
        super.getMeshData().addAll(model.getMeshData());
    }
}
