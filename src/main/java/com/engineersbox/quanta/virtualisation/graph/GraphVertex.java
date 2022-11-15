package com.engineersbox.quanta.virtualisation.graph;

import com.engineersbox.quanta.virtualisation.primitive.Vertex;

public record GraphVertex(Vertex vertex, Graph parent) {

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final GraphVertex other = (GraphVertex) obj;
        if (!parent().equals(other.parent())) return false;
        if (vertex == null) return other.vertex == null;
        return vertex.equals(other.vertex);
    }

    @Override
    public String toString() {
        return this.vertex.getPosition().toString();
    }

}
