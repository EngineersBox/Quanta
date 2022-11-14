#version 330

in vec2 outTextCoord;
out vec4 fragColor;

uniform vec4 diffuse;
uniform sampler2D texSampler;
uniform int hasTexture;

void main() {
    if (hasTexture == 1) {
        fragColor = texture(texSampler, outTextCoord);
    } else {
        fragColor = diffuse;
    }
}