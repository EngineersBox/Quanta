package com.engineersbox.quanta.resources.object;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity {

    private final String id;
    private final String modelId;
    private final Matrix4f modelMatrix;
    private final Vector3f position;
    private final Quaternionf rotation;
    private float scale;

    public Entity(final String id,
                  final String modelId) {
        this.id = id;
        this.modelId = modelId;
        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = 1;
    }

    public String getId() {
        return this.id;
    }

    public String getModelId() {
        return this.modelId;
    }

    public Matrix4f getModelMatrix() {
        return this.modelMatrix;
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Quaternionf getRotation() {
        return this.rotation;
    }

    public float getScale() {
        return this.scale;
    }

    public void setPosition(final float x, final float y, final float z) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }

    public void setPosition(final Vector3f pos) {
        setPosition(pos.x, pos.y, pos.z);
    }

    public void setRotation(final float x, final float y, final float z, final float angle) {
        this.rotation.fromAxisAngleRad(x, y, z, angle);
    }

    public void setScale(final float scale) {
        this.scale = scale;
    }

    public void updateModelMatrix() {
        this.modelMatrix.translationRotateScale(
                this.position,
                this.rotation,
                this.scale
        );
    }

}
