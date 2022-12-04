package com.engineersbox.quanta.resources.assets.object;

import com.engineersbox.quanta.utils.ListUtils;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HeightMapMesh extends Mesh {

    private static final int MAX_CHANNEL_VALUE = 255;
    private static final int MAX_COLOUR = MAX_CHANNEL_VALUE * MAX_CHANNEL_VALUE * MAX_CHANNEL_VALUE;
    public static final float START_X = -0.5f;
    public static final float START_Z = -0.5f;

    private final float minY;
    private final float maxY;
    private final float[][] heightArray;

    public HeightMapMesh(final float minY,
                         final float maxY,
                         final ByteBuffer heightMapImage,
                         final int width,
                         final int height,
                         final int texInc) {
        super();
        this.minY = minY;
        this.maxY = maxY;
        this.heightArray = new float[height][width];
        super.build(buildMesh(width, height, heightMapImage, texInc));
    }

    private MeshData buildMesh(final int width,
                               final int height,
                               final ByteBuffer heightMapImage,
                               final int texInc) {
        final float incX = getXLength() / (width - 1);
        final float incZ = getZLength() / (height - 1);
        final List<Float> positions = new ArrayList<>();
        final List<Float> texCoords = new ArrayList<>();
        final List<Integer> indices = new ArrayList<>();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                positions.add(START_X + col * incX);
                final float currentHeight = getHeight(col, row, width, heightMapImage);
                this.heightArray[row][col] = currentHeight;
                positions.add(currentHeight);
                positions.add(START_Z + row * incZ);

                texCoords.add((float) texInc * (float) col / (float) width);
                texCoords.add((float) texInc * (float) row / (float) height);

                if (col < width - 1 && row < height - 1) {
                    final int leftTop = row * width + col;
                    final int leftBottom = (row + 1) * width + col;
                    final int rightBottom = (row + 1) * width + col + 1;
                    final int rightTop = row * width + col + 1;

                    indices.add(leftTop);
                    indices.add(leftBottom);
                    indices.add(rightTop);

                    indices.add(rightTop);
                    indices.add(leftBottom);
                    indices.add(rightBottom);
                }
            }
        }
        final float[] posArr = ListUtils.floatListToArray(positions);
        final float[] normals = calcNormals(posArr, width, height);
        return new MeshData(
                posArr,
                normals,
                new float[normals.length],
                new float[normals.length],
                ListUtils.floatListToArray(texCoords),
                ListUtils.intListToArray(indices),
                new int[0],
                new float[0],
                new Vector3f(),
                new Vector3f()
        );
    }

    public float getHeight(final int row, final int col) {
        if ((row >= 0 && row < this.heightArray.length)
                && (col >= 0 && col < this.heightArray[row].length)) {
            return this.heightArray[row][col];
        }
        return 0;
    }

    public static float getXLength() {
        return Math.abs(-START_X * 2);
    }

    public static float getZLength() {
        return Math.abs(-START_Z * 2);
    }

    private float[] calcNormals(final float[] posArr,
                                final int width,
                                final int height) {
        final Vector3f v0 = new Vector3f();
        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();
        Vector3f v3 = new Vector3f();
        Vector3f v4 = new Vector3f();
        final Vector3f v12 = new Vector3f();
        final Vector3f v23 = new Vector3f();
        final Vector3f v34 = new Vector3f();
        final Vector3f v41 = new Vector3f();
        final List<Float> normals = new ArrayList<>();
        Vector3f normal = new Vector3f();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (row > 0 && row < height - 1 && col > 0 && col < width - 1) {
                    assignVertexPosition(v0, row, col, width, posArr);
                    assignVertexPosition(v1, row, col - 1, width, posArr);
                    v1 = v1.sub(v0);
                    assignVertexPosition(v2, row + 1, col, width, posArr);
                    v2 = v2.sub(v0);
                    assignVertexPosition(v3, row, col + 1, width, posArr);
                    v3 = v3.sub(v0);
                    assignVertexPosition(v4, row - 1, col, width, posArr);
                    v4 = v4.sub(v0);

                    crossNorm(v1, v2, v12);
                    crossNorm(v2, v3, v23);
                    crossNorm(v3, v4, v34);
                    crossNorm(v4, v1, v41);

                    normal = v12.add(v23).add(v34).add(v41);
                    normal.normalize();
                } else {
                    normal.x = 0;
                    normal.y = 1;
                    normal.z = 0;
                }
                normal.normalize();
                normals.add(normal.x);
                normals.add(normal.y);
                normals.add(normal.z);
            }
        }
        return ListUtils.floatListToArray(normals);
    }

    private void assignVertexPosition(final Vector3f vec,
                                      final int row,
                                      final int col,
                                      final int width,
                                      final float[] posArr) {
        final int idx = row * width * 3 + col * 3;
        vec.x = posArr[idx];
        vec.y = posArr[idx + 1];
        vec.z = posArr[idx + 2];
    }

    private void crossNorm(final Vector3f a,
                           final Vector3f b,
                           final Vector3f c) {
        a.cross(b, c);
        c.normalize();
    }

    private float getHeight(final int x,
                            final int z,
                            final int width,
                            final ByteBuffer buffer) {
        return this.minY + Math.abs(this.maxY - this.minY) * ((float) getRGB(x, z, width, buffer) / (float) MAX_COLOUR);
    }

    public static int getRGB(final int x,
                             final int z,
                             final int width,
                             final ByteBuffer buffer) {
        final byte r = buffer.get(x * 4 + z * 4 * width);
        final byte g = buffer.get(x * 4 + 1 + z * 4 * width);
        final byte b = buffer.get(x * 4 + 2 + z * 4 * width);
        final byte a = buffer.get(x * 4 + 3 + z * 4 * width);
        return ((0xFF & a) << 24)
                | ((0xFF & r) << 16)
                | ((0xFF & g) << 8)
                | (0xFF & b);
    }

}
