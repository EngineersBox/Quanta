package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;
import com.engineersbox.quanta.device.gpu.texture.MemoryTexture;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;

public class FBO extends GPUResource {

    private final FBOType type;

    public FBO(final FBOType type) {
        super.id = glGenFramebuffers();
        this.type = type;
    }

    @Override
    public void validate() {
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cannot complete framebuffer");
        }
    }

    @Override
    public void bind() {
        glBindFramebuffer(this.type.glType(), super.id);
    }

    @Override
    public void unbind() {
        glBindFramebuffer(this.type.glType(), 0);
    }

    public void attach(final MemoryTexture texture,
                       final int textureTarget3D,
                       final int attachment,
                       final int level) {
        switch (texture.getType()) {
            case T1D -> glFramebufferTexture1D(
                    this.type.glType(),
                    attachment,
                    texture.getType().glType(),
                    texture.getId(),
                    level
            );
            case T2D -> glFramebufferTexture2D(
                    this.type.glType(),
                    attachment,
                    texture.getType().glType(),
                    texture.getId(),
                    level
            );
            case T3D -> glFramebufferTexture3D(
                    this.type.glType(),
                    attachment,
                    textureTarget3D,
                    texture.getType().glType(),
                    texture.getId(),
                    level
            );
        }
    }

    @Override
    public void destroy() {
        glDeleteFramebuffers(super.id);
    }

}
