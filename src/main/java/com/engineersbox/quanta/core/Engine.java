package com.engineersbox.quanta.core;

import com.engineersbox.quanta.graph.Render;
import com.engineersbox.quanta.scene.Scene;

public class Engine {

    public static final int TARGET_UPS = 30;
    private final IAppLogic appLogic;
    private final Window window;
    private final Render render;
    private boolean running;
    private final Scene scene;
    private final int targetFPS;
    private final int targetUPS;

    public Engine(final String title,
                  final Window.Options options,
                  final IAppLogic appLogic) {
        this.window = new Window(title, options, () -> {
            resize();
            return null;
        });
        this.targetFPS = options.fps;
        this.targetUPS = options.ups;
        this.appLogic = appLogic;
        this.render = new Render();
        this.scene = new Scene();
        this.appLogic.init(this.window, this.scene, this.render);
        this.running = true;
    }

    public void start() {
        this.running = true;
        run();
    }

    public void stop() {
        this.running = false;
    }

    private void run() {
        long initialTime = System.currentTimeMillis();
        final float timeU = 1000.0f / this.targetUPS;
        final float timeR = this.targetFPS > 0 ? 1000.0f / this.targetFPS : 0;
        float deltaUpdate = 0;
        float deltaFps = 0;

        long updateTime = initialTime;
        while (this.running && !this.window.windowShouldClose()) {
            Window.pollEvents();

            final long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;

            if (this.targetFPS <= 0 || deltaFps >= 1) {
                this.appLogic.input(this.window, this.scene, now - initialTime);
            }

            if (deltaUpdate >= 1) {
                final long diffTimeMillis = now - updateTime;
                this.appLogic.update(this.window, this.scene, diffTimeMillis);
                updateTime = now;
                deltaUpdate--;
            }

            if (this.targetFPS <= 0 || deltaFps >= 1) {
                Render.render(this.window, this.scene);
                deltaFps--;
                this.window.update();
            }
            initialTime = now;
        }

        cleanup();
    }

    private void cleanup() {
        this.appLogic.cleanup();
        this.render.cleanup();
        this.scene.cleanup();
        this.window.cleanup();
    }

    public void resize() {

    }

}
