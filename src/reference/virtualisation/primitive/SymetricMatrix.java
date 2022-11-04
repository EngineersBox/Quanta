package com.engineersbox.yajge.scene.element.object.composite.virtualisation.primitive;

public class SymetricMatrix {

    private final double[] m = new double[10];

    public SymetricMatrix(final double c) {
        for (int i = 0; i < 10; i++) {
            this.m[i] = c;
        }
    }

    public SymetricMatrix(final double m11, final double m12, final double m13, final double m14,
                          final double m22, final double m23, final double m24,
                          final double m33, final double m34,
                          final double m44) {
        this.m[0] = m11;
        this.m[1] = m12;
        this.m[2] = m13;
        this.m[3] = m14;
        this.m[4] = m22;
        this.m[5] = m23;
        this.m[6] = m24;
        this.m[7] = m33;
        this.m[8] = m34;
        this.m[9] = m44;
    }

    // Make plane
    public SymetricMatrix(final double a, final double b, final double c, final double d) {
        this.m[0] = a * a;
        this.m[1] = a * b;
        this.m[2] = a * c;
        this.m[3] = a * d;
        this.m[4] = b * b;
        this.m[5] = b * c;
        this.m[6] = b * d;
        this.m[7] = c * c;
        this.m[8] = c * d;
        this.m[9] = d * d;
    }

    public void set(final SymetricMatrix s) {
        System.arraycopy(s.m, 0, this.m, 0, this.m.length);
    }

    public double getValue(final int c) {
        return this.m[c];
    }

    // Determinant
    public double det(final int a11, final int a12, final int a13,
                      final int a21, final int a22, final int a23,
                      final int a31, final int a32, final int a33) {
        return this.m[a11] * this.m[a22] * this.m[a33] + this.m[a13] * this.m[a21] * this.m[a32] + this.m[a12] * this.m[a23] * this.m[a31]
                - this.m[a13] * this.m[a22] * this.m[a31] - this.m[a11] * this.m[a23] * this.m[a32] - this.m[a12] * this.m[a21] * this.m[a33];
    }

    public SymetricMatrix add(final SymetricMatrix n) {
        return new SymetricMatrix(
                this.m[0] + n.getValue(0),
                this.m[1] + n.getValue(1),
                this.m[2] + n.getValue(2),
                this.m[3] + n.getValue(3),
                this.m[4] + n.getValue(4),
                this.m[5] + n.getValue(5),
                this.m[6] + n.getValue(6),
                this.m[7] + n.getValue(7),
                this.m[8] + n.getValue(8),
                this.m[9] + n.getValue(9));
    }

    public void addLocal(final SymetricMatrix n) {
        this.m[0] += n.getValue(0);
        this.m[1] += n.getValue(1);
        this.m[2] += n.getValue(2);
        this.m[3] += n.getValue(3);
        this.m[4] += n.getValue(4);
        this.m[5] += n.getValue(5);
        this.m[6] += n.getValue(6);
        this.m[7] += n.getValue(7);
        this.m[8] += n.getValue(8);
        this.m[9] += n.getValue(9);
    }

}
