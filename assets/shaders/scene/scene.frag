#version 330

const int DEBUG_SHADOWS = 0;
const int MAX_POINT_LIGHTS = 5;
const int MAX_SPOT_LIGHTS = 5;
const float SPECULAR_POWER = 10;
const int NUM_CASCADES = 3;
const float BIAS = 0.0005;
const float SHADOW_FACTOR = 0.25;

in vec3 outNormal;
in vec3 outTangent;
in vec3 outBitangent;
in vec2 outTextCoord;
in vec3 outViewPosition;
in vec4 outWorldPosition;

out vec4 fragColor;

struct Attenuation {
    float constant;
    float linear;
    float exponent;
};

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float reflectance;
    int hasNormalMap;
};

struct AmbientLight {
    float factor;
    vec3 color;
};

struct PointLight {
    vec3 position;
    vec3 color;
    float intensity;
    Attenuation att;
};

struct SpotLight {
    PointLight pl;
    vec3 coneDir;
    float cutoff;
};

struct DirectionalLight {
    vec3 color;
    vec3 direction;
    float intensity;
};

struct Fog {
    int activeFog;
    vec3 color;
    float density;
};

struct CascadeShadow {
    mat4 projectionViewMatrix;
    float splitDistance;
};

uniform sampler2D texSampler;
uniform sampler2D normalSampler;
uniform Material material;
uniform AmbientLight ambientLight;
uniform PointLight pointLights[MAX_POINT_LIGHTS];
uniform SpotLight spotLights[MAX_SPOT_LIGHTS];
uniform DirectionalLight directionalLight;
uniform Fog fog;
uniform CascadeShadow cascadeshadows[NUM_CASCADES];
uniform sampler2D shadowMap_0;
uniform sampler2D shadowMap_1;
uniform sampler2D shadowMap_2;

vec4 calcAmbient(AmbientLight ambientLight, vec4 ambient) {
    return vec4(ambientLight.factor * ambientLight.color, 1) * ambient;
}

vec4 calcLightColor(vec4 diffuse, vec4 specular, vec3 lightColor, float light_intensity, vec3 position, vec3 to_light_dir, vec3 normal) {
    vec4 diffuseColor = vec4(0, 0, 0, 1);
    vec4 specColor = vec4(0, 0, 0, 1);

    // Diffuse Light
    float diffuseFactor = max(dot(normal, to_light_dir), 0.0);
    diffuseColor = diffuse * vec4(lightColor, 1.0) * light_intensity * diffuseFactor;

    // Specular Light
    vec3 camera_direction = normalize(-position);
    vec3 from_light_dir = -to_light_dir;
    vec3 reflected_light = normalize(reflect(from_light_dir, normal));
    float specularFactor = max(dot(camera_direction, reflected_light), 0.0);
    specularFactor = pow(specularFactor, SPECULAR_POWER);
    specColor = specular * light_intensity  * specularFactor * material.reflectance * vec4(lightColor, 1.0);

    return (diffuseColor + specColor);
}

vec4 calcPointLight(vec4 diffuse, vec4 specular, PointLight light, vec3 position, vec3 normal) {
    vec3 light_direction = light.position - position;
    vec3 to_light_dir  = normalize(light_direction);
    vec4 light_color = calcLightColor(diffuse, specular, light.color, light.intensity, position, to_light_dir, normal);

    // Apply Attenuation
    float distance = length(light_direction);
    float attenuationInv = light.att.constant + light.att.linear * distance +
    light.att.exponent * distance * distance;
    return light_color / attenuationInv;
}

vec4 calcSpotLight(vec4 diffuse, vec4 specular, SpotLight light, vec3 position, vec3 normal) {
    vec3 light_direction = light.pl.position - position;
    vec3 to_light_dir  = normalize(light_direction);
    vec3 from_light_dir  = -to_light_dir;
    float spot_alfa = dot(from_light_dir, normalize(light.coneDir));

    vec4 color = vec4(0, 0, 0, 0);

    if (spot_alfa > light.cutoff)
    {
        color = calcPointLight(diffuse, specular, light.pl, position, normal);
        color *= (1.0 - (1.0 - spot_alfa)/(1.0 - light.cutoff));
    }
    return color;
}

vec4 calcDirectionalLight(vec4 diffuse, vec4 specular, DirectionalLight light, vec3 position, vec3 normal) {
    return calcLightColor(diffuse, specular, light.color, light.intensity, position, normalize(light.direction), normal);
}

vec4 calcFog(vec3 pos, vec4 color, Fog fog, vec3 ambientLight, DirectionalLight directionalLight) {
    vec3 fogColor = fog.color * (ambientLight + directionalLight.color * directionalLight.intensity);
    float distance = length(pos);
    float fogFactor = 1.0 / exp((distance * fog.density) * (distance * fog.density));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 resultColor = mix(fogColor, color.xyz, fogFactor);
    return vec4(resultColor.xyz, color.w);
}

vec3 calcNormal(vec3 normal, vec3 tangent, vec3 bitangent, vec2 textCoords) {
    mat3 TBN = mat3(tangent, bitangent, normal);
    vec3 newNormal = texture(normalSampler, textCoords).rgb;
    newNormal = normalize(newNormal * 2.0 - 1.0);
    newNormal = normalize(TBN * newNormal);
    return newNormal;
}

float textureProj(vec4 shadowCoord, vec2 offset, int idx) {
    float shadow = 1.0;
    if (shadowCoord.z > -1.0 && shadowCoord.z < 1.0) {
        float dist = 0.0;
        if (idx == 0) {
            dist = texture(shadowMap_0, vec2(shadowCoord.xy + offset)).r;
        } else if (idx == 1) {
            dist = texture(shadowMap_1, vec2(shadowCoord.xy + offset)).r;
        } else {
            dist = texture(shadowMap_2, vec2(shadowCoord.xy + offset)).r;
        }
        if (shadowCoord.w > 0 && dist < shadowCoord.z - BIAS) {
            shadow = SHADOW_FACTOR;
        }
    }
    return shadow;
}

float calcShadow(vec4 worldPosition, int idx) {
    vec4 shadowMapPosition = cascadeshadows[idx].projectionViewMatrix * worldPosition;
    float shadow = 1.0;
    vec4 shadowCoord = (shadowMapPosition / shadowMapPosition.w) * 0.5 + 0.5;
    shadow = textureProj(shadowCoord, vec2(0, 0), idx);
    return shadow;
}

void main() {
    vec4 text_color = texture(texSampler, outTextCoord);
    vec4 ambient = calcAmbient(ambientLight, text_color + material.ambient);
    vec4 diffuse = text_color + material.diffuse;
    vec4 specular = text_color + material.specular;

    vec3 normal = outNormal;
    if (material.hasNormalMap > 0) {
        normal = calcNormal(outNormal, outTangent, outBitangent, outTextCoord);
    }

    vec4 diffuseSpecularComp = calcDirectionalLight(diffuse, specular, directionalLight, outViewPosition, normal);

    int cascadeIndex;
    for (int i = 0; i < NUM_CASCADES - 1; i++) {
        if (outViewPosition.z < cascadeshadows[i].splitDistance) {
            cascadeIndex = i + 1;
            break;
        }
    }
    float shadowFactor = calcShadow(outWorldPosition, cascadeIndex);

    for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
        if (pointLights[i].intensity > 0) {
            diffuseSpecularComp += calcPointLight(diffuse, specular, pointLights[i], outViewPosition, normal);
        }
    }

    for (int i = 0; i < MAX_SPOT_LIGHTS; i++) {
        if (spotLights[i].pl.intensity > 0) {
            diffuseSpecularComp += calcSpotLight(diffuse, specular, spotLights[i], outViewPosition, normal);
        }
    }
    fragColor = ambient + diffuseSpecularComp;
    fragColor.rgb = fragColor.rgb * shadowFactor;

    if (fog.activeFog == 1) {
        fragColor = calcFog(outViewPosition, fragColor, fog, ambientLight.color, directionalLight);
    }

    if (DEBUG_SHADOWS == 1) {
        switch (cascadeIndex) {
            case 0:
            fragColor.rgb *= vec3(1.0f, 0.25f, 0.25f);
            break;
            case 1:
            fragColor.rgb *= vec3(0.25f, 1.0f, 0.25f);
            break;
            case 2:
            fragColor.rgb *= vec3(0.25f, 0.25f, 1.0f);
            break;
            default :
            fragColor.rgb *= vec3(1.0f, 1.0f, 0.25f);
            break;
        }
    }
}