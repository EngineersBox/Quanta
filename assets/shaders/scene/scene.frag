#version 330

#extension GL_ARB_shading_language_include: require
#include </quanta/shader_config.incl>

// const int MAX_MATERIALS = 100;
// const int MAX_TEXTURES = 100;

in vec3 outNormal;
in vec3 outTangent;
in vec3 outBitangent;
in vec2 outTextCoord;
in vec4 outViewPosition;
in vec4 outWorldPosition;
flat in uint outMaterialIdx;

layout (location = 0) out vec4 buffAlbedo;
layout (location = 1) out vec4 buffNormal;
layout (location = 2) out vec4 buffSpecular;
layout (location = 3) out vec4 buffPosition;

struct Material {
    vec4 diffuse;
    vec4 specular;
    float reflectance;
    int normalMapIdx;
    int textureIdx;
};

uniform sampler2D textureSampler[MAX_TEXTURES];
uniform Material materials[MAX_MATERIALS];

uniform bool showNormals;

vec3 calcNormal(int idx, vec3 normal, vec3 tangent, vec3 bitangent, vec2 textCoords) {
    mat3 TBN = mat3(tangent, bitangent, normal);
    vec3 newNormal = texture(textureSampler[idx], textCoords).rgb;
    newNormal = normalize(newNormal * 2.0 - 1.0);
    newNormal = normalize(TBN * newNormal);
    return newNormal;
}

void main() {
    buffPosition = outViewPosition;
    Material material = materials[outMaterialIdx];
    vec4 text_color = texture(textureSampler[material.textureIdx], outTextCoord);
    if (text_color.a < 0.1) {
        discard;
    }
    vec4 diffuse = text_color + material.diffuse;
    if (diffuse.a < 1.0) {
        discard;
    }
    vec4 specular = text_color + material.specular;

    vec3 normal = outNormal;
    if (material.normalMapIdx > 0) {
        normal = calcNormal(material.normalMapIdx, outNormal, outTangent, outBitangent, outTextCoord);
    }

    if (showNormals) {
        buffAlbedo = vec4(0.5 * normal + 0.5, 1.0);
    } else {
        buffAlbedo = diffuse;
    }
    buffNormal = vec4(0.5 * normal + 0.5, material.reflectance);
    buffSpecular = specular;
}