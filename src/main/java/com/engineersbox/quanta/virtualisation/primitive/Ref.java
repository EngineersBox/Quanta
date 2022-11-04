package com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive;

public class Ref {

    private int triangleId;
    private int triangleVertex;

    public Ref(final int triangleId, final int triangleVertex) {
        this.setTriangleId(triangleId);
        this.setTriangleVertex(triangleVertex);
    }

    public int getTriangleId() {
        return this.triangleId;
    }

    public void setTriangleId(final int triangleId) {
        this.triangleId = triangleId;
    }

    public int getTriangleVertex() {
        return this.triangleVertex;
    }

    public void setTriangleVertex(final int triangleVertex) {
        this.triangleVertex = triangleVertex;
    }
}
