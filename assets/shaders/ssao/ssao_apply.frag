#version 330

out vec4 FragColor;

in vec2 outTextCoord;

uniform sampler2D scene;
uniform sampler2D ssao;

void main() {
    float ambientOcclusion = texture(ssao, outTextCoord).r;
    FragColor = texture(scene, outTextCoord);
    FragColor.rgb = FragColor.rgb * ambientOcclusion;
}