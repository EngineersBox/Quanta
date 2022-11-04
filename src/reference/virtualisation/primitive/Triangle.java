package com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive;

import org.joml.Vector3f;

public class Triangle {

    private final int[] v = new int[3];
    private final double[] error = new double[4];
    private boolean deleted = false;
    private boolean dirty = false;
    private final Vector3f n = new Vector3f();

    public Triangle(final int a, final int b, final int c) {
        this.getVertices()[0] = a;
        this.getVertices()[1] = b;
        this.getVertices()[2] = c;
    }

    public int[] getVertices() {
        return this.v;
    }

    public double[] getError() {
        return this.error;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    public Vector3f getNormal() {
        return this.n;
    }
}
