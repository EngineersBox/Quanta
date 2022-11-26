#version 330

const vec3 brightnessThreshold = vec3(0.2126, 0.7152, 0.0722);

layout (location = 0) out vec4 FragColor;
layout (location = 1) out vec4 BrightColor;

in vec2 outTextCoord;

uniform vec4 diffuse;
uniform sampler2D textureSampler;
uniform int hasTexture;

void main() {
    if (hasTexture == 1) {
        FragColor = texture(textureSampler, outTextCoord);
    } else {
        FragColor = diffuse;
    }
    float brightness = dot(FragColor.rgb, brightnessThreshold);
    if (brightness > 1.0) {
        BrightColor = vec4(FragColor.rgb, 1.0);
    } else {
        BrightColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}