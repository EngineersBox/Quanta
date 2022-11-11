package com.engineersbox.quanta.rendering.view;

import com.engineersbox.quanta.resources.config.ConfigHandler;
import org.joml.Matrix4f;

public class Projection {

    private final Matrix4f projectionMatrix;

    public Projection(final int width,
                      final int height) {
        this.projectionMatrix = new Matrix4f();
        updateProjectionMatrix(width, height);
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public void updateProjectionMatrix(final int width, final int height) {
        this.projectionMatrix.setPerspective(
                (float) Math.toRadians(ConfigHandler.CONFIG.render.camera.fov),
                (float) width / height,
                (float) ConfigHandler.CONFIG.render.camera.zNear,
                (float) ConfigHandler.CONFIG.render.camera.zFar
        );
    }
}
