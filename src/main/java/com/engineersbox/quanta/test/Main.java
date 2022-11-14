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
import com.engineersbox.quanta.scene.lighting.DirectionalLight;
import com.engineersbox.quanta.scene.lighting.SceneLights;
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

    private static final int NUM_CHUNKS = 4;

    private Entity[][] terrainEntities;

    private Entity cubeEntity;
    private final Vector4f displInc = new Vector4f();
    private float rotation;
    private LightControls lightControls;
    private int lightAngle;

    @Override
    public void init(final Window window, final Scene scene, final Renderer renderer) {
        final String wallNoNormalsModelId = "quad-no-normals-model";
        final Model quadModelNoNormals = ModelLoader.loadModel(wallNoNormalsModelId, "assets/models/wall/wall_nonormals.obj",
                scene.getTextureCache());
        scene.addModel(quadModelNoNormals);

        final Entity wallLeftEntity = new Entity("wallLeftEntity", wallNoNormalsModelId);
        wallLeftEntity.setPosition(-3f, 0, 0);
        wallLeftEntity.setScale(2.0f);
        wallLeftEntity.updateModelMatrix();
        scene.addEntity(wallLeftEntity);

        final String wallModelId = "quad-model";
        final Model quadModel = ModelLoader.loadModel(wallModelId, "assets/models/wall/wall.obj",
                scene.getTextureCache());
        scene.addModel(quadModel);

        final Entity wallRightEntity = new Entity("wallRightEntity", wallModelId);
        wallRightEntity.setPosition(3f, 0, 0);
        wallRightEntity.setScale(2.0f);
        wallRightEntity.updateModelMatrix();
        scene.addEntity(wallRightEntity);

        final SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.2f);
        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        dirLight.setPosition(1, 1, 0);
        dirLight.setIntensity(1.0f);
        scene.setSceneLights(sceneLights);

        final Camera camera = scene.getCamera();
        camera.moveUp(5.0f);
        camera.addRotation((float) Math.toRadians(90), 0);

        this.lightAngle = -35;
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
        if (window.isKeyPressed(GLFW_KEY_LEFT)) {
            this.lightAngle -= 2.5f;
            if (this.lightAngle < -90) {
                this.lightAngle = -90;
            }
        } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
            this.lightAngle += 2.5f;
            if (this.lightAngle > 90) {
                this.lightAngle = 90;
            }
        }
        final MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            final Vector2f displayVec = mouseInput.getDisplayVec();
            camera.addRotation(
                    (float) Math.toRadians(displayVec.x * ConfigHandler.CONFIG.mouse.sensitivity),
                    (float) Math.toRadians(displayVec.y * ConfigHandler.CONFIG.mouse.sensitivity)
            );
        }
        final SceneLights sceneLights = scene.getSceneLights();
        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        final double angRad = Math.toRadians(this.lightAngle);
        dirLight.getDirection().x = (float) Math.sin(angRad);
        dirLight.getDirection().y = (float) Math.cos(angRad);
    }

    @Override
    public void update(final Window window, final Scene scene, final long diffTimeMillis) {
//        updateTerrain(scene);
    }

    public void updateTerrain(final Scene scene) {
        final int cellSize = 10;
        final Vector3f cameraPos = scene.getCamera().getPosition();
        final int cellCol = (int) (cameraPos.x / cellSize);
        final int cellRow = (int) (cameraPos.z / cellSize);
        final int numRows = Main.NUM_CHUNKS * 2 + 1;
        int zOffset = -Main.NUM_CHUNKS;
        final float scale = cellSize / 2.0f;
        for (int j = 0; j < numRows; j++) {
            int xOffset = -Main.NUM_CHUNKS;
            for (int i = 0; i < numRows; i++) {
                final Entity entity = this.terrainEntities[j][i];
                entity.setScale(scale);
                entity.setPosition((cellCol + xOffset) * 2.0f, 0, (cellRow + zOffset) * 2.0f);
                entity.getModelMatrix().identity().scale(scale).translate(entity.getPosition());
                xOffset++;
            }
            zOffset++;
        }
    }

    @Override
    public void cleanup() {
    }

}