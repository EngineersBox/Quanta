package com.engineersbox.quanta.resources.assets.object.animation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Matrix4f;

public class AnimatedFrame {

    private Matrix4f[] boneMatrices;
    private int offset;

    public AnimatedFrame(final Matrix4f[] boneMatrices) {
        this.boneMatrices = boneMatrices;
    }

    @JsonCreator
    public AnimatedFrame(@JsonProperty("bone_matricies") final Matrix4f[] boneMatrices,
                         @JsonProperty("offset") final int offset) {
        this(boneMatrices);
        this.offset = offset;
    }

    public void clearData() {
        this.boneMatrices = null;
    }

    @JsonProperty("bone_matricies")
    public Matrix4f[] getBoneMatrices() {
        return this.boneMatrices;
    }

    @JsonProperty("offset")
    public int getOffset() {
        return this.offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

}
