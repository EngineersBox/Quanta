package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.resources.material.Material;
import com.engineersbox.quanta.resources.material.Texture;
import com.engineersbox.quanta.resources.object.Entity;
import com.engineersbox.quanta.resources.object.Mesh;
import com.engineersbox.quanta.resources.object.Model;
import com.engineersbox.quanta.scene.Scene;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class Main implements IAppLogic {

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

    @Override
    public void init(final Window window, final Scene scene, final Renderer renderer) {
        final float[] positions = new float[]{
                // V0
                -0.5f, 0.5f, 0.5f,
                // V1
                -0.5f, -0.5f, 0.5f,
                // V2
                0.5f, -0.5f, 0.5f,
                // V3
                0.5f, 0.5f, 0.5f,
                // V4
                -0.5f, 0.5f, -0.5f,
                // V5
                0.5f, 0.5f, -0.5f,
                // V6
                -0.5f, -0.5f, -0.5f,
                // V7
                0.5f, -0.5f, -0.5f,

                // For text coords in top face
                // V8: V4 repeated
                -0.5f, 0.5f, -0.5f,
                // V9: V5 repeated
                0.5f, 0.5f, -0.5f,
                // V10: V0 repeated
                -0.5f, 0.5f, 0.5f,
                // V11: V3 repeated
                0.5f, 0.5f, 0.5f,

                // For text coords in right face
                // V12: V3 repeated
                0.5f, 0.5f, 0.5f,
                // V13: V2 repeated
                0.5f, -0.5f, 0.5f,

                // For text coords in left face
                // V14: V0 repeated
                -0.5f, 0.5f, 0.5f,
                // V15: V1 repeated
                -0.5f, -0.5f, 0.5f,

                // For text coords in bottom face
                // V16: V6 repeated
                -0.5f, -0.5f, -0.5f,
                // V17: V7 repeated
                0.5f, -0.5f, -0.5f,
                // V18: V1 repeated
                -0.5f, -0.5f, 0.5f,
                // V19: V2 repeated
                0.5f, -0.5f, 0.5f,
        };
        final float[] textCoords = new float[]{
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.5f, 0.0f,

                0.0f, 0.0f,
                0.5f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,

                // For text coords in top face
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.0f, 1.0f,
                0.5f, 1.0f,

                // For text coords in right face
                0.0f, 0.0f,
                0.0f, 0.5f,

                // For text coords in left face
                0.5f, 0.0f,
                0.5f, 0.5f,

                // For text coords in bottom face
                0.5f, 0.0f,
                1.0f, 0.0f,
                0.5f, 0.5f,
                1.0f, 0.5f,
        };
        final int[] indices = new int[]{
                // Front face
                0, 1, 3, 3, 1, 2,
                // Top Face
                8, 10, 11, 9, 8, 11,
                // Right face
                12, 13, 7, 5, 12, 7,
                // Left face
                14, 15, 6, 4, 14, 6,
                // Bottom face
                16, 18, 19, 17, 16, 19,
                // Back face
                4, 6, 7, 5, 4, 7,};
        final Texture texture = scene.getTextureCache().createTexture("assets/cube/cube.png");
        final Material material = new Material();
        material.setTexturePath(texture.getPath());
        final List<Material> materialList = new ArrayList<>();
        materialList.add(material);

        final Mesh mesh = new Mesh(positions, textCoords, indices);
        material.getMeshes().add(mesh);
        final Model cubeModel = new Model("cube-model", materialList);
        scene.addModel(cubeModel);

        this.cubeEntity = new Entity("cube-entity", cubeModel.getId());
        this.cubeEntity.setPosition(0, 0, -2);
        scene.addEntity(this.cubeEntity);
    }

    @Override
    public void input(final Window window, final Scene scene, final long diffTimeMillis) {
        this.displInc.zero();
        if (window.isKeyPressed(GLFW_KEY_UP)) {
            this.displInc.y = 1;
        } else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
            this.displInc.y = -1;
        }
        if (window.isKeyPressed(GLFW_KEY_LEFT)) {
            this.displInc.x = -1;
        } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
            this.displInc.x = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            this.displInc.z = -1;
        } else if (window.isKeyPressed(GLFW_KEY_Q)) {
            this.displInc.z = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_Z)) {
            this.displInc.w = -1;
        } else if (window.isKeyPressed(GLFW_KEY_X)) {
            this.displInc.w = 1;
        }

        this.displInc.mul(diffTimeMillis / 1000.0f);

        final Vector3f entityPos = this.cubeEntity.getPosition();
        this.cubeEntity.setPosition(this.displInc.x + entityPos.x, this.displInc.y + entityPos.y, this.displInc.z + entityPos.z);
        this.cubeEntity.setScale(this.cubeEntity.getScale() + this.displInc.w);
        this.cubeEntity.updateModelMatrix();
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