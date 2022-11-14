package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.assets.audio.SoundBuffer;
import com.engineersbox.quanta.resources.assets.audio.SoundListener;
import com.engineersbox.quanta.resources.assets.audio.SoundManager;
import com.engineersbox.quanta.resources.assets.audio.SoundSource;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimationData;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.SkyBox;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.AmbientLight;
import com.engineersbox.quanta.scene.lighting.DirectionalLight;
import com.engineersbox.quanta.scene.lighting.SceneLights;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.openal.AL11;

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
    private AnimationData animationData;
    private SoundSource playerSoundSource;
    private SoundManager soundManager;

    @Override
    public void init(final Window window, final Scene scene, final Renderer renderer) {
        final String terrainModelId = "terrain";
        final Model terrainModel = ModelLoader.loadModel(terrainModelId, "assets/models/terrain/terrain.obj",
                scene.getTextureCache(), false);
        scene.addModel(terrainModel);
        final Entity terrainEntity = new Entity("terrainEntity", terrainModelId);
        terrainEntity.setScale(100.0f);
        terrainEntity.updateModelMatrix();
        scene.addEntity(terrainEntity);

        final String bobModelId = "bobModel";
        final Model bobModel = ModelLoader.loadModel(bobModelId, "assets/models/bob/boblamp.md5mesh",
                scene.getTextureCache(), true);
        scene.addModel(bobModel);
        final Entity bobEntity = new Entity("bobEntity", bobModelId);
        bobEntity.setScale(0.05f);
        bobEntity.updateModelMatrix();
        this.animationData = new AnimationData(bobModel.getAnimations().get(0));
        bobEntity.setAnimationData(this.animationData);
        scene.addEntity(bobEntity);

        final SceneLights sceneLights = new SceneLights();
        final AmbientLight ambientLight = sceneLights.getAmbientLight();
        ambientLight.setIntensity(0.5f);
        ambientLight.setColor(0.3f, 0.3f, 0.3f);

        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        dirLight.setPosition(0, 1, 0);
        dirLight.setIntensity(1.0f);
        scene.setSceneLights(sceneLights);

        final SkyBox skyBox = new SkyBox("assets/models/skybox/skybox.obj", scene.getTextureCache());
        skyBox.getEntity().setScale(100);
        skyBox.getEntity().updateModelMatrix();
        scene.setSkyBox(skyBox);

        scene.setFog(new Fog(true, new Vector3f(0.5f, 0.5f, 0.5f), 0.02f));

        final Camera camera = scene.getCamera();
        camera.setPosition(-1.5f, 3.0f, 4.5f);
        camera.addRotation((float) Math.toRadians(15.0f), (float) Math.toRadians(390.f));

        this.lightAngle = 0;
        initSounds(bobEntity.getPosition(), camera);
    }

    private void initSounds(final Vector3f position, final Camera camera) {
        this.soundManager = new SoundManager();
        this.soundManager.setAttenuationModel(AL11.AL_EXPONENT_DISTANCE);
        this.soundManager.setListener(new SoundListener(camera.getPosition()));

        SoundBuffer buffer = new SoundBuffer("assets/sounds/creak1.ogg");
        this.soundManager.addSoundBuffer(buffer);
        this.playerSoundSource = new SoundSource(false, false);
        this.playerSoundSource.setPosition(position);
        this.playerSoundSource.setBuffer(buffer.getBufferId());
        this.soundManager.addSoundSource("CREAK", this.playerSoundSource);

        buffer = new SoundBuffer("assets/sounds/woo_scary.ogg");
        this.soundManager.addSoundBuffer(buffer);
        final SoundSource source = new SoundSource(true, true);
        source.setBuffer(buffer.getBufferId());
        this.soundManager.addSoundSource("MUSIC", source);
        source.play();
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

        this.soundManager.updateListenerPosition(camera);
    }

    @Override
    public void update(final Window window, final Scene scene, final long diffTimeMillis) {
        animationData.nextFrame();
        if (animationData.getCurrentFrameIdx() == 45) {
            playerSoundSource.play();
        }
    }

    @Override
    public void cleanup() {
        this.soundManager.cleanup();
    }

}