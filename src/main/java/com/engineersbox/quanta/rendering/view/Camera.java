package com.engineersbox.quanta.rendering.view;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {

    private final Vector3f direction;
    private final Vector3f position;
    private final Vector3f right;
    private final Vector2f rotation;
    private final Vector3f up;
    private final Matrix4f viewMatrix;

    public Camera() {
        this.direction = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        this.position = new Vector3f();
        this.viewMatrix = new Matrix4f();
        this.rotation = new Vector2f();
    }

    public void addRotation(final float x, final float y) {
        this.rotation.add(x, y);
        recalculate();
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }

    public void moveBackwards(final float inc) {
        this.viewMatrix.positiveZ(this.direction).negate().mul(inc);
        this.position.sub(this.direction);
        recalculate();
    }

    public void moveDown(final float inc) {
        this.viewMatrix.positiveY(this.up).mul(inc);
        this.position.sub(this.up);
        recalculate();
    }

    public void moveForward(final float inc) {
        this.viewMatrix.positiveZ(this.direction).negate().mul(inc);
        this.position.add(this.direction);
        recalculate();
    }

    public void moveLeft(final float inc) {
        this.viewMatrix.positiveX(this.right).mul(inc);
        this.position.sub(this.right);
        recalculate();
    }

    public void moveRight(final float inc) {
        this.viewMatrix.positiveX(this.right).mul(inc);
        this.position.add(this.right);
        recalculate();
    }

    public void moveUp(final float inc) {
        this.viewMatrix.positiveY(this.up).mul(inc);
        this.position.add(this.up);
        recalculate();
    }

    private void recalculate() {
        this.viewMatrix.identity()
                .rotateX(this.rotation.x)
                .rotateY(this.rotation.y)
                .translate(-this.position.x, -this.position.y, -this.position.z);
    }

    public void setPosition(final float x, final float y, final float z) {
        this.position.set(x, y, z);
        recalculate();
    }

    public void setRotation(final float x, final float y) {
        this.rotation.set(x, y);
        recalculate();
    }

}
