package com.engineersbox.quanta.resources.assets.object.builtin;

import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;

public class Pyramid extends Model {

    private static final String MODEL_RESOURCE_PATH = "/assets/models/pyramid.obj";

    public Pyramid(final String id,
                   final TextureCache textureCache,
                   final MaterialCache materialCache) {
        super(id, Pyramid.MODEL_RESOURCE_PATH);
        final Model model = ModelLoader.loadModel(
                id,
                Pyramid.MODEL_RESOURCE_PATH,
                textureCache,
                materialCache,
                false
        );
        super.getMeshData().addAll(model.getMeshData());
    }

}
