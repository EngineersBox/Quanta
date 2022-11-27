package com.engineersbox.quanta.rendering.buffers;

import com.engineersbox.quanta.core.Window;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL30.*;

public class GBuffer {

    public static final int TOTAL_TEXTURES = 4;

    private final int gBufferId;
    private final int height;
    private final int[] textureIds;
    private final int width;

    public GBuffer(final Window window) {
        this.gBufferId = glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.gBufferId);

        this.textureIds = new int[GBuffer.TOTAL_TEXTURES];
        glGenTextures(this.textureIds);

        this.width = window.getWidth();
        this.height = window.getHeight();

        for (int i = 0; i < GBuffer.TOTAL_TEXTURES; i++) {
            glBindTexture(GL_TEXTURE_2D, this.textureIds[i]);
            final int attachmentType;
            if (i == GBuffer.TOTAL_TEXTURES - 1) {
                glTexImage2D(
                        GL_TEXTURE_2D,
                        0,
                        GL_DEPTH_COMPONENT32F,
                        this.width,
                        this.height,
                        0,
                        GL_DEPTH_COMPONENT,
                        GL_FLOAT,
                        (ByteBuffer) null
                );
                attachmentType = GL_DEPTH_ATTACHMENT;
            } else {
                glTexImage2D(
                        GL_TEXTURE_2D,
                        0,
                        GL_RGBA32F,
                        this.width,
                        this.height,
                        0,
                        GL_RGBA,
                        GL_FLOAT,
                        (ByteBuffer) null
                );
                attachmentType = GL_COLOR_ATTACHMENT0 + i;
            }
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    attachmentType,
                    GL_TEXTURE_2D,
                    this.textureIds[i],
                    0
            );
        }

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer intBuff = stack.mallocInt(GBuffer.TOTAL_TEXTURES);
            for (int i = 0; i < GBuffer.TOTAL_TEXTURES; i++) {
                intBuff.put(i, GL_COLOR_ATTACHMENT0 + i);
            }
            glDrawBuffers(intBuff);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void cleanup() {
        glDeleteFramebuffers(this.gBufferId);
        Arrays.stream(this.textureIds).forEach(GL30::glDeleteTextures);
    }

    public int getGBufferId() {
        return this.gBufferId;
    }

    public int getHeight() {
        return this.height;
    }

    public int[] getTextureIds() {
        return this.textureIds;
    }

    public int getWidth() {
        return this.width;
    }

}
