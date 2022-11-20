package com.engineersbox.quanta.resources.assets.material;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector4f;

public class Material {

    public static final Vector4f DEFAULT_COLOR = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

    private Vector4f ambientColor;
    private float reflectance;
    private Vector4f specularColor;
    private Vector4f diffuseColor;
    private String texturePath;
    private String normalMapPath;
    private int materialIdx;

    public Material() {
        this.diffuseColor = Material.DEFAULT_COLOR;
        this.ambientColor = Material.DEFAULT_COLOR;
        this.specularColor = Material.DEFAULT_COLOR;
        materialIdx = 0;
    }

    public void cleanup() {
    }

    @JsonProperty("texture_path")
    public String getTexturePath() {
        return this.texturePath;
    }

    @JsonProperty("texture_path")
    public void setTexturePath(final String texturePath) {
        this.texturePath = texturePath;
    }

    @JsonProperty("diffuse")
    public Vector4f getDiffuseColor() {
        return this.diffuseColor;
    }

    @JsonProperty("diffuse")
    public void setDiffuseColor(final Vector4f diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    @JsonProperty("ambient")
    public Vector4f getAmbientColor() {
        return this.ambientColor;
    }

    @JsonProperty("ambient")
    public void setAmbientColor(final Vector4f ambientColor) {
        this.ambientColor = ambientColor;
    }

    @JsonProperty("reflectance")
    public float getReflectance() {
        return this.reflectance;
    }

    @JsonProperty("reflectance")
    public void setReflectance(final float reflectance) {
        this.reflectance = reflectance;
    }

    @JsonProperty("specular")
    public Vector4f getSpecularColor() {
        return this.specularColor;
    }

    @JsonProperty("specular")
    public void setSpecularColor(final Vector4f specularColor) {
        this.specularColor = specularColor;
    }

    @JsonProperty("normal_map_path")
    public String getNormalMapPath() {
        return this.normalMapPath;
    }

    @JsonProperty("normal_map_path")
    public void setNormalMapPath(final String normalMapPath) {
        this.normalMapPath = normalMapPath;
    }

    @JsonProperty("material_index")
    public int getMaterialIdx() {
        return this.materialIdx;
    }

    @JsonProperty("material_index")
    public void setMaterialIdx(final int materialIdx) {
        this.materialIdx = materialIdx;
    }

}
