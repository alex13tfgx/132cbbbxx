#version 330

#moj_import <mre:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

layout(std140) uniform BorderData {
    vec2 Size;
    vec4 Radius;
    float Thickness;
    vec2 Smoothness;
};

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist = rdist(center - (FragCoord.xy * Size), center - 1.0, Radius);
    float alpha = smoothstep(1.0 - Thickness - Smoothness.x - Smoothness.y, 1.0 - Thickness - Smoothness.y, dist);
    alpha *= 1.0 - smoothstep(1.0 - Smoothness.y, 1.0, dist);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);
    if (color.a == 0.0) discard;
    OutColor = color;
}
