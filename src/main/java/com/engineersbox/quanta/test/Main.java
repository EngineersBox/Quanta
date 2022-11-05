package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.resources.object.Mesh;
import com.engineersbox.quanta.scene.Scene;

public class Main implements IAppLogic {

    public static void main(final String[] args) {
        final Main main = new Main();
        final Engine engine = new Engine(
                "Quanta",
                main
        );
        engine.start();
    }

    @Override
    public void init(final Window window, final Scene scene, final Renderer renderer) {
        final float[] positions = new float[]{
                -0.5f, 0.5f, -1.0f,
                -0.5f, -0.5f, -1.0f,
                0.5f, -0.5f, -1.0f,
                0.5f, 0.5f, -1.0f,
        };
        final float[] colors = new float[]{
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
        };
        final int[] indices = new int[]{
                0, 1, 3, 3, 1, 2,
        };
        final Mesh mesh = new Mesh(positions, colors, indices);
        scene.addMesh("quad", mesh);
    }

    @Override
    public void input(final Window window, final Scene scene, final long diffTimeMillis) {

    }

    @Override
    public void update(final Window window, final Scene scene, final long diffTimeMillis) {

    }

    @Override
    public void cleanup() {

    }
}