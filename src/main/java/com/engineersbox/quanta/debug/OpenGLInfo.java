package com.engineersbox.quanta.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;

public record OpenGLInfo(String version,
                         String glslVersion,
                         String vendor,
                         String renderer,
                         int extensions) {

    public static OpenGLInfo retrieve() {
        final int[] supportedExtensionsCount = new int[1];
        glGetIntegerv(GL_NUM_EXTENSIONS, supportedExtensionsCount);
        return new OpenGLInfo(
                glGetString(GL_VERSION),
                glGetString(GL_SHADING_LANGUAGE_VERSION),
                glGetString(GL_VENDOR),
                glGetString(GL_RENDERER),
                supportedExtensionsCount[0]
        );
    }

    private static final Logger LOGGER = LogManager.getLogger(OpenGLInfo.class);

    public void log(final boolean showExtensions) {
        OpenGLInfo.LOGGER.info("[OPENGL] OpenGL Version: {}", this.version);
        OpenGLInfo.LOGGER.info("[OPENGL] GLSL Version: {}", this.glslVersion);
        OpenGLInfo.LOGGER.info("[OPENGL] Vendor: {}", this.vendor);
        OpenGLInfo.LOGGER.info("[OPENGL] Renderer: {}", this.renderer);
        OpenGLInfo.LOGGER.info("[OPENGL] Found {} supported Extensions", this.extensions);
        if (!showExtensions) {
            return;
        }
        for (int i = 0; i < this.extensions; i++) {
            OpenGLInfo.LOGGER.info("\t{}. {}", i + 1, glGetStringi(GL_EXTENSIONS, i));
        }

    }

}
