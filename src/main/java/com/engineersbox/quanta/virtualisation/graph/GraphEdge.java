package com.engineersbox.quanta.virtualisation.graph;

public record GraphEdge(GraphVertex v1, GraphVertex v2) {
    public float length() {
        return this.v1.vertex().getPosition().distance(v2.vertex().getPosition());
    }
}
