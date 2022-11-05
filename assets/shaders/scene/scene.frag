#version 330

in vec3 outColour;
out vec4 fragColor;

void main() {
    fragColor = vec4(outColour, 1.0);
}