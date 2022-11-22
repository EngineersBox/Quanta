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
        OpenGLInfo.LOGGER.debug("[OPENGL] OpenGL Version: {}", this.version);
        OpenGLInfo.LOGGER.debug("[OPENGL] GLSL Version: {}", this.glslVersion);
        OpenGLInfo.LOGGER.debug("[OPENGL] Vendor: {}", this.vendor);
        OpenGLInfo.LOGGER.debug("[OPENGL] Renderer: {}", this.renderer);
        OpenGLInfo.LOGGER.debug("[OPENGL] Found {} supported Extensions", this.extensions);
        if (!showExtensions) {
            return;
        }
        for (int i = 0; i < this.extensions; i++) {
            OpenGLInfo.LOGGER.trace("\t{}. {}", i + 1, glGetStringi(GL_EXTENSIONS, i));
        }

    }

}
