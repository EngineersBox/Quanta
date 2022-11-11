package com.engineersbox.quanta.resources.loader;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class ModelLoader {

    private ModelLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static Model loadModel(final String modelId,
                                  final String modelPath,
                                  final TextureCache textureCache) {
        return ModelLoader.loadModel(
                modelId,
                modelPath,
                textureCache,
                aiProcess_GenSmoothNormals
                        | aiProcess_JoinIdenticalVertices
                        | aiProcess_Triangulate
                        | aiProcess_FixInfacingNormals
                        | aiProcess_CalcTangentSpace
                        | aiProcess_LimitBoneWeights
                        | aiProcess_PreTransformVertices
        );

    }

    public static Model loadModel(final String modelId,
                                  final String modelPath,
                                  final TextureCache textureCache,
                                  final int flags) {
        final File file = new File(modelPath);
        if (!file.exists()) {
            throw new RuntimeException("Model path does not exist [" + modelPath + "]");
        }
        final String modelDir = file.getParent();
        final AIScene aiScene = aiImportFile(modelPath, flags);
        if (aiScene == null) {
            throw new RuntimeException("Error loading model [modelPath: " + modelPath + "]");
        }
        final int numMaterials = aiScene.mNumMaterials();
        final List<Material> materialList = new ArrayList<>();
        for (int i = 0; i < numMaterials; i++) {
            final AIMaterial aiMaterial = AIMaterial.create(aiScene.mMaterials().get(i));
            materialList.add(ModelLoader.processMaterial(aiMaterial, modelDir, textureCache));
        }
        final int numMeshes = aiScene.mNumMeshes();
        final PointerBuffer aiMeshes = aiScene.mMeshes();
        if (aiMeshes == null) {
            throw new IllegalStateException("Unable to retrieve meshes");
        }
        final Material defaultMaterial = new Material();
        for (int i = 0; i < numMeshes; i++) {
            final AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            final Mesh mesh = ModelLoader.processMesh(aiMesh);
            final int materialIdx = aiMesh.mMaterialIndex();
            final Material material;
            if (materialIdx >= 0 && materialIdx < materialList.size()) {
                material = materialList.get(materialIdx);
            } else {
                material = defaultMaterial;
            }
            material.getMeshes().add(mesh);
        }
        if (!defaultMaterial.getMeshes().isEmpty()) {
            materialList.add(defaultMaterial);
        }
        return new Model(modelId, materialList);
    }

    private static int[] processIndices(final AIMesh aiMesh) {
        final List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < aiMesh.mNumFaces(); i++) {
            final IntBuffer buffer = aiMesh.mFaces().get(i).mIndices();
            while (buffer.remaining() > 0) {
                indices.add(buffer.get());
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Material processMaterial(final AIMaterial aiMaterial,
                                            final String modelDir,
                                            final TextureCache textureCache) {
        final Material material = new Material();
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final AIColor4D color = AIColor4D.create();
            final int result = aiGetMaterialColor(
                    aiMaterial,
                    AI_MATKEY_COLOR_DIFFUSE,
                    aiTextureType_NONE,
                    0,
                    color
            );
            if (result == aiReturn_SUCCESS) {
                material.setDiffuseColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
            }
            final AIString aiTexturePath = AIString.calloc(stack);
            aiGetMaterialTexture(
                    aiMaterial,
                    aiTextureType_DIFFUSE,
                    0, aiTexturePath,
                    (IntBuffer) null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            final String texturePath = aiTexturePath.dataString();
            if (texturePath != null && texturePath.length() > 0) {
                material.setTexturePath(modelDir + File.separator + new File(texturePath).getName());
                textureCache.createTexture(material.getTexturePath());
                material.setDiffuseColor(Material.DEFAULT_COLOR);
            }
            return material;
        }
    }

    private static Mesh processMesh(final AIMesh aiMesh) {
        final float[] vertices = ModelLoader.processVertices(aiMesh);
        float[] textCoords = ModelLoader.processTextCoords(aiMesh);
        final int[] indices = ModelLoader.processIndices(aiMesh);
        // Texture coordinates may not have been populated. We need at least the empty slots
        if (textCoords.length == 0) {
            final int numElements = (vertices.length / 3) * 2;
            textCoords = new float[numElements];
        }
        return new Mesh(vertices, textCoords, indices);
    }

    private static float[] processTextCoords(final AIMesh aiMesh) {
        final AIVector3D.Buffer buffer = aiMesh.mTextureCoords(0);
        if (buffer == null) {
            return new float[]{};
        }
        final float[] data = new float[buffer.remaining() * 2];
        int pos = 0;
        while (buffer.remaining() > 0) {
            final AIVector3D textCoord = buffer.get();
            data[pos++] = textCoord.x();
            data[pos++] = 1 - textCoord.y();
        }
        return data;
    }

    private static float[] processVertices(final AIMesh aiMesh) {
        final AIVector3D.Buffer buffer = aiMesh.mVertices();
        final float[] data = new float[buffer.remaining() * 3];
        int pos = 0;
        while (buffer.remaining() > 0) {
            final AIVector3D textCoord = buffer.get();
            data[pos++] = textCoord.x();
            data[pos++] = textCoord.y();
            data[pos++] = textCoord.z();
        }
        return data;
    }

}
