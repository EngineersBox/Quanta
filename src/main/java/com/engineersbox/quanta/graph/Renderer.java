package com.engineersbox.quanta.graph;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;
import org.lwjgl.opengl.GL;

public class Renderer {

    private final SceneRenderer sceneRenderer;

    public Renderer() {
        GL.createCapabilities();
        this.sceneRenderer = new SceneRenderer();
    }

    public void cleanup() {
        this.sceneRenderer.cleanup();
    }

    public void render(final Window window,
                       final Scene scene) {
        this.sceneRenderer.render(window, scene);
    }

}
