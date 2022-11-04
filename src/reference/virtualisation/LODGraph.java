package com.engineersbox.yajge.scene.element.object.composite.virtualisation;

import com.engineersbox.yajge.core.window.Window;
import com.engineersbox.yajge.scene.element.object.composite.Mesh;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.Graph;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class LODGraph {

    private final Mesh initialMesh;
    private final List<List<LODLevel>> levels;
    private final int initialVertexClusterThreshold;
    private final int initialGroupingSize;
    private final int lodLevels;

    public LODGraph(final Mesh mesh,
                    final int lodLevels) {
        this(
                mesh,
                128,
                4,
                lodLevels
        );
    }

    public LODGraph(final Mesh mesh,
                    final int initialVertexClusterThreshold,
                    final int initialGroupingSize,
                    final int lodLevels) {
        this.initialMesh = mesh;
        this.levels = new ArrayList<>();
        this.initialVertexClusterThreshold = initialVertexClusterThreshold;
        this.initialGroupingSize = initialGroupingSize;
        this.lodLevels = lodLevels;
        for (int i = 0; i < lodLevels; i++) {
            this.levels.add(new ArrayList<>());
        }
    }

    public void build() {
        final Graph graph = new Graph(this.initialMesh);
        // 1. Cluster
        List<VertexCluster> clustering = ClusterGenerator.findClustersThreshold(
                graph,
                this.initialVertexClusterThreshold
        );
        int groupingSize = this.initialGroupingSize;
        // 2. Create LODLevels for each cluster
        List<LODLevel> currentLevels = this.levels.get(this.lodLevels - 1);
        for (final VertexCluster cluster : clustering) {
            currentLevels.add(new LODLevel(
                    this.lodLevels - 1,
                    0,
                    List.of(cluster.copy()),
                    new ArrayList<>()
            ));
        }
        for (int i = this.lodLevels - 2; i >= 0; i--) {
            // 3. Get previous lod levels
            final List<LODLevel> previousLodLevels = this.levels.get(i + 1);
            // 4. Group clusters and Update LODLevels with parent LODLevel containing group
            final List<List<VertexCluster>> grouping = groupClusters(clustering, groupingSize /= 2, previousLodLevels, i);
            // 5. Merge clusters in groups
            final List<VertexCluster> mergedClusters = mergeGroups(grouping);
            // 6. Simplify groups
            final List<VertexCluster> simplifiedClusters = new ArrayList<>(); // TODO: refactor QuadricErrorSimplifier to work on VertexCluster, with target as 50%
            for (final LODLevel level : previousLodLevels) {
                for (final LODLevel current : level.parents()) {
                    current.clusters().clear();
                    current.clusters().add(simplifiedClusters.get(current.groupIndex()).copy());
                }
            }
            // 7. Reduce group to new clusters
            final Map<VertexCluster, List<VertexCluster>> mergeMappings = ClusterGenerator.findClustersCount(simplifiedClusters, simplifiedClusters.size() / 2);
            // 8. Update LODLevel parents to remap group to next level clusters
            for (final LODLevel level : previousLodLevels) {
                for (final LODLevel current : level.parents()) {
                    final List<VertexCluster> newMappings = mergeMappings.get(current.clusters().get(0))
                            .stream()
                            .map(VertexCluster::copy)
                            .toList();
                    current.clusters().clear();
                    current.clusters().addAll(newMappings);
                }
            }
        }
    }

    private List<VertexCluster> mergeGroups(final List<List<VertexCluster>> groups) {
        return groups.stream()
                .map((final List<VertexCluster> group) -> group.stream().reduce((final VertexCluster a, final VertexCluster b) -> {
                        final VertexCluster newA = a.copy();
                        newA.merge(b.copy());
                        return newA;
                })).filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private List<List<VertexCluster>> groupClusters(final List<VertexCluster> clusters,
                                                    final int maxSize,
                                                    final List<LODLevel> previousLodLevels,
                                                    final int level) {
        final List<List<VertexCluster>> groups = new ArrayList<>();
        final LinkedList<VertexCluster> clusterQueue = new LinkedList<>(clusters);
        int index = 0;
        while (!clusterQueue.isEmpty()) {
            final VertexCluster cluster = clusterQueue.pop();
            final List<VertexCluster> neighbours = getNeighbours(cluster, clusters, maxSize);
            final LODLevel previousLodLevel = findPreviousLodLevel(cluster, previousLodLevels);
            final LODLevel nextLODLevel = new LODLevel(
                    level,
                    index,
                    neighbours,
                    new ArrayList<>()
            );
            this.levels.get(level).add(nextLODLevel);
            previousLodLevel.parents().add(nextLODLevel);
            clusterQueue.removeAll(neighbours);
            neighbours.add(cluster);
            groups.add(neighbours);
            index++;
        }
        return groups;
    }

    private LODLevel findPreviousLodLevel(final VertexCluster cluster,
                                                final List<LODLevel> previousLodLevels) {
        return previousLodLevels.stream()
                .filter((final LODLevel level) -> level.clusters().contains(cluster))
                .findFirst()
                .orElse(null);
    }

    private List<VertexCluster> getNeighbours(final VertexCluster cluster,
                                              final List<VertexCluster> clusters,
                                              final int maxSize) {
        return clusters.stream()
                .filter((final VertexCluster vc) -> !vc.equals(cluster))
                .filter((final VertexCluster vc) -> CollectionUtils.intersection(
                        cluster.getVertices(),
                        vc.getVertices()
                ).size() > 0)
                .limit(maxSize)
                .toList();
    }


    public void viewDependentCut(final Window window) {
        // TODO: Filter LOD levels on whether a cluster's triangles can be rendered based on if they are larger than a pixel
    }

    public void serialiseStreamingStores(final String outputDirectory) {
        // TODO: Create serialised versions of the LOD graph and write them to the directory, then replace the
        //       vertex data in LODLevel instances in the graph with filename references
    }

    private void lookupVertexData(final String reference) {
        // TODO: Load vertex data for this LOD designated by the given reference
    }
}
