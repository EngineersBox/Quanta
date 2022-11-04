package com.engineersbox.yajge.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.lwjgl.opengl.GL11.GL_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;

public record OpenGLInfo(String version,
                         String glslVersion,
                         String vendor,
                         String renderer,
                         int extensions) {

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
