package com.engineersbox.quanta.resources.assets.object.builtin;

import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;

public class Sphere extends Model {

    private static final String MODEL_RESOURCE_PATH = "/assets/models/sphere.obj";

    public Sphere(final String id,
                  final TextureCache textureCache,
                  final MaterialCache materialCache) {
        super(id, Sphere.MODEL_RESOURCE_PATH);
        final Model model = ModelLoader.loadModel(
                id,
                Sphere.MODEL_RESOURCE_PATH,
                textureCache,
                materialCache,
                false,
                true
        );
        super.getMeshData().addAll(model.getMeshData());
        super.setInternalState(true);
    }

}