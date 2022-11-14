package com.engineersbox.quanta.resources.assets.object.animation;

import org.joml.Matrix4f;

public record AnimatedFrame(Matrix4f[] boneMatrices) {
}
