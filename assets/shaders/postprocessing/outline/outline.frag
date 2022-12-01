#version 330

out vec4 FragColor;

uniform vec4 outlineColour;

void main() {
    FragColor = outlineColour;
}