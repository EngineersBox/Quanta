#version 330

const int MAX_POINT_LIGHTS = 5;
const int MAX_SPOT_LIGHTS = 5;
const float SPECULAR_POWER = 10;
const int NUM_CASCADES = 3;
// const float BIAS = 0.0005;
// const float SHADOW_FACTOR = 0.25;
// const vec3 brightnessThreshold = vec3(0.2126, 0.7152, 0.0722);

layout (location = 0) out vec4 FragColor;
layout (location = 1) out vec4 BrightColor;

in vec2 outTextCoord;

struct Attenuation {
    float constant;
    float linear;
    float exponent;
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

struct ShadowCascade {
    mat4 projectionViewMatrix;
    float splitDistance;
};

uniform sampler2D albedoSampler;
uniform sampler2D normalSampler;
uniform sampler2D specularSampler;
uniform sampler2D depthSampler;

uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;

uniform AmbientLight ambientLight;
uniform PointLight pointLights[MAX_POINT_LIGHTS];
uniform SpotLight spotLights[MAX_SPOT_LIGHTS];
uniform DirectionalLight directionalLight;
uniform Fog fog;
uniform ShadowCascade shadowCascade[NUM_CASCADES];
uniform sampler2D shadowMap_0;
uniform sampler2D shadowMap_1;
uniform sampler2D shadowMap_2;

uniform bool showCascades;
uniform bool showDepth;
uniform bool showShadows;

uniform float farPlane;
uniform float shadowFactor;
uniform float shadowBias;
uniform vec3 brightnessThreshold;

vec4 calcAmbient(AmbientLight ambientLight, vec4 ambient) {
    return vec4(ambientLight.factor * ambientLight.color, 1) * ambient;
}

vec4 calcLightColor(vec4 diffuse, vec4 specular, float reflectance, vec3 lightColor, float light_intensity, vec3 position, vec3 to_light_dir, vec3 normal) {
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
    specColor = specular * light_intensity  * specularFactor * reflectance * vec4(lightColor, 1.0);

    return (diffuseColor + specColor);
}

vec4 calcPointLight(vec4 diffuse, vec4 specular, float reflectance, PointLight light, vec3 position, vec3 normal) {
    vec3 light_direction = light.position - position;
    vec3 to_light_dir  = normalize(light_direction);
    vec4 light_color = calcLightColor(diffuse, specular, reflectance, light.color, light.intensity, position, to_light_dir, normal);

    // Apply Attenuation
    float distance = length(light_direction);
    float attenuationInv = light.att.constant + light.att.linear * distance + light.att.exponent * distance * distance;
    return light_color / attenuationInv;
}

vec4 calcSpotLight(vec4 diffuse, vec4 specular, float reflectance, SpotLight light, vec3 position, vec3 normal) {
    vec3 light_direction = light.pl.position - position;
    vec3 to_light_dir  = normalize(light_direction);
    vec3 from_light_dir  = -to_light_dir;
    float spot_alpha = dot(from_light_dir, normalize(light.coneDir));

    vec4 color = vec4(0, 0, 0, 0);

    if (spot_alpha > light.cutoff) {
        color = calcPointLight(diffuse, specular, reflectance, light.pl, position, normal);
        color *= (1.0 - (1.0 - spot_alpha)/(1.0 - light.cutoff));
    }
    return color;
}

vec4 calcDirectionalLight(vec4 diffuse, vec4 specular, float reflectance, DirectionalLight light, vec3 position, vec3 normal) {
    return calcLightColor(diffuse, specular, reflectance, light.color, light.intensity, position, normalize(light.direction), normal);
}

vec4 calcFog(vec3 pos, vec4 color, Fog fog, vec3 ambientLight, DirectionalLight directionalLight) {
    vec3 fogColor = fog.color * (ambientLight + directionalLight.color * directionalLight.intensity);
    float distance = length(pos);
    float fogFactor = 1.0 / exp((distance * fog.density) * (distance * fog.density));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 resultColor = mix(fogColor, color.xyz, fogFactor);
    return vec4(resultColor.xyz, color.w);
}

float textureProj(vec4 shadowCoord, vec2 offset, int idx) {
    float shadow = 1.0;

    if (shadowCoord.z > -1.0 && shadowCoord.z < 1.0) {
        float dist = 0.0;
        if (idx == 0) {
            dist = texture(shadowMap_0, vec2(shadowCoord.xy + offset * (1.0 / textureSize(shadowMap_0, 0)))).r;
        } else if (idx == 1) {
            dist = texture(shadowMap_1, vec2(shadowCoord.xy + offset * (1.0 / textureSize(shadowMap_0, 0)))).r;
        } else {
            dist = texture(shadowMap_2, vec2(shadowCoord.xy + offset * (1.0 / textureSize(shadowMap_0, 0)))).r;
        }
        if (dist < shadowCoord.z - shadowBias) {
            shadow = shadowFactor;
        }
    }
    return shadow;
}

float calcShadow(vec4 worldPosition, int idx) {
    vec4 shadowMapPosition = shadowCascade[idx].projectionViewMatrix * worldPosition;
    float shadow = 0.0;
    vec4 shadowCoord = (shadowMapPosition / shadowMapPosition.w) * 0.5 + 0.5;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            shadow += textureProj(shadowCoord, vec2(x, y), idx);
        }
    }
    return shadow / 9.0;
}

void main() {
    vec4 diffuse = texture(albedoSampler, outTextCoord);
    vec4 normalTexel = texture(normalSampler, outTextCoord);

    float reflectance = normalTexel.a;
    vec3 normal = normalize(2.0 * normalTexel.rgb  - 1.0);
    vec4 specular = texture(specularSampler, outTextCoord);

    // Retrieve position from depth
    float rawDepth = texture(depthSampler, outTextCoord).x;
    if (showDepth) {
        FragColor.rgb = vec3(rawDepth / farPlane);
        return;
    }
    float depth = rawDepth * 2.0 - 1.0;
    if (depth == 1) {
        discard;
    }
    vec4 clip      = vec4(outTextCoord.x * 2.0 - 1.0, outTextCoord.y * 2.0 - 1.0, depth, 1.0);
    vec4 view_w    = inverseProjectionMatrix * clip;
    vec3 view_pos  = view_w.xyz / view_w.w;
    vec4 world_pos = inverseViewMatrix * vec4(view_pos, 1);

    vec4 diffuseSpecularComp = calcDirectionalLight(diffuse, specular, reflectance, directionalLight, view_pos, normal);

    int cascadeIndex;
    for (int i = 0; i < NUM_CASCADES - 1; i++) {
        if (view_pos.z < shadowCascade[i].splitDistance) {
            cascadeIndex = i + 1;
            break;
        }
    }
    float shadowFactor = calcShadow(world_pos, cascadeIndex);
    if (showShadows) {
        FragColor.rgb = vec3(shadowFactor, shadowFactor, shadowFactor);
        return;
    }

    for (int i = 0; i < MAX_POINT_LIGHTS; i++) {
        if (pointLights[i].intensity > 0) {
            diffuseSpecularComp += calcPointLight(diffuse, specular, reflectance, pointLights[i], view_pos, normal);
        }
    }

    for (int i = 0; i < MAX_SPOT_LIGHTS; i++) {
        if (spotLights[i].pl.intensity > 0) {
            diffuseSpecularComp += calcSpotLight(diffuse, specular, reflectance, spotLights[i], view_pos, normal);
        }
    }
    vec4 ambient = calcAmbient(ambientLight, diffuse);
    FragColor = ambient + diffuseSpecularComp;
    FragColor.rgb = FragColor.rgb * shadowFactor;
    if (fog.activeFog == 1) {
        FragColor = calcFog(view_pos, FragColor, fog, ambientLight.color, directionalLight);
    }
    float brightness = dot(FragColor.rgb, brightnessThreshold);
    if (brightness > 1.0) {
        BrightColor = vec4(FragColor.rgb, 1.0);
    } else {
        BrightColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
#define RENDER_CASCADE(r,g,b) FragColor.rgb *= vec3(r,g,b); break;
    if (showCascades) {
        switch (cascadeIndex) {
            case 0:  RENDER_CASCADE( 1.0f, 0.25f, 0.25f)
            case 1:  RENDER_CASCADE(0.25f,  1.0f, 0.25f)
            case 2:  RENDER_CASCADE(0.25f, 0.25f,  1.0f)
            default: RENDER_CASCADE( 1.0f,  1.0f, 0.25f)
        }
    }
}