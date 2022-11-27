package com.engineersbox.quanta.rendering.buffers;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.debug.hooks.VariableHook;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SSAOBuffer {

    private static final RandomDataGenerator RANDOM = new RandomDataGenerator();
    @VariableHook(name = "renderer.ssao.kernel_size")
    public static int KERNEL_SIZE = 64;

    private final int fbo;
    private final int blurFbo;
    private final int colourBuffer;
    private final int colourBufferBlur;
    private final int noiseTexture;
    private final List<Vector3f> kernel;

    public SSAOBuffer(final Window window) {
        this.fbo = glGenFramebuffers();
        this.blurFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        this.colourBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.colourBuffer);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RED,
                window.getWidth(),
                window.getHeight(),
                0,
                GL_RED,
                GL_FLOAT,
                NULL
        );
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                this.colourBuffer,
                0
        );
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cannot complete framebuffer");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, this.blurFbo);
        this.colourBufferBlur = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.colourBufferBlur);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RED,
                window.getWidth(),
                window.getHeight(),
                0,
                GL_RED,
                GL_FLOAT,
                NULL
        );
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                this.colourBufferBlur,
                0
        );
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cannot complete framebuffer");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        this.noiseTexture = generateNoiseTexture();
        this.kernel = generateKernel();
    }

    private int generateNoiseTexture() {
        final float[] noise = new float[16 * 3];
        for (int i = 0; i < 16; i++) {
            noise[i] = (float) RANDOM.nextUniform(0.0, 1.0) * 2.0f - 1.0f;
            noise[i + 1] = (float) RANDOM.nextUniform(0.0, 1.0) * 2.0f - 1.0f;
            noise[i + 2] = 0.0f;
        }
        final int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA32F,
                4,
                4,
                0,
                GL_RGB,
                GL_FLOAT,
                noise
        );
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        return texture;
    }

    private List<Vector3f> generateKernel() {
        final List<Vector3f> kernelValues = new ArrayList<>();
        for (int i = 0; i < this.KERNEL_SIZE; i++) {
            Vector3f sample = new Vector3f(
                    (float) RANDOM.nextUniform(0.0, 1.0) * 2.0f - 1.0f,
                    (float) RANDOM.nextUniform(0.0, 1.0) * 2.0f - 1.0f,
                    (float) RANDOM.nextUniform(0.0, 1.0)
            );
            sample = sample.normalize();
            sample = sample.mul((float) RANDOM.nextUniform(0.0, 1.0));
            float scale = (float) i / (float) this.KERNEL_SIZE;
            scale = lerp(0.1f, 1.0f, scale * scale);
            sample = sample.mul(scale);
            kernelValues.add(sample);
        }
        return kernelValues;
    }

    private float lerp(final float a,
                       final float b,
                       final float f) {
        return a + f * (b - a);
    }

    public int getFboId() {
        return this.fbo;
    }

    public int getBlurFboId() {
        return this.blurFbo;
    }

    public int getColourBuffer() {
        return this.colourBuffer;
    }

    public int getColourBufferBlur() {
        return this.colourBufferBlur;
    }

    public int getNoiseTexture() {
        return this.noiseTexture;
    }

    public List<Vector3f> getKernel() {
        return this.kernel;
    }

    public void cleanup() {
        glDeleteFramebuffers(new int[]{
                this.fbo,
                this.blurFbo
        });
        glDeleteTextures(new int[]{
                this.colourBuffer,
                this.colourBufferBlur
        });
    }

}
