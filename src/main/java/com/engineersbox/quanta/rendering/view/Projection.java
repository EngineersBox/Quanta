package com.engineersbox.quanta.rendering.view;

import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.utils.serialization.JsonDeserializeExternalizable;
import com.engineersbox.quanta.utils.serialization.JsonSerializeExternalizable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Matrix4f;

public class Projection {

    private final Matrix4f projectionMatrix;
    private final Matrix4f inverseProjectionMatrix;

    public Projection(final int width,
                      final int height) {
        this(
                new Matrix4f(),
                new Matrix4f()
        );
        updateProjectionMatrix(width, height);
    }

    @JsonCreator
    public Projection(@JsonProperty("projection_matrix") @JsonDeserializeExternalizable final Matrix4f projectionMatrix,
                      @JsonProperty("inverse_projection_matrix") @JsonDeserializeExternalizable final Matrix4f inverseProjectionMatrix) {
        this.projectionMatrix = projectionMatrix;
        this.inverseProjectionMatrix = inverseProjectionMatrix;
    }

    @JsonProperty("projection_matrix")
    @JsonSerializeExternalizable
    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    @JsonProperty("inverse_projection_matrix")
    @JsonSerializeExternalizable
    public Matrix4f getInverseProjectionMatrix() {
        return this.inverseProjectionMatrix;
    }

    public void updateProjectionMatrix(final int width, final int height) {
        this.projectionMatrix.setPerspective(
                (float) Math.toRadians(ConfigHandler.CONFIG.render.camera.fov),
                (float) width / height,
                (float) ConfigHandler.CONFIG.render.camera.zNear,
                (float) ConfigHandler.CONFIG.render.camera.zFar
        );
        this.inverseProjectionMatrix.set(this.projectionMatrix).invert();
    }

    public void update(final Projection other) {
        this.projectionMatrix.set(other.projectionMatrix);
        this.inverseProjectionMatrix.set(other.inverseProjectionMatrix);
    }
}
