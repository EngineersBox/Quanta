package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.graph.Render;
import com.engineersbox.quanta.resources.object.Mesh;
import com.engineersbox.quanta.scene.Scene;

public class Main implements IAppLogic {

    public static void main(final String[] args) {
        final Main main = new Main();
        final Engine engine = new Engine(
                "Quanta",
                new Window.Options(),
                main
        );
        engine.start();
    }

    @Override
    public void init(final Window window, final Scene scene, final Render render) {
        final float[] positions = new float[]{
                0.0f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f
        };
        final Mesh mesh = new Mesh(positions, 3);
        scene.addMesh("triangle", mesh);
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