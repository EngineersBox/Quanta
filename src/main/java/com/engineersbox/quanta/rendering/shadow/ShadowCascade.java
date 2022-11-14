package com.engineersbox.quanta.rendering.shadow;

import com.engineersbox.quanta.scene.Scene;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class ShadowCascade {

    public static final int SHADOW_MAP_CASCADE_COUNT = 3;

    private Matrix4f projectionViewMatrix;
    private float splitDistance;

    public ShadowCascade() {
        this.projectionViewMatrix = new Matrix4f();
    }

    // Function are derived from Vulkan examples from Sascha Willems, and licensed under the MIT License:
    // https://github.com/SaschaWillems/Vulkan/tree/master/examples/shadowmappingcascade, which are based on
    // https://johanmedestrom.wordpress.com/2016/03/18/opengl-cascaded-shadow-maps/
    public static void updateCascadeShadows(final List<ShadowCascade> cascadeShadows, final Scene scene) {
        final Matrix4f viewMatrix = scene.getCamera().getViewMatrix();
        final Matrix4f projMatrix = scene.getProjection().getProjectionMatrix();
        final Vector4f lightPos = new Vector4f(scene.getSceneLights().getDirectionalLight().getDirection(), 0);

        final float cascadeSplitLambda = 0.95f;
        final float[] cascadeSplits = new float[ShadowCascade.SHADOW_MAP_CASCADE_COUNT];

        final float nearClip = projMatrix.perspectiveNear();
        final float farClip = projMatrix.perspectiveFar();
        final float clipRange = farClip - nearClip;

        final float maxZ = nearClip + clipRange;
        final float range = maxZ - nearClip;
        final float ratio = maxZ / nearClip;

        // Calculate split depths based on view camera frustum
        // Based on method presented in https://developer.nvidia.com/gpugems/GPUGems3/gpugems3_ch10.html
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            final float p = (i + 1) / (float) (ShadowCascade.SHADOW_MAP_CASCADE_COUNT);
            final float log = (float) (nearClip * java.lang.Math.pow(ratio, p));
            final float uniform = nearClip + range * p;
            final float d = cascadeSplitLambda * (log - uniform) + uniform;
            cascadeSplits[i] = (d - nearClip) / clipRange;
        }

        // Calculate orthographic projection matrix for each cascade
        float lastSplitDist = 0.0f;
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            final float splitDist = cascadeSplits[i];
            final Vector3f[] frustumCorners = new Vector3f[]{
                    new Vector3f(-1.0f, 1.0f, -1.0f),
                    new Vector3f(1.0f, 1.0f, -1.0f),
                    new Vector3f(1.0f, -1.0f, -1.0f),
                    new Vector3f(-1.0f, -1.0f, -1.0f),
                    new Vector3f(-1.0f, 1.0f, 1.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Vector3f(1.0f, -1.0f, 1.0f),
                    new Vector3f(-1.0f, -1.0f, 1.0f),
            };
            // Project frustum corners into world space
            final Matrix4f invCam = (new Matrix4f(projMatrix).mul(viewMatrix)).invert();
            for (int j = 0; j < 8; j++) {
                final Vector4f invCorner = new Vector4f(frustumCorners[j], 1.0f).mul(invCam);
                frustumCorners[j] = new Vector3f(invCorner.x / invCorner.w, invCorner.y / invCorner.w, invCorner.z / invCorner.w);
            }
            for (int j = 0; j < 4; j++) {
                final Vector3f dist = new Vector3f(frustumCorners[j + 4]).sub(frustumCorners[j]);
                frustumCorners[j + 4] = new Vector3f(frustumCorners[j]).add(new Vector3f(dist).mul(splitDist));
                frustumCorners[j] = new Vector3f(frustumCorners[j]).add(new Vector3f(dist).mul(lastSplitDist));
            }

            // Get frustum center
            final Vector3f frustumCenter = new Vector3f(0.0f);
            for (int j = 0; j < 8; j++) {
                frustumCenter.add(frustumCorners[j]);
            }
            frustumCenter.div(8.0f);

            float radius = 0.0f;
            for (int j = 0; j < 8; j++) {
                final float distance = (new Vector3f(frustumCorners[j]).sub(frustumCenter)).length();
                radius = java.lang.Math.max(radius, distance);
            }
            radius = (float) java.lang.Math.ceil(radius * 16.0f) / 16.0f;
            final Vector3f maxExtents = new Vector3f(radius);
            final Vector3f minExtents = new Vector3f(maxExtents).mul(-1);
            final Vector3f lightDir = (new Vector3f(lightPos.x, lightPos.y, lightPos.z).mul(-1)).normalize();
            final Vector3f eye = new Vector3f(frustumCenter).sub(new Vector3f(lightDir).mul(-minExtents.z));
            final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
            final Matrix4f lightViewMatrix = new Matrix4f().lookAt(eye, frustumCenter, up);
            final Matrix4f lightOrthoMatrix = new Matrix4f().ortho
                    (minExtents.x, maxExtents.x, minExtents.y, maxExtents.y, 0.0f, maxExtents.z - minExtents.z, true);

            // Store split distance and matrix in cascade
            final ShadowCascade cascadeShadow = cascadeShadows.get(i);
            cascadeShadow.splitDistance = (nearClip + splitDist * clipRange) * -1.0f;
            cascadeShadow.projectionViewMatrix = lightOrthoMatrix.mul(lightViewMatrix);
            lastSplitDist = cascadeSplits[i];
        }
    }

    public Matrix4f getProjectionViewMatrix() {
        return this.projectionViewMatrix;
    }

    public float getSplitDistance() {
        return this.splitDistance;
    }

}
