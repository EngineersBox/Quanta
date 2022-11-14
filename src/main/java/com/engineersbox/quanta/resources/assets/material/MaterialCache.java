package com.engineersbox.quanta.resources.assets.material;

import java.util.ArrayList;
import java.util.List;

public class MaterialCache {

    public static final int DEFAULT_MATERIAL_IDX = 0;

    private final List<Material> materials;

    public MaterialCache() {
        this.materials = new ArrayList<>();
        final Material defaultMaterial = new Material();
        this.materials.add(defaultMaterial);
    }

    public void addMaterial(final Material material) {
        this.materials.add(material);
        material.setMaterialIdx(this.materials.size() - 1);
    }

    public Material getMaterial(final int idx) {
        return this.materials.get(idx);
    }

    public List<Material> getMaterials() {
        return this.materials;
    }

}
