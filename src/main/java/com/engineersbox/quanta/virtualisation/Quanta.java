package com.engineersbox.yajge.scene.element.object.composite.virtualisation;

import com.engineersbox.yajge.core.window.Window;
import com.engineersbox.yajge.scene.element.object.composite.Mesh;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Quanta {

    private final int lodLevels;
    private final Map<Mesh, LODGraph> meshGraph;

    public Quanta(final List<Mesh> meshes,
                  final int lodLevels) {
        this.lodLevels = lodLevels;
        this.meshGraph = meshes.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        (final Mesh mesh) -> new LODGraph(mesh, lodLevels)
                ));
    }

    public void buildLODGraph(final String serialiseOutputDirectory) {
        for (final Map.Entry<Mesh, LODGraph> meshGraph : meshGraph.entrySet()) {
            final LODGraph graph = meshGraph.getValue();
            graph.build();
            graph.serialiseStreamingStores(serialiseOutputDirectory/*, meshGraph.getKey().getName() */); // TODO: Supply mesh name to serialiser to identify it
        }
    }

    public Mesh queryLODGraph(final String meshName,
                              final Window window) {
        // TODO: Search meshGraphs for mesh and call LODGraph$viewDependentCut() and return result
        return null;
    }
}
