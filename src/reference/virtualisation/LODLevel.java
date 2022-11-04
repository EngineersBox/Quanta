package com.engineersbox.yajge.scene.element.object.composite.virtualisation;

import java.util.List;

public record LODLevel(int index,
                       int groupIndex,
                       List<VertexCluster> clusters,
                       List<LODLevel> parents) {

}
