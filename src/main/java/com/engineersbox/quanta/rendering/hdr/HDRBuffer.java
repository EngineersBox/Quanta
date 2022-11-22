package com.engineersbox.quanta.rendering.hdr;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.deferred.GBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class HDRBuffer {

    private final int rboId;
    private final int colourBufferId;

    public HDRBuffer(final Window window,
                     final GBuffer gBuffer) {
        this.colourBufferId = 0;
        this.rboId = 0;
//        this.colourBufferId = glGenTextures();
//        glBindTexture(GL_TEXTURE_2D, this.colourBufferId);
//        glTexImage2D(
//                GL_TEXTURE_2D,
//                0,
//                GL_RGBA16F,
//                window.getWidth(),
//                window.getHeight(),
//                0,
//                GL_RGBA,
//                GL_FLOAT,
//                NULL
//        );
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//
//        this.rboId = glGenRenderbuffers();
//        glBindRenderbuffer(GL_RENDERBUFFER, this.rboId);
//        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, window.getWidth(), window.getHeight());
//
//        glBindFramebuffer(GL_FRAMEBUFFER, gBuffer.getGBufferId());
//        glFramebufferRenderbuffer(
//                GL_FRAMEBUFFER,
//                GL_DEPTH_ATTACHMENT,
//                GL_RENDERBUFFER,
//                this.rboId
//        );
//        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
//            throw new RuntimeException("Could not create framebuffer");
//        }
//        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getColourBufferId() {
        return this.colourBufferId;
    }

    public void cleanup() {
        glDeleteBuffers(this.colourBufferId);
        glDeleteBuffers(this.rboId);
    }
}
