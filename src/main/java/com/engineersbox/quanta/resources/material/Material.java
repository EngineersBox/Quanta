package com.engineersbox.quanta.resources.material;

import com.engineersbox.quanta.resources.object.Mesh;

import java.util.ArrayList;
import java.util.List;

public class Material {

    private final List<Mesh> meshes;
    private String texturePath;

    public Material() {
        this.meshes = new ArrayList<>();
    }

    public void cleanup() {
        this.meshes.forEach(Mesh::cleanup);
    }

    public List<Mesh> getMeshes() {
        return this.meshes;
    }

    public String getTexturePath() {
        return this.texturePath;
    }

    public void setTexturePath(final String texturePath) {
        this.texturePath = texturePath;
    }

}
