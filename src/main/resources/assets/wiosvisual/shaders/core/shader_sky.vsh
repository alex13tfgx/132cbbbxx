#version 150

layout(std140) uniform SkyData {
    mat4 InvViewProj;
    vec4 P1;
    vec4 P2;
    vec4 C1;
    vec4 C2;
};

out vec2 ndc;

void main() {
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    ndc = p * 2.0 - 1.0;
    // z = w: depth is exactly 1.0, so this only draws where the world left the sky visible.
    gl_Position = vec4(ndc * 0.99999, 0.99999, 1.0);
}
