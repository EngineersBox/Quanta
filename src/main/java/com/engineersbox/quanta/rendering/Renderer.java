package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private final SceneRenderer sceneRenderer;
    private final GUIRenderer guiRenderer;
    private final SkyBoxRenderer skyBoxRenderer;

    public Renderer(final Window window) {
        this.sceneRenderer = new SceneRenderer();
        this.guiRenderer = new GUIRenderer(window);
        this.skyBoxRenderer = new SkyBoxRenderer();
    }

    public void cleanup() {
        this.sceneRenderer.cleanup();
        this.guiRenderer.cleanup();
        this.skyBoxRenderer.cleanup();
    }

    public void render(final Window window,
                       final Scene scene) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        this.skyBoxRenderer.render(scene);
        this.sceneRenderer.render(window, scene);
        this.guiRenderer.render(scene);
    }

    public void resize(final int width,
                       final int height) {
        this.guiRenderer.resize(width, height);
    }
}
