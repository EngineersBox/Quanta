package com.engineersbox.quanta.resources.assets.object.builtin;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.HeightMapMesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.builtin.primitive.Box2D;
import com.engineersbox.quanta.resources.loader.ResourceLoader;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import org.joml.Vector3f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

public class Terrain extends Model {

    private static final String TERRAIN_PATH = "@quanta__TERRAIN_MODEL";

    private final HeightMapMesh heightMapMesh;
    private final Box2D[][] boundingBoxes;
    private final int size;
    private final int verticesPerCol;
    private final int verticesPerRow;

    public Terrain(final String id,
                   final int terrainSize,
                   final float scale,
                   final float minY,
                   final float maxY,
                   final int textInc,
                   final String heightMapFile,
                   final String textureFile,
                   final MaterialCache materialCache,
                   final TextureCache textureCache) {
        this(
                id,
                terrainSize,
                scale,
                minY,
                maxY,
                textInc,
                heightMapFile,
                textureFile,
                materialCache,
                textureCache,
                false
        );
    }

    public Terrain(final String id,
                   final int terrainSize,
                   final float scale,
                   final float minY,
                   final float maxY,
                   final int textInc,
                   final String heightMapFile,
                   final String textureFile,
                   final MaterialCache materialCache,
                   final TextureCache textureCache,
                   final boolean classPathResource) {
        super(
                id,
                TERRAIN_PATH
        );
        this.size = terrainSize;
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer w = stack.mallocInt(1);
            final IntBuffer h = stack.mallocInt(1);
            final IntBuffer avChannels = stack.mallocInt(1);
            final ByteBuffer decodedImage;
            if (classPathResource) {
                final ByteBuffer rawData = ResourceLoader.loadResource(heightMapFile);
                if (rawData == null) {
                    throw new IllegalStateException("Unable to find height map at " + heightMapFile);
                }
                decodedImage = stbi_load_from_memory(
                        rawData,
                        w,
                        h,
                        avChannels,
                        4
                );
                MemoryUtil.memFree(rawData);
            } else {
                decodedImage = stbi_load(
                        heightMapFile,
                        w,
                        h,
                        avChannels,
                        4
                );
            }
            final int width = w.get();
            final int height = h.get();
            this.verticesPerCol = width - 1;
            this.verticesPerRow = height - 1;
            this.heightMapMesh = new HeightMapMesh(
                    minY,
                    maxY,
                    decodedImage,
                    width,
                    height,
                    textInc
            );
            textureCache.createTexture(textureFile);
            final Material material = new Material();
            material.setTexturePath(textureFile);
            materialCache.addMaterial(material);
            this.heightMapMesh.getMeshData().setMaterialIdx(material.getMaterialIdx());
            this.boundingBoxes = new Box2D[terrainSize][terrainSize];
            for (int row = 0; row < terrainSize; row++) {
                for (int col = 0; col < terrainSize; col++) {
                    final float xDisplacement = (col - ((float) terrainSize - 1) / (float) 2) * scale * HeightMapMesh.getXLength();
                    final float zDisplacement = (row - ((float) terrainSize - 1) / (float) 2) * scale * HeightMapMesh.getZLength();

                    final Entity terrainBlock = new Entity(
                            id + "_" + (row + col),
                            super.getId()
                    );
                    terrainBlock.setScale(scale);
                    terrainBlock.setPosition(xDisplacement, 0, zDisplacement);
                    this.entities.add(terrainBlock);

                    this.boundingBoxes[row][col] = getBoundingBox(terrainBlock);
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public float getHeight(final Vector3f position) {
        Box2D boundingBox = null;
        boolean found = false;
        Entity terrainChunk = null;
        for (int row = 0; row < this.size && !found; row++) {
            for (int col = 0; col < this.size && !found; col++) {
                terrainChunk = this.entities.get(row * this.size + col);
                boundingBox = this.boundingBoxes[row][col];
                found = boundingBox.contains(position.x, position.z);
            }
        }

        if (found) {
            final Vector3f[] triangle = getTriangle(position, boundingBox, terrainChunk);
            return interpolateHeight(triangle[0], triangle[1], triangle[2], position.x, position.z);
        }

        return Float.MIN_VALUE;
    }

    protected Vector3f[] getTriangle(final Vector3f position,
                                     final Box2D boundingBox,
                                     final Entity terrainChunk) {
        final float cellWidth = boundingBox.width() / (float) this.verticesPerCol;
        final float cellHeight = boundingBox.height() / (float) this.verticesPerRow;
        final int col = (int) ((position.x - boundingBox.x()) / cellWidth);
        final int row = (int) ((position.z - boundingBox.y()) / cellHeight);

        final Vector3f[] triangle = new Vector3f[3];
        triangle[1] = new Vector3f(
                boundingBox.x() + col * cellWidth,
                getWorldHeight(row + 1, col, terrainChunk),
                boundingBox.y() + (row + 1) * cellHeight
        );
        triangle[2] = new Vector3f(
                boundingBox.x() + (col + 1) * cellWidth,
                getWorldHeight(row, col + 1, terrainChunk),
                boundingBox.y() + row * cellHeight
        );

        if (position.z < getDiagonalZCoord(triangle[1].x, triangle[1].z, triangle[2].x, triangle[2].z, position.x)) {
            triangle[0] = new Vector3f(
                    boundingBox.x() + col * cellWidth,
                    getWorldHeight(row, col, terrainChunk),
                    boundingBox.y() + row * cellHeight
            );
        } else {
            triangle[0] = new Vector3f(
                    boundingBox.x() + (col + 1) * cellWidth,
                    getWorldHeight(row + 2, col + 1, terrainChunk),
                    boundingBox.y() + (row + 1) * cellHeight
            );
        }

        return triangle;
    }

    protected float getDiagonalZCoord(final float x1,
                                      final float z1,
                                      final float x2,
                                      final float z2,
                                      final float x) {
        return ((z1 - z2) / (x1 - x2)) * (x - x1) + z1;
    }

    protected float getWorldHeight(final int row,
                                   final int col,
                                   final Entity sceneElement) {
        final float y = this.heightMapMesh.getHeight(row, col);
        return y * sceneElement.getScale() + sceneElement.getPosition().y;
    }

    protected float interpolateHeight(final Vector3f pA,
                                      final Vector3f pB,
                                      final Vector3f pC,
                                      final float x,
                                      final float z) {
        final float a = (pB.y - pA.y) * (pC.z - pA.z) - (pC.y - pA.y) * (pB.z - pA.z);
        final float b = (pB.z - pA.z) * (pC.x - pA.x) - (pC.z - pA.z) * (pB.x - pA.x);
        final float c = (pB.x - pA.x) * (pC.y - pA.y) - (pC.x - pA.x) * (pB.y - pA.y);
        final float d = -(a * pA.x + b * pA.y + c * pA.z);
        return (-d - a * x - c * z) / b;
    }

    private Box2D getBoundingBox(final Entity terrainChunk) {
        final float scale = terrainChunk.getScale();
        final Vector3f position = terrainChunk.getPosition();

        final float topLeftX = HeightMapMesh.STARTX * scale + position.x;
        final float topLeftZ = HeightMapMesh.STARTZ * scale + position.z;
        final float width = Math.abs(HeightMapMesh.STARTX * 2) * scale;
        final float height = Math.abs(HeightMapMesh.STARTZ * 2) * scale;
        return new Box2D(topLeftX, topLeftZ, width, height);
    }

    public void bind(final Scene scene) {
        scene.addModel(this);
        super.entities.forEach(scene::addEntity);
    }

}
