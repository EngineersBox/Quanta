#version 330

layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec3 colour;

out vec3 outColour;

void main() {
    gl_Position = vec4(inPosition, 0.0);
    outColour = colour;
}