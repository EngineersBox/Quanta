package com.engineersbox.quanta.resources.assets.object.animation;

import org.joml.Matrix4f;

public class AnimatedFrame {

    private Matrix4f[] boneMatrices;
    private int offset;

    public AnimatedFrame(final Matrix4f[] boneMatrices) {
        this.boneMatrices = boneMatrices;
    }

    public void clearData() {
        this.boneMatrices = null;
    }

    public Matrix4f[] getBoneMatrices() {
        return this.boneMatrices;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

}
