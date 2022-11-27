package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.EngineInitContext;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimationData;
import com.engineersbox.quanta.resources.assets.object.builtin.Cone;
import com.engineersbox.quanta.resources.assets.object.builtin.GeometryBuffer;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.SkyBox;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.AmbientLight;
import com.engineersbox.quanta.scene.lighting.DirectionalLight;
import com.engineersbox.quanta.scene.lighting.SceneLights;
import org.apache.commons.geometry.euclidean.threed.RegionBSPTree3D;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.shape.Parallelepiped;
import org.apache.commons.geometry.euclidean.threed.shape.Sphere;
import org.apache.commons.numbers.core.Precision;
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

    private AnimationData animationData1;
    private AnimationData animationData2;
    private Entity cubeEntity1;
    private Entity cubeEntity2;
    private Entity coneEntity;
    private Entity sponzaEntity;
    private float lightAngleX;
    private float lightAngleZ;
    private float rotation;
    private TestGUI console;

    @Override
    public void init(final EngineInitContext context) {
        this.console = new TestGUI(context);
        context.scene().setGUIInstance(this.console);
//        final String terrainModelId = "terrain";
//        final Model terrainModel = ModelLoader.loadModel(
//                terrainModelId,
//                "assets/models/terrain/terrain.obj",
//                context.scene().getTextureCache(),
//                context.scene().getMaterialCache(),
//                false
//        );
//        context.scene().addModel(terrainModel);
//        final Entity terrainEntity = new Entity("terrainEntity", terrainModelId);
//        terrainEntity.setScale(100.0f);
//        terrainEntity.updateModelMatrix();
//        context.scene().addEntity(terrainEntity);
//
        final String bobModelId = "bobModel";
        final Model bobModel = ModelLoader.loadModel(
                bobModelId,
                "assets/models/bob/boblamp.md5mesh",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache(),
                true
        );
        context.scene().addModel(bobModel);
        final Entity bobEntity = new Entity("bobEntity-1", bobModelId);
        bobEntity.setScale(0.05f);
        bobEntity.updateModelMatrix();
        this.animationData1 = new AnimationData(bobModel.getAnimations().get(0));
        bobEntity.setAnimationData(this.animationData1);
        context.scene().addEntity(bobEntity);

        final Entity bobEntity2 = new Entity("bobEntity-2", bobModelId);
        bobEntity2.setPosition(2, 0, 0);
        bobEntity2.setScale(0.025f);
        bobEntity2.updateModelMatrix();
        this.animationData2 = new AnimationData(bobModel.getAnimations().get(0));
        bobEntity2.setAnimationData(this.animationData2);
        context.scene().addEntity(bobEntity2);
//
        final Model cubeModel = ModelLoader.loadModel(
                "cube-model",
                "assets/models/cube/cube.obj",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache(),
                false
        );
        context.scene().addModel(cubeModel);
        this.cubeEntity1 = new Entity("cube-entity-1", cubeModel.getId());
        this.cubeEntity1.setPosition(0, 2, -1);
        this.cubeEntity1.updateModelMatrix();
        context.scene().addEntity(this.cubeEntity1);

        this.cubeEntity2 = new Entity("cube-entity-2", cubeModel.getId());
        this.cubeEntity2.setPosition(-2, 2, -1);
        this.cubeEntity2.updateModelMatrix();
        context.scene().addEntity(this.cubeEntity2);

        final Cone coneModel = new Cone(
                "cone-model",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache()
        );
        context.scene()
                .getMaterialCache()
                .getMaterial(coneModel
                        .getMeshData()
                        .get(0)
                        .getMaterialIdx()
                ).setDiffuseColor(new Vector4f(1, 0, 0, 1));
        context.scene().addModel(coneModel);
        this.coneEntity = new Entity("cone-entity", coneModel.getId());
        this.coneEntity.setPosition(1, 2, 4);
        this.coneEntity.setScale(0.25f);
        this.coneEntity.updateModelMatrix();
        context.scene().addEntity(coneEntity);

        final Precision.DoubleEquivalence precision = Precision.doubleEquivalenceOfEpsilon(1e-6);
        final RegionBSPTree3D tree = Parallelepiped.unitCube(precision).toTree();
        final Sphere sphere = Sphere.from(
                Vector3D.ZERO,
                0.65,
                precision
        );
        tree.difference(sphere.toTree(3));
        final GeometryBuffer geoBuffer = new GeometryBuffer(tree.toTriangleMesh(precision));
        final Model testGeoModel = geoBuffer.getModel(
                "test-geomtry-model",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache()
        );
        context.scene().addModel(testGeoModel);
        final Entity testGeoEntity = new Entity("test-geometry-entity", testGeoModel.getId());
        testGeoEntity.setPosition(4, 4, -4);
        testGeoEntity.updateModelMatrix();
        context.scene().addEntity(testGeoEntity);

        final Model sponzaModel = ModelLoader.loadModel(
                "sponza-model",
                "assets/models/sponza_simple/sponza.obj",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache(),
                false
        );
        context.scene().addModel(sponzaModel);
        this.sponzaEntity = new Entity("sponza-entity", sponzaModel.getId());
        context.scene().addEntity(this.sponzaEntity);

        final Model windowModel = ModelLoader.loadModel(
                "window",
                "assets/models/window/window.obj",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache(),
                false
        );
        context.scene().addModel(windowModel);
        final Entity windowEntity = new Entity("window-entity", windowModel.getId());
        windowEntity.setPosition(0, 3, -3);
        windowEntity.updateModelMatrix();
        context.scene().addEntity(windowEntity);

        final com.engineersbox.quanta.resources.assets.object.builtin.Sphere sphereModel = new com.engineersbox.quanta.resources.assets.object.builtin.Sphere(
                "sphere-model",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache()
        );
        context.scene().addModel(sphereModel);
        final Material sphereMaterial = context.scene().getMaterialCache().getMaterial(sphereModel.getMeshData().get(0).getMaterialIdx());
        sphereMaterial.setAmbientColor(new Vector4f(0.8f, 0.7f, 0.9f, 1.0f));
        sphereMaterial.setDiffuseColor(new Vector4f(0.8f, 0.7f, 0.9f, 1.0f));
        sphereMaterial.setReflectance(0.95f);
        sphereMaterial.setSpecularColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        final Entity sphereEntity = new Entity("sphere-entity", sphereModel.getId());
        sphereEntity.setPosition(-5, 2, 1);
        sphereEntity.setScale(0.5f);
        sphereEntity.updateModelMatrix();
        context.scene().addEntity(sphereEntity);

        context.renderer().setupData(
                context.scene(),
                context.window()
        );

        final SceneLights sceneLights = new SceneLights();
        final AmbientLight ambientLight = sceneLights.getAmbientLight();
        ambientLight.setIntensity(0.5f);
        ambientLight.setColor(0.3f, 0.3f, 0.3f);

        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        dirLight.setPosition(0, 60, 0);
        dirLight.setIntensity(1.0f);
        context.scene().setSceneLights(sceneLights);

        final SkyBox skyBox = new SkyBox(
                "assets/models/skybox/skybox.obj",
                context.scene().getTextureCache(),
                context.scene().getMaterialCache()
        );
        skyBox.getEntity().setScale(200);
        skyBox.getEntity().updateModelMatrix();
        context.scene().setSkyBox(skyBox);

        context.scene().setFog(new Fog(
                true,
                new Vector3f(0.5f, 0.5f, 0.5f),
                0.005f
        ));

        final Camera camera = context.scene().getCamera();
        camera.setPosition(-1.5f, 3.0f, 4.5f);
        camera.addRotation((float) Math.toRadians(15.0f), (float) Math.toRadians(390.f), 0);

//        context.renderer().setupData(
//                context.scene(),
//                context.window()
//        );
//        context.scene()
//                .getModels()
//                .entrySet()
//                .stream()
//                .filter((final Map.Entry<String, Model> entry) -> entry.getKey().equals("bobModel"))
//                .map(Map.Entry::getValue)
//                .map(Model::getEntities)
//                .map(List::stream)
//                .findFirst()
//                .ifPresent((final Stream<Entity> entities) -> entities.forEach((final Entity entity) -> {
//                    if (entity.getId().equals("bobEntity-1")) {
//                        this.animationData1 = entity.getAnimationData();
//                    } else if (entity.getId().equals("bobEntity-2")) {
//                        this.animationData2 = entity.getAnimationData();
//                    }
//                }));
//        context.scene().getModels().get("cube-model").getEntities().forEach((final Entity entity) -> {
//            if (entity.getId().equals("cube-entity-1")) {
//                this.cubeEntity1 = entity;
//            } else if (entity.getId().equals("cube-entity-2")) {
//                this.cubeEntity2 = entity;
//            }
//        });
        this.lightAngleX = 45.001f;
        this.lightAngleZ = 0f;
    }

    @Override
    public void input(final Window window,
                      final Scene scene,
                      final long diffTimeMillis,
                      final boolean inputConsumed) {
        if (inputConsumed) {
            return;
        }
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
            this.lightAngleX -= 2.5f;
            if (this.lightAngleX < -90) {
                this.lightAngleX = -90;
            }
        } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
            this.lightAngleX += 2.5f;
            if (this.lightAngleX > 90) {
                this.lightAngleX = 90;
            }
        }
        if (window.isKeyPressed(GLFW_KEY_UP)) {
            this.lightAngleZ -= 2.5f;
            if (this.lightAngleZ < -90) {
                this.lightAngleZ = -90;
            }
        } else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
            this.lightAngleZ += 2.5f;
            if (this.lightAngleZ > 90) {
                this.lightAngleZ = 90;
            }
        }
        final MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            final Vector2f displayVec = mouseInput.getDisplayVec();
            camera.addRotation(
                    (float) Math.toRadians(displayVec.x * ConfigHandler.CONFIG.mouse.sensitivity),
                    (float) Math.toRadians(displayVec.y * ConfigHandler.CONFIG.mouse.sensitivity),
                    0
            );
        }
        final SceneLights sceneLights = scene.getSceneLights();
        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        final double angRadX = Math.toRadians(this.lightAngleX);
        final double angRadZ = Math.toRadians(this.lightAngleZ);
        dirLight.getDirection().x = (float) Math.sin(angRadX);
        dirLight.getDirection().z = (float) Math.sin(angRadZ);
        dirLight.getDirection().y = (float) Math.cos(angRadX);
    }

    @Override
    public void update(final Window window,
                       final Scene scene,
                       final long diffTimeMillis) {
        animationData1.nextFrame();
        if (diffTimeMillis % 2 == 0) {
            animationData2.nextFrame();
        }
        this.rotation += 1.5;
        if (this.rotation > 360) {
            this.rotation = 0;
        }
        this.cubeEntity1.setRotation(1, 1, 1, (float) Math.toRadians(this.rotation));
        this.cubeEntity1.updateModelMatrix();

        this.cubeEntity2.setRotation(1, 1, 1, (float) Math.toRadians(360 - this.rotation));
        this.cubeEntity2.updateModelMatrix();
    }

    @Override
    public void cleanup() {
    }

}