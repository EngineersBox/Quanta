package com.engineersbox.quanta.device.gpu.texture;

import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;

public enum TextureType {
    T1D(GL_TEXTURE),
    T2D(GL_TEXTURE_2D),
    T3D(GL_TEXTURE_3D);

    private final int glType;

    TextureType(final int glType) {
        this.glType = glType;
    }

    public int glType() {
        return this.glType;
    }
}
