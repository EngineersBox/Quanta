package com.engineersbox.quanta.device.gpu.buffer;

import static org.lwjgl.opengl.GL30.*;

public enum FBOType {
    DRAW_READ(GL_FRAMEBUFFER),
    DRAW(GL_DRAW_FRAMEBUFFER),
    READ(GL_READ_FRAMEBUFFER);

    private final int glType;

    FBOType(final int glType) {
        this.glType = glType;
    }

    public int glType() {
        return this.glType;
    }

}
