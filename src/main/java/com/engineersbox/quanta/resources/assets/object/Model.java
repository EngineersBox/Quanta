package com.engineersbox.quanta.resources.assets.object;

import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.resources.assets.object.animation.Animation;
import com.engineersbox.quanta.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {

    private final String id;
    private final List<Animation> animationList;
    private final List<Entity> entitiesList;
    private final List<MeshData> meshData;
    private final List<MeshDrawData> meshDrawData;

    public Model(final String id,
                 final List<MeshData> meshData,
                 final List<Animation> animationList) {
        this.entitiesList = new ArrayList<>();
        this.id = id;
        this.meshData = meshData;
        this.animationList = animationList;
        this.meshDrawData = new ArrayList<>();
    }

    public List<Animation> getAnimationList() {
        return this.animationList;
    }

    public List<Entity> getEntities() {
        return this.entitiesList;
    }

    public String getId() {
        return this.id;
    }

    public List<MeshData> getMeshDataList() {
        return this.meshData;
    }

    public List<MeshDrawData> getMeshDrawDataList() {
        return this.meshDrawData;
    }

    public boolean isAnimated() {
        return this.animationList != null && !this.animationList.isEmpty();
    }

}
