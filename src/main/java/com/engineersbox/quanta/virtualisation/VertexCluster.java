package com.engineersbox.yajge.scene.element.object.composite.virtualisation;

import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.Graph;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.GraphEdge;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph.GraphVertex;

import java.util.HashSet;
import java.util.Set;

public class VertexCluster {

    private final Graph graph;

    private Set<GraphVertex> vertices = new HashSet<>();

    public VertexCluster(final Graph graph) {
        this.graph = graph;
    }

    public void addVertex(final GraphVertex vertexId) {
        this.vertices.add(vertexId);
    }

    public float distanceTo(final VertexCluster other) {
        if (this.equals(other)) {
            return 0f;
        }
        float distance = Float.POSITIVE_INFINITY;
        for (final GraphEdge edge : getOutgoingEdges()) {
            if ((other.vertices.contains(edge.v1()) || other.vertices.contains(edge.v2())) && edge.length() < distance) {
                distance = edge.length();
            }
        }
        return distance;
    }

    public Set<GraphVertex> getVertices() {
        return this.vertices;
    }

    public void merge(final VertexCluster other) {
        if (!this.graph.equals(other.graph)) {
            throw new IllegalArgumentException();
        }
        this.vertices.addAll(other.vertices);
    }

    public void removeVertex(final GraphVertex vertexId) {
        this.vertices.remove(vertexId);
    }

    public int size() {
        return this.vertices.size();
    }

    @Override
    public String toString() {
        return "Cluster [vertices=" + this.vertices + "]";
    }

    private Set<GraphEdge> getOutgoingEdges() {
        final Set<GraphEdge> outgoingEdges = new HashSet<>();
        for (final GraphVertex vertex : this.vertices) {
            for (final GraphEdge edge : this.graph.getOutgoingEdges(vertex)) {
                if (!this.vertices.contains(edge.v2())) {
                    outgoingEdges.add(edge);
                }
            }
        }
        return outgoingEdges;
    }

    public VertexCluster copy() {
        final VertexCluster cluster = new VertexCluster(this.graph);
        cluster.vertices = new HashSet<>(this.vertices);
        return cluster;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof VertexCluster vco)) {
            return false;
        }
        return this.vertices.equals(vco.vertices);
    }
}
