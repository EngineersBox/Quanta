#version 330

in vec2 outTextCoord;
out vec4 fragColor;

uniform vec4 diffuse;
uniform sampler2D textureSampler;
uniform int hasTexture;

void main() {
    if (hasTexture == 1) {
        fragColor = texture(textureSampler, outTextCoord);
    } else {
        fragColor = diffuse;
    }
}