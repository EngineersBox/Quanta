package com.engineersbox.quanta.rendering.hdr;

import com.engineersbox.quanta.core.Window;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class HDRBuffer {

    private final int hdrFboId;
    private final int rboDepth;
    private final int[] colourBuffers;
    private final int[] pingPongFBOs;
    private final int[] pingPongColourBuffers;

    public HDRBuffer(final Window window) {
        this.hdrFboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, this.hdrFboId);
        this.colourBuffers = new int[2];
        for (int i = 0; i < colourBuffers.length; i++) {
            glBindTexture(GL_TEXTURE_2D, this.colourBuffers[i]);
            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA16F,
                    window.getWidth(),
                    window.getHeight(),
                    0,
                    GL_RGBA,
                    GL_FLOAT,
                    NULL
            );
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);  // we clamp to the edge as the blur filter would otherwise sample repeated texture values!
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            // attach texture to framebuffer
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0 + i,
                    GL_TEXTURE_2D,
                    this.colourBuffers[i],
                    0
            );
        }
        this.rboDepth = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, this.rboDepth);
        glRenderbufferStorage(
                GL_RENDERBUFFER,
                GL_DEPTH_COMPONENT,
                window.getWidth(),
                window.getHeight()
        );
        glFramebufferRenderbuffer(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_RENDERBUFFER,
                this.rboDepth
        );
        glDrawBuffers(new int[]{
                GL_COLOR_ATTACHMENT0,
                GL_COLOR_ATTACHMENT0 + 1
        });
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Unable to create framebuffer");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        this.pingPongFBOs = new int[2];
        glGenFramebuffers(this.pingPongFBOs);
        this.pingPongColourBuffers = new int[2];
        glGenTextures(this.pingPongColourBuffers);
        for (int i = 0; i < pingPongFBOs.length; i++) {
            glBindFramebuffer(GL_FRAMEBUFFER, this.pingPongFBOs[i]);
            glBindTexture(GL_TEXTURE_2D, this.pingPongColourBuffers[i]);
            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA16F,
                    window.getWidth(),
                    window.getHeight(),
                    0,
                    GL_RGBA,
                    GL_FLOAT,
                    NULL
            );
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); // we clamp to the edge as the blur filter would otherwise sample repeated texture values!
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D,
                    this.pingPongColourBuffers[i],
                    0
            );
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Unable to create framebuffer");
            }
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getFboId() {
        return this.hdrFboId;
    }

    public int[] getColourBuffers() {
        return this.colourBuffers;
    }

    public int[] getPingPongFBOs() {
        return this.pingPongFBOs;
    }

    public int[] getPingPongColourBuffers() {
        return this.pingPongColourBuffers;
    }

    public void cleanup() {
        glDeleteFramebuffers(this.hdrFboId);
        glDeleteTextures(this.colourBuffers);
        glDeleteRenderbuffers(this.rboDepth);
    }
}
