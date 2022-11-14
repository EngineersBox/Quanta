package com.engineersbox.quanta.resources.loader;

import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.*;
import com.engineersbox.quanta.utils.ListUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;

public class ModelLoader {

    public static final int MAX_BONES = 150;
    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();

    private ModelLoader() {
        // Utility class
    }

    private static void buildFrameMatrices(final AIAnimation aiAnimation,
                                           final List<Bone> boneList,
                                           final AnimatedFrame animatedFrame,
                                           final int frame,
                                           final Node node,
                                           final Matrix4f parentTransformation,
                                           final Matrix4f globalInverseTransform) {
        final String nodeName = node.getName();
        final AINodeAnim aiNodeAnim = ModelLoader.findAIAnimNode(aiAnimation, nodeName);
        Matrix4f nodeTransform = node.getNodeTransformation();
        if (aiNodeAnim != null) {
            nodeTransform = ModelLoader.buildNodeTransformationMatrix(aiNodeAnim, frame);
        }
        final Matrix4f nodeGlobalTransform = new Matrix4f(parentTransformation).mul(nodeTransform);
        final List<Bone> affectedBones = boneList.stream().filter(b -> b.boneName().equals(nodeName)).toList();
        for (final Bone bone : affectedBones) {
            final Matrix4f boneTransform = new Matrix4f(globalInverseTransform).mul(nodeGlobalTransform).
                    mul(bone.offsetMatrix());
            animatedFrame.boneMatrices()[bone.boneId()] = boneTransform;
        }
        for (final Node childNode : node.getChildren()) {
            ModelLoader.buildFrameMatrices(aiAnimation, boneList, animatedFrame, frame, childNode, nodeGlobalTransform,
                    globalInverseTransform);
        }
    }

    private static Matrix4f buildNodeTransformationMatrix(final AINodeAnim aiNodeAnim,
                                                          final int frame) {
        final AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
        final AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
        final AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();
        AIVectorKey aiVecKey;
        AIVector3D vec;
        final Matrix4f nodeTransform = new Matrix4f();
        final int numPositions = aiNodeAnim.mNumPositionKeys();
        if (numPositions > 0) {
            aiVecKey = positionKeys.get(Math.min(numPositions - 1, frame));
            vec = aiVecKey.mValue();
            nodeTransform.translate(vec.x(), vec.y(), vec.z());
        }
        final int numRotations = aiNodeAnim.mNumRotationKeys();
        if (numRotations > 0) {
            final AIQuatKey quatKey = rotationKeys.get(Math.min(numRotations - 1, frame));
            final AIQuaternion aiQuat = quatKey.mValue();
            final Quaternionf quat = new Quaternionf(aiQuat.x(), aiQuat.y(), aiQuat.z(), aiQuat.w());
            nodeTransform.rotate(quat);
        }
        final int numScalingKeys = aiNodeAnim.mNumScalingKeys();
        if (numScalingKeys > 0) {
            aiVecKey = scalingKeys.get(Math.min(numScalingKeys - 1, frame));
            vec = aiVecKey.mValue();
            nodeTransform.scale(vec.x(), vec.y(), vec.z());
        }
        return nodeTransform;
    }

    private static Node buildNodesTree(final AINode aiNode,
                                       final Node parentNode) {
        final String nodeName = aiNode.mName().dataString();
        final Node node = new Node(nodeName, parentNode, ModelLoader.toMatrix(aiNode.mTransformation()));
        final int numChildren = aiNode.mNumChildren();
        final PointerBuffer aiChildren = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            final AINode aiChildNode = AINode.create(aiChildren.get(i));
            final Node childNode = ModelLoader.buildNodesTree(aiChildNode, node);
            node.addChild(childNode);
        }
        return node;
    }

    private static int calcAnimationMaxFrames(final AIAnimation aiAnimation) {
        int maxFrames = 0;
        final int numNodeAnims = aiAnimation.mNumChannels();
        final PointerBuffer aiChannels = aiAnimation.mChannels();
        for (int i = 0; i < numNodeAnims; i++) {
            final AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
            final int numFrames = Math.max(Math.max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()),
                    aiNodeAnim.mNumRotationKeys());
            maxFrames = Math.max(maxFrames, numFrames);
        }

        return maxFrames;
    }

    private static AINodeAnim findAIAnimNode(final AIAnimation aiAnimation,
                                             final String nodeName) {
        AINodeAnim result = null;
        final int numAnimNodes = aiAnimation.mNumChannels();
        final PointerBuffer aiChannels = aiAnimation.mChannels();
        for (int i = 0; i < numAnimNodes; i++) {
            final AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
            if (nodeName.equals(aiNodeAnim.mNodeName().dataString())) {
                result = aiNodeAnim;
                break;
            }
        }
        return result;
    }

    public static Model loadModel(final String modelId,
                                  final String modelPath,
                                  final TextureCache textureCache,
                                  final boolean animation) {
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
                        | (animation ? 0 : aiProcess_PreTransformVertices)
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
        final Material defaultMaterial = new Material();
        final List<Bone> boneList = new ArrayList<>();
        for (int i = 0; i < numMeshes; i++) {
            final AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            final Mesh mesh = ModelLoader.processMesh(aiMesh, boneList);
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
        List<Animation> animations = new ArrayList<>();
        final int numAnimations = aiScene.mNumAnimations();
        if (numAnimations > 0) {
            final Node rootNode = ModelLoader.buildNodesTree(aiScene.mRootNode(), null);
            final Matrix4f globalInverseTransformation = ModelLoader.toMatrix(aiScene.mRootNode().mTransformation()).invert();
            animations = ModelLoader.processAnimations(aiScene, boneList, rootNode, globalInverseTransformation);
        }
        aiReleaseImport(aiScene);
        return new Model(modelId, materialList, animations);
    }

    private static List<Animation> processAnimations(final AIScene aiScene, final List<Bone> boneList,
                                                     final Node rootNode, final Matrix4f globalInverseTransformation) {
        final List<Animation> animations = new ArrayList<>();
        // Process all animations
        final int numAnimations = aiScene.mNumAnimations();
        final PointerBuffer aiAnimations = aiScene.mAnimations();
        for (int i = 0; i < numAnimations; i++) {
            final AIAnimation aiAnimation = AIAnimation.create(aiAnimations.get(i));
            final int maxFrames = ModelLoader.calcAnimationMaxFrames(aiAnimation);
            final List<AnimatedFrame> frames = new ArrayList<>();
            final Animation animation = new Animation(aiAnimation.mName().dataString(), aiAnimation.mDuration(), frames);
            animations.add(animation);
            for (int j = 0; j < maxFrames; j++) {
                final Matrix4f[] boneMatrices = new Matrix4f[ModelLoader.MAX_BONES];
                Arrays.fill(boneMatrices, ModelLoader.IDENTITY_MATRIX);
                final AnimatedFrame animatedFrame = new AnimatedFrame(boneMatrices);
                ModelLoader.buildFrameMatrices(aiAnimation, boneList, animatedFrame, j, rootNode,
                        rootNode.getNodeTransformation(), globalInverseTransformation);
                frames.add(animatedFrame);
            }
        }
        return animations;
    }

    private static float[] processBiTangents(final AIMesh aiMesh, final float[] normals) {
        final AIVector3D.Buffer buffer = aiMesh.mBitangents();
        float[] data = new float[buffer.remaining() * 3];
        int pos = 0;
        while (buffer.remaining() > 0) {
            final AIVector3D aiBitangent = buffer.get();
            data[pos++] = aiBitangent.x();
            data[pos++] = aiBitangent.y();
            data[pos++] = aiBitangent.z();
        }
        // Assimp may not calculate tangents with models that do not have texture coordinates. Just create empty values
        if (data.length == 0) {
            data = new float[normals.length];
        }
        return data;
    }

    private static AnimMeshData processBones(final AIMesh aiMesh, final List<Bone> boneList) {
        final List<Integer> boneIds = new ArrayList<>();
        final List<Float> weights = new ArrayList<>();
        final Map<Integer, List<VertexWeight>> weightSet = new HashMap<>();
        final int numBones = aiMesh.mNumBones();
        final PointerBuffer aiBones = aiMesh.mBones();
        for (int i = 0; i < numBones; i++) {
            final AIBone aiBone = AIBone.create(aiBones.get(i));
            final int id = boneList.size();
            final Bone bone = new Bone(
                    id,
                    aiBone.mName().dataString(),
                    ModelLoader.toMatrix(aiBone.mOffsetMatrix())
            );
            boneList.add(bone);
            final int numWeights = aiBone.mNumWeights();
            final AIVertexWeight.Buffer aiWeights = aiBone.mWeights();
            for (int j = 0; j < numWeights; j++) {
                final AIVertexWeight aiWeight = aiWeights.get(j);
                final VertexWeight vw = new VertexWeight(
                        bone.boneId(),
                        aiWeight.mVertexId(),
                        aiWeight.mWeight()
                );
                List<VertexWeight> vertexWeightList = weightSet.get(vw.vertexId());
                if (vertexWeightList == null) {
                    vertexWeightList = new ArrayList<>();
                    weightSet.put(vw.vertexId(), vertexWeightList);
                }
                vertexWeightList.add(vw);
            }
        }
        final int numVertices = aiMesh.mNumVertices();
        for (int i = 0; i < numVertices; i++) {
            final List<VertexWeight> vertexWeightList = weightSet.get(i);
            final int size = vertexWeightList != null ? vertexWeightList.size() : 0;
            for (int j = 0; j < Mesh.MAX_WEIGHTS; j++) {
                if (j < size) {
                    final VertexWeight vw = vertexWeightList.get(j);
                    weights.add(vw.weight());
                    boneIds.add(vw.boneId());
                } else {
                    weights.add(0.0f);
                    boneIds.add(0);
                }
            }
        }
        return new AnimMeshData(
                ListUtils.listFloatToArray(weights),
                ListUtils.listIntToArray(boneIds)
        );
    }

    private static int[] processIndices(final AIMesh aiMesh) {
        final List<Integer> indices = new ArrayList<>();
        final int numFaces = aiMesh.mNumFaces();
        final AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            final AIFace aiFace = aiFaces.get(i);
            final IntBuffer buffer = aiFace.mIndices();
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

            int result = aiGetMaterialColor(
                    aiMaterial,
                    AI_MATKEY_COLOR_AMBIENT,
                    aiTextureType_NONE,
                    0,
                    color
            );
            if (result == aiReturn_SUCCESS) {
                material.setAmbientColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
            }

            result = aiGetMaterialColor(
                    aiMaterial,
                    AI_MATKEY_COLOR_DIFFUSE,
                    aiTextureType_NONE,
                    0,
                    color
            );
            if (result == aiReturn_SUCCESS) {
                material.setDiffuseColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
            }

            result = aiGetMaterialColor(
                    aiMaterial,
                    AI_MATKEY_COLOR_SPECULAR,
                    aiTextureType_NONE,
                    0,
                    color
            );
            if (result == aiReturn_SUCCESS) {
                material.setSpecularColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
            }

            float reflectance = 0.0f;
            final float[] shininessFactor = new float[]{0.0f};
            final int[] pMax = new int[]{1};
            result = aiGetMaterialFloatArray(
                    aiMaterial,
                    AI_MATKEY_SHININESS_STRENGTH,
                    aiTextureType_NONE,
                    0,
                    shininessFactor,
                    pMax
            );
            if (result != aiReturn_SUCCESS) {
                reflectance = shininessFactor[0];
            }
            material.setReflectance(reflectance);

            final AIString aiTexturePath = AIString.calloc(stack);
            aiGetMaterialTexture(
                    aiMaterial,
                    aiTextureType_DIFFUSE,
                    0,
                    aiTexturePath,
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
            final AIString aiNormalMapPath = AIString.calloc(stack);
            Assimp.aiGetMaterialTexture(
                    aiMaterial,
                    aiTextureType_NORMALS,
                    0,
                    aiNormalMapPath,
                    (IntBuffer) null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            final String normalMapPath = aiNormalMapPath.dataString();
            if (normalMapPath != null && normalMapPath.length() > 0) {
                material.setNormalMapPath(modelDir + File.separator + new File(normalMapPath).getName());
                textureCache.createTexture(material.getNormalMapPath());
            }
            return material;
        }
    }

    private static Mesh processMesh(final AIMesh aiMesh, final List<Bone> boneList) {
        final float[] vertices = ModelLoader.processVertices(aiMesh);
        final float[] normals = ModelLoader.processNormals(aiMesh);
        final float[] tangents = ModelLoader.processTangents(aiMesh, normals);
        final float[] biTangents = ModelLoader.processBiTangents(aiMesh, normals);
        float[] textCoords = ModelLoader.processTextCoords(aiMesh);
        final int[] indices = ModelLoader.processIndices(aiMesh);
        final AnimMeshData animMeshData = ModelLoader.processBones(aiMesh, boneList);
        // Texture coordinates may not have been populated. We need at least the empty slots
        if (textCoords.length == 0) {
            final int numElements = (vertices.length / 3) * 2;
            textCoords = new float[numElements];
        }
        return new Mesh(
                vertices,
                normals,
                tangents,biTangents,
                textCoords,
                indices,
                animMeshData.boneIds(),
                animMeshData.weights()
        );
    }

    private static float[] processNormals(final AIMesh aiMesh) {
        final AIVector3D.Buffer buffer = aiMesh.mNormals();
        final float[] data = new float[buffer.remaining() * 3];
        int pos = 0;
        while (buffer.remaining() > 0) {
            final AIVector3D normal = buffer.get();
            data[pos++] = normal.x();
            data[pos++] = normal.y();
            data[pos++] = normal.z();
        }
        return data;
    }

    private static float[] processTangents(final AIMesh aiMesh,
                                           final float[] normals) {
        final AIVector3D.Buffer buffer = aiMesh.mTangents();
        float[] data = new float[buffer.remaining() * 3];
        int pos = 0;
        while (buffer.remaining() > 0) {
            final AIVector3D aiTangent = buffer.get();
            data[pos++] = aiTangent.x();
            data[pos++] = aiTangent.y();
            data[pos++] = aiTangent.z();
        }
        // Assimp may not calculate tangents with models that do not have texture coordinates. Just create empty values
        if (data.length == 0) {
            data = new float[normals.length];
        }
        return data;
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

    private static Matrix4f toMatrix(final AIMatrix4x4 aiMatrix4x4) {
        final Matrix4f result = new Matrix4f();
        result.m00(aiMatrix4x4.a1());
        result.m10(aiMatrix4x4.a2());
        result.m20(aiMatrix4x4.a3());
        result.m30(aiMatrix4x4.a4());
        result.m01(aiMatrix4x4.b1());
        result.m11(aiMatrix4x4.b2());
        result.m21(aiMatrix4x4.b3());
        result.m31(aiMatrix4x4.b4());
        result.m02(aiMatrix4x4.c1());
        result.m12(aiMatrix4x4.c2());
        result.m22(aiMatrix4x4.c3());
        result.m32(aiMatrix4x4.c4());
        result.m03(aiMatrix4x4.d1());
        result.m13(aiMatrix4x4.d2());
        result.m23(aiMatrix4x4.d3());
        result.m33(aiMatrix4x4.d4());
        return result;
    }

}
