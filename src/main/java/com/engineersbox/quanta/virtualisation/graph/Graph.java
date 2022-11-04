package com.engineersbox.yajge.scene.element.object.composite.virtualisation.graph;

import com.engineersbox.yajge.scene.element.object.composite.Mesh;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive.Triangle;
import com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive.Vertex;
import java.util.*;

public class Graph {
    private final Set<GraphEdge> edges;
    private final Map<GraphVertex, List<GraphVertex>> adjVertices;

    public Graph() {
        this.edges = new HashSet<>();
        this.adjVertices = new HashMap<>();
    }

    public Graph(final Mesh mesh) {
        this();
        final List<Vertex> vertices = mesh.getGroupedVertices()
                .stream()
                .map(Vertex::new)
                .toList();
        final int[] meshIndices = mesh.getIndices();
        int index = 0;
        for (int i = 0; i < meshIndices.length; i += 3) {
            final Triangle triangle = new Triangle(
                    meshIndices[index++],
                    meshIndices[index++],
                    meshIndices[index++]
            );
            addGraphVertex(vertices.get(triangle.getVertices()[0]));
            addGraphVertex(vertices.get(triangle.getVertices()[1]));
            addGraphVertex(vertices.get(triangle.getVertices()[2]));
            addEdge(
                    vertices.get(triangle.getVertices()[0]),
                    vertices.get(triangle.getVertices()[1])
            );
            addEdge(
                    vertices.get(triangle.getVertices()[1]),
                    vertices.get(triangle.getVertices()[2])
            );
            addEdge(
                    vertices.get(triangle.getVertices()[2]),
                    vertices.get(triangle.getVertices()[0])
            );
        }
    }

    public Mesh reconstructMesh() {
        final Mesh mesh = new Mesh(
                new float[0],
                new float[0],
                new float[0],
                new int[0]
        );
        // TODO: Finish this
        return mesh;
    }

    public void addGraphVertex(final Vertex label) {
        this.adjVertices.putIfAbsent(new GraphVertex(label, this), new ArrayList<>());
    }

    public void removeGraphVertex(final Vertex label) {
        final GraphVertex v = new GraphVertex(label, this);
        this.adjVertices.values().forEach((final List<GraphVertex> edges) -> edges.remove(v));
        this.adjVertices.remove(new GraphVertex(label, this));
    }

    public void addEdge(final Vertex label1,
                        final Vertex label2) {
        final GraphVertex v1 = new GraphVertex(label1, this);
        final GraphVertex v2 = new GraphVertex(label2, this);
        this.adjVertices.get(v1).add(v2);
        this.adjVertices.get(v2).add(v1);
        this.edges.add(new GraphEdge(v1, v2));
    }

    public void removeEdge(final Vertex label1,
                           final Vertex label2) {
        final GraphVertex vertex1 = new GraphVertex(label1, this);
        final GraphVertex vertex2 = new GraphVertex(label2, this);
        final List<GraphVertex> vertex1Edges = this.adjVertices.get(vertex1);
        final List<GraphVertex> vertex2Edges = this.adjVertices.get(vertex2);
        if (vertex1Edges != null) {
            vertex1Edges.remove(vertex2);
        }
        if (vertex2Edges != null) {
            vertex2Edges.remove(vertex1);
        }
        this.edges.remove(new GraphEdge(vertex1, vertex2));
        this.edges.remove(new GraphEdge(vertex2, vertex1));
    }

    public List<GraphVertex> getAdjVertices(final Vertex label) {
        return this.adjVertices.get(new GraphVertex(label, this));
    }

    public List<GraphEdge> getOutgoingEdges(final GraphVertex vertex) {
        return this.adjVertices.get(vertex)
                .stream()
                .map((final GraphVertex otherVertex) -> new GraphEdge(vertex, otherVertex))
                .toList();
    }

    public Set<GraphEdge> getEdges() {
        return this.edges;
    }

    public Set<GraphVertex> getVertices() {
        return this.adjVertices.keySet();
    }

    @Override
    public String toString() {
        return String.format(
                "[Vertices: %d] [Edges: %d]",
                getVertices().size(),
                getEdges().size()
        );
    }
}
