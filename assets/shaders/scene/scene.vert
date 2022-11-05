#version 330

layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec3 colour;

out vec3 outColour;

uniform mat4 projectionMatrix;

void main() {
    gl_Position = projectionMatrix * vec4(inPosition, 0.0);
    outColour = colour;
}