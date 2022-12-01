#version 330

const int MAX_DRAW_ELEMENTS = 100;
const int MAX_ENTITIES = 50;

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;

out vec2 outTextCoord;
out vec4 outViewPosition;
out vec4 outWorldPosition;

struct DrawElement {
    int modelMatrixIdx;
    int materialIdx;
};

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform DrawElement drawElements[MAX_DRAW_ELEMENTS];
uniform mat4 modelMatrices[MAX_ENTITIES];

void main() {
    vec4 initPos = vec4(position, 1.0);
    uint idx = gl_BaseInstance + gl_InstanceID;
    DrawElement drawElement = drawElements[idx];
    mat4 modelMatrix =  modelMatrices[drawElement.modelMatrixIdx];
    outWorldPosition = modelMatrix * initPos;
    outViewPosition  = viewMatrix * outWorldPosition;
    gl_Position   = projectionMatrix * outViewPosition;
    outTextCoord  = texCoord;
}