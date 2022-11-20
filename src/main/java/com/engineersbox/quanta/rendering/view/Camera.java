package com.engineersbox.quanta.rendering.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    private final Vector3f direction;
    private final Vector3f position;
    private final Vector3f right;
    private final Vector3f rotation;
    private final Vector3f up;
    private final Matrix4f viewMatrix;
    private final Matrix4f inverseViewMatrix;

    public Camera() {
        this(
                new Vector3f(),
                new Vector3f(),
                new Matrix4f(),
                new Matrix4f()
        );
    }

    @JsonCreator
    public Camera(@JsonProperty("position") final Vector3f position,
                  @JsonProperty("rotation") final Vector3f rotation,
                  @JsonProperty("view_matrix") final Matrix4f viewMatrix,
                  @JsonProperty("inverse_view_matrix") final Matrix4f inverseViewMatrix) {
        this.position = position;
        this.rotation = rotation;
        this.viewMatrix = viewMatrix;
        this.inverseViewMatrix = inverseViewMatrix;
        this.direction = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
    }

    public void addRotation(final float x, final float y, final float z) {
        this.rotation.add(x, y, z);
        recalculate();
    }

    @JsonProperty("position")
    public Vector3f getPosition() {
        return this.position;
    }

    @JsonProperty("rotation")
    public Vector3f getRotation() {
        return this.rotation;
    }

    @JsonProperty("view_matrix")
    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }

    @JsonProperty("inverse_view_matrix")
    public Matrix4f getInverseViewMatrix() {
        return this.inverseViewMatrix;
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
                .rotateZ(this.rotation.z)
                .translate(-this.position.x, -this.position.y, -this.position.z);
        this.inverseViewMatrix.set(this.viewMatrix).invert();
    }

    public void setPosition(final float x, final float y, final float z) {
        this.position.set(x, y, z);
        recalculate();
    }

    public void setRotation(final float x, final float y, final float z) {
        this.rotation.set(x, y, z);
        recalculate();
    }

}
