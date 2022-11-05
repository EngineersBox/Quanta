package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private final SceneRenderer sceneRenderer;

    public Renderer() {
        this.sceneRenderer = new SceneRenderer();
    }

    public void cleanup() {
        this.sceneRenderer.cleanup();
    }

    public void render(final Window window,
                       final Scene scene) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        this.sceneRenderer.render(window, scene);
    }

}
