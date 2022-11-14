#version 330

in vec2 frgTextCoords;
in vec4 frgColor;

uniform sampler2D textureSampler;

out vec4 outColor;

void main() {
    outColor = frgColor  * texture(textureSampler, frgTextCoords);
}