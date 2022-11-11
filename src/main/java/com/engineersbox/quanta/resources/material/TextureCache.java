package com.engineersbox.quanta.resources.material;

import java.util.HashMap;
import java.util.Map;

public class TextureCache {

    public static final String DEFAULT_TEXTURE = "assets/textures/default_internal.png";

    private final Map<String, Texture> textureMap;

    public TextureCache() {
        this.textureMap = new HashMap<>();
        this.textureMap.put(TextureCache.DEFAULT_TEXTURE, new Texture(TextureCache.DEFAULT_TEXTURE));
    }

    public void cleanup() {
        this.textureMap.values().forEach(Texture::cleanup);
    }

    public Texture createTexture(final String texturePath) {
        return this.textureMap.computeIfAbsent(texturePath, Texture::new);
    }

    public Texture getTexture(final String texturePath) {
        Texture texture = null;
        if (texturePath != null) {
            texture = this.textureMap.get(texturePath);
        }
        if (texture == null) {
            texture = this.textureMap.get(TextureCache.DEFAULT_TEXTURE);
        }
        return texture;
    }

}
