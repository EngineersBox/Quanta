package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Engine;
import com.engineersbox.quanta.core.IAppLogic;
import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.Renderer;
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
                // VO
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
        };
        final float[] colors = new float[]{
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
        };
        final int[] indices = new int[]{
                // Front face
                0, 1, 3, 3, 1, 2,
                // Top Face
                4, 0, 3, 5, 4, 3,
                // Right face
                3, 2, 7, 5, 3, 7,
                // Left face
                6, 1, 0, 6, 0, 4,
                // Bottom face
                2, 1, 6, 2, 6, 7,
                // Back face
                7, 6, 4, 7, 4, 5,
        };
        final List<Mesh> meshList = new ArrayList<>();
        final Mesh mesh = new Mesh(positions, colors, indices);
        meshList.add(mesh);
        final String cubeModelId = "cube-model";
        final Model model = new Model(cubeModelId, meshList);
        scene.addModel(model);

        this.cubeEntity = new Entity("cube-entity", cubeModelId);
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