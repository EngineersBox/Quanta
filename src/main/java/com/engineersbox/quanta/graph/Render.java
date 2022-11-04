package com.engineersbox.quanta.graph;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.scene.Scene;
import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL11.*;

public class Render {

    public Render() {
        GL.createCapabilities();
    }

    public void cleanup() {

    }

    public static void render(final Window window,
                              final Scene scene) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

}
