package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.lighting.PointLight;
import com.engineersbox.quanta.scene.lighting.SceneLights;
import com.engineersbox.quanta.scene.lighting.SpotLight;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;

public class Main implements IAppLogic {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(final String[] args) {
        final Main main = new Main();
        final Engine engine = new Engine(
                "Quanta",
                main
        );
        engine.start();
    }

    private Entity cubeEntity;
    private final Vector4f displInc = new Vector4f();
    private float rotation;
    private LightControls lightControls;

    @Override
    public void init(final Window window, final Scene scene, final Renderer renderer) {
        final Model cubeModel = ModelLoader.loadModel(
                "cube-model",
                "assets/models/cube/cube.obj",
                scene.getTextureCache()
        );
        scene.addModel(cubeModel);
        this.cubeEntity = new Entity("cube-entity", cubeModel.getId());
        this.cubeEntity.setPosition(0, 0, -2);
        scene.addEntity(this.cubeEntity);
        final SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.3f);
        scene.setSceneLights(sceneLights);
        sceneLights.getPointLights().add(new PointLight(
                new Vector3f(1, 1, 1),
                new Vector3f(0, 0, -1.4f),
                1.0f
        ));
        final Vector3f coneDir = new Vector3f(0, 0, -1);
        sceneLights.getSpotLights().add(new SpotLight(new PointLight(
                new Vector3f(1, 1, 1),
                new Vector3f(0, 0, -1.4f),
                0.0f),
                coneDir,
                140.0f
        ));
        this.lightControls = new LightControls(scene);
        scene.setGUIInstance(this.lightControls);
    }

    @Override
    public void input(final Window window,
                      final Scene scene,
                      final long diffTimeMillis,
                      final boolean inputConsumed) {
        final float move = diffTimeMillis * (float) ConfigHandler.CONFIG.game.movementSpeed;
        final Camera camera = scene.getCamera();
        if (window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(move);
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackwards(move);
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(move);
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(move);
        }
        if (window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(move);
        } else if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            camera.moveDown(move);
        }

        final MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            final Vector2f displayVec = mouseInput.getDisplayVec();
            camera.addRotation(
                    (float) Math.toRadians(-displayVec.x * ConfigHandler.CONFIG.mouse.sensitivity),
                    (float) Math.toRadians(-displayVec.y * ConfigHandler.CONFIG.mouse.sensitivity)
            );
        }
    }

    @Override
    public void update(final Window window, final Scene scene, final long diffTimeMillis) {
        this.rotation += 1.5;
        if (this.rotation > 360) {
            this.rotation = 0;
        }
        this.cubeEntity.setRotation(1, 1, 1, (float) Math.toRadians(this.rotation));
        this.cubeEntity.updateModelMatrix();
    }

    @Override
    public void cleanup() {
    }

}