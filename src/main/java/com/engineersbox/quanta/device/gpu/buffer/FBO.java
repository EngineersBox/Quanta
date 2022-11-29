package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;
import com.engineersbox.quanta.device.gpu.texture.MemoryTexture;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;

public class FBO extends GPUResource {

    private FBOType type;

    public FBO() {
        super.id = glGenFramebuffers();
    }

    @Override
    public void validate() {
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cannot complete framebuffer");
        }
    }

    public void bind(final FBOType type) {
        super.bind();
        this.type = type;
        glBindFramebuffer(this.type.glType(), super.id);
    }

    public void unbind() {
        super.unbind();
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
