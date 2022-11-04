package com.engineersbox.quanta.graph;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;
import org.lwjgl.opengl.GL;

public class Render {

    private final SceneRender sceneRender;

    public Render() {
        GL.createCapabilities();
        this.sceneRender = new SceneRender();
    }

    public void cleanup() {
        this.sceneRender.cleanup();
    }

    public void render(final Window window,
                       final Scene scene) {
        this.sceneRender.render(window, scene);
    }

}
