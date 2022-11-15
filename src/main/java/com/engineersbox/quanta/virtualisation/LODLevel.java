package com.engineersbox.quanta.virtualisation;

import java.util.List;

public record LODLevel(int index,
                       int groupIndex,
                       List<VertexCluster> clusters,
                       List<LODLevel> parents) {

}
