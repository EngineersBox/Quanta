package com.engineersbox.quanta.rendering.indirect;

import com.engineersbox.quanta.scene.Entity;

public record AnimMeshDrawData(Entity entity,
                               int bindingPoseOffset,
                               int weightsOffset) {
}
