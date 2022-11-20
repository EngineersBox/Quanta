package com.engineersbox.quanta.rendering.indirect;

import com.engineersbox.quanta.scene.Entity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record AnimMeshDrawData(Entity entity,
                               int bindingPoseOffset,
                               int weightsOffset) {
}
