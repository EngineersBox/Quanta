package com.engineersbox.yajge.scene.element.object.composite.virtualisation;

import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.Graph;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.GraphEdge;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.GraphVertex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

// https://github.com/palianytsia/algorithms/blob/master/src/main/java/edu/stanford/algo/greedy/Clustering.java
public class ClusterGenerator {

    private static final Logger LOGGER = LogManager.getLogger(ClusterGenerator.class);

    public static List<VertexCluster> findClustersThreshold(final Graph graph,
                                                           final int clusterVertexThreshold) {
        ClusterGenerator.LOGGER.info("[CLUSTERING - THRESHOLD | START] Reducing graph {} with cluster threshold {}", graph, clusterVertexThreshold);
        final long timeStart = System.currentTimeMillis();
        final PriorityQueue<GraphEdge> edges = new PriorityQueue<>(graph.getEdges().size(), (final GraphEdge a, final GraphEdge b) -> {
            if (a.length() < b.length()) {
                return -1;
            } else if (a.length() > b.length()) {
                return 1;
            }
            return 0;
        });
        edges.addAll(graph.getEdges());

        // Initialisation step 2: put each point in its own cluster and create mapping between vertices and clusters
        // they belong to.
        final Map<GraphVertex, VertexCluster> clusters = new HashMap<>(graph.getVertices().size());
        for (final GraphVertex vertex : graph.getVertices()) {
            final VertexCluster cluster = new VertexCluster(graph);
            cluster.addVertex(vertex);
            clusters.put(vertex, cluster);
        }

        final List<VertexCluster> clustering = group(
                clusters,
                edges,
                clusterVertexThreshold
        );
        final long timeEnd = System.currentTimeMillis();
        final OptionalDouble average = clustering
                .stream()
                .map(VertexCluster::size)
                .mapToDouble(Double::valueOf)
                .average();
        ClusterGenerator.LOGGER.info(
                "[CLUSTERING - THRESHOLD | FINISH] Mesh clustering finished in {}ms [Clusters: {}] [Average Vertices per Cluster: {}]",
                timeEnd - timeStart,
                clustering.size(),
                average.orElse(0)
        );
        return clustering;
    }

    private static List<VertexCluster> group(final Map<GraphVertex, VertexCluster> clusters,
                                             final PriorityQueue<GraphEdge> edges,
                                             final int clusterVertexThreshold) {
        int numClusters = 0;
        while (!edges.isEmpty()) {
            final GraphEdge e = edges.remove();
            final VertexCluster clusterA = clusters.get(e.v1());
            final VertexCluster clusterB = clusters.get(e.v2());
            if (clusterA.equals(clusterB) || clusterA.size() + clusterB.size() >= clusterVertexThreshold) {
                continue;
            }
            final VertexCluster smallerCluster;
            final VertexCluster biggerCluster;
            if (clusterA.size() >= clusterB.size()) {
                smallerCluster = clusterB;
                biggerCluster = clusterA;
            } else {
                smallerCluster = clusterA;
                biggerCluster = clusterB;
            }
            final VertexCluster merged = biggerCluster.copy();
            merged.merge(smallerCluster.copy());
            if (merged.getVertices().size() >= clusterVertexThreshold) {
                continue;
            }
            for (final GraphVertex vertex : smallerCluster.getVertices()) {
                clusters.put(vertex, biggerCluster);
            }
            numClusters++;
        }
        return new ArrayList<>(clusters.values());
    }

    private static Set<GraphEdge> getClustersEdges(final List<VertexCluster> clusters) {
        final Set<GraphEdge> edges = new HashSet<>();
        for (final VertexCluster cluster : clusters) {
            final Set<GraphVertex> vertices = cluster.getVertices();
            for (final GraphVertex vertex : vertices) {
                edges.addAll(vertex.parent()
                        .getOutgoingEdges(vertex)
                        .stream()
                        .filter((final GraphEdge edge) -> vertices.containsAll(List.of(edge.v1(), edge.v2())))
                        .collect(Collectors.toSet())
                );
            }
        }
        return edges;
    }

    public static Map<VertexCluster, List<VertexCluster>> findClustersCount(final List<VertexCluster> clusters,
                                                                            final int k) {
        ClusterGenerator.LOGGER.info("[CLUSTERING - COUNT | START] Reducing clusters with cluster count {}", k);
        final long timeStart = System.currentTimeMillis();
        final PriorityQueue<GraphEdge> edges = new PriorityQueue<>(getClustersEdges(clusters).size(), (final GraphEdge a, final GraphEdge b) -> {
            if (a.length() < b.length()) {
                return -1;
            } else if (a.length() > b.length()) {
                return 1;
            }
            return 0;
        });
        // Initialisation step 2: put each point in its own cluster and create mapping between vertices and clusters
        // they belong to.
        final Map<GraphVertex, VertexCluster> vertexClusterMapping = new HashMap<>();
        for (final VertexCluster cluster : clusters) {
            for (final GraphVertex vertex : cluster.getVertices()) {
                vertexClusterMapping.put(vertex, cluster);
            }
        }
        final Map<VertexCluster, List<VertexCluster>> mergeMappings = new HashMap<>();
        // Until k clusters left, merge the clusters with smallest spacing
        int numClusters = clusters.size();
        while (numClusters > k) {
            GraphEdge edge = edges.remove();
            VertexCluster clusterA = vertexClusterMapping.get(edge.v1());
            VertexCluster clusterB = vertexClusterMapping.get(edge.v2());
            // If vertices are not already in the same cluster - merge clusters
            if (clusterA.equals(clusterB)) {
                continue;
            }
            // To reduce the number of cluster reassigments we want bigger cluster to merge the smaller one.
            VertexCluster smallerCluster;
            VertexCluster biggerCluster;
            if (clusterA.size() >= clusterB.size()) {
                smallerCluster = clusterB;
                biggerCluster = clusterA;
            } else {
                smallerCluster = clusterA;
                biggerCluster = clusterB;
            }
            biggerCluster.merge(smallerCluster);
            for (GraphVertex vertex : smallerCluster.getVertices()) {
                vertexClusterMapping.put(vertex, biggerCluster);
            }
            mergeMappings.computeIfAbsent(biggerCluster, (final VertexCluster key) -> new ArrayList<>()).add(smallerCluster);
            numClusters--;
        }
        final long timeEnd = System.currentTimeMillis();
        final OptionalDouble average = vertexClusterMapping.values()
                .stream()
                .map(VertexCluster::size)
                .mapToDouble(Double::valueOf)
                .average();
        ClusterGenerator.LOGGER.info(
                "[CLUSTERING - COUNT | FINISH] Mesh clustering finished in {}ms [Clusters: {}] [Average Vertices per Cluster: {}]",
                timeEnd - timeStart,
                vertexClusterMapping.size(),
                average.orElse(0)
        );
        return mergeMappings;
    }

    public static Graph unifyClusters(final Set<VertexCluster> clusters) {
        final Graph graph = new Graph();
        // TODO: Finish this
        return graph;
    }

}
