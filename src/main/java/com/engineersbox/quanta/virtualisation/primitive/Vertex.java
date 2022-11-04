package com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive;

import org.joml.Vector3f;

public class Vertex {

    private final Vector3f position = new Vector3f();
    private int triangleStart;
    private int triangleCount;
    private final SymetricMatrix q = new SymetricMatrix(0);
    private boolean border;

    public Vertex(final Vector3f position) {
        this.getPosition().set(position);
    }


    public Vector3f getPosition() {
        return this.position;
    }

    public int getTriangleStart() {
        return this.triangleStart;
    }

    public void setTriangleStart(final int triangleStart) {
        this.triangleStart = triangleStart;
    }

    public int getTriangleCount() {
        return this.triangleCount;
    }

    public void setTriangleCount(final int triangleCount) {
        this.triangleCount = triangleCount;
    }

    public SymetricMatrix getQ() {
        return this.q;
    }

    public boolean isBorder() {
        return this.border;
    }

    public void setBorder(final boolean border) {
        this.border = border;
    }
}
