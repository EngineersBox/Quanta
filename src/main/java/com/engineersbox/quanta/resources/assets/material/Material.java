package com.engineersbox.quanta.resources.assets.material;

import com.engineersbox.quanta.resources.assets.object.Mesh;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class Material {

    public static final Vector4f DEFAULT_COLOR = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

    private Vector4f ambientColor;
    private float reflectance;
    private Vector4f specularColor;
    private Vector4f diffuseColor;
    private final List<Mesh> meshes;
    private String texturePath;

    public Material() {
        this.diffuseColor = Material.DEFAULT_COLOR;
        this.ambientColor = Material.DEFAULT_COLOR;
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

    public Vector4f getDiffuseColor() {
        return this.diffuseColor;
    }

    public void setDiffuseColor(final Vector4f diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    public Vector4f getAmbientColor() {
        return this.ambientColor;
    }

    public void setAmbientColor(final Vector4f ambientColor) {
        this.ambientColor = ambientColor;
    }

    public float getReflectance() {
        return this.reflectance;
    }

    public void setReflectance(final float reflectance) {
        this.reflectance = reflectance;
    }

    public Vector4f getSpecularColor() {
        return this.specularColor;
    }

    public void setSpecularColor(final Vector4f specularColor) {
        this.specularColor = specularColor;
    }


}
