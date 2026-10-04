#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform HandFxData {
    vec4 p0; // time, opacity, mode (0 web, 1 plasma), scale
    vec4 p1; // tint rgb
    vec4 p2; // tint2 rgb
    vec4 p3; // screen w, h, glowRadius, flameHeight
    vec4 p4; // trail, rise, customColor, threshold
    vec4 p5; // fire tint rgb
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec4 before = texture(Sampler0, texCoord);
    vec4 after = texture(Sampler1, texCoord);
    float mask = smoothstep(0.004, 0.035, length(after.rgb - before.rgb));
    if (mask < 0.001) {
        fragColor = after;
        return;
    }

    float time = p0.x;
    float scale = max(p0.w, 0.05);
    vec2 uv = (texCoord - 0.5) * vec2(p3.x / p3.y, 1.0) * 6.0 / scale;
    float pattern;
    if (p0.z < 0.5) {
        // Веб-шейдер: бегущая паутина из пересекающихся линий.
        float a = sin(uv.x * 3.0 + time * 1.3) + sin(uv.y * 4.0 - time * 1.1) + sin((uv.x + uv.y) * 2.5 + time * 0.8);
        float lines = 1.0 - smoothstep(0.0, 0.35, abs(sin(a * 1.6)));
        pattern = clamp(0.25 + lines, 0.0, 1.0);
    } else {
        // Плазма: наложение синусоид.
        float v = sin(uv.x * 2.0 + time) + sin(uv.y * 2.3 - time * 1.2) + sin((uv.x + uv.y) * 1.7 + time * 0.7) + sin(length(uv) * 3.0 - time * 1.5);
        pattern = 0.5 + 0.5 * sin(v * 1.5);
    }

    float luma = dot(after.rgb, vec3(0.299, 0.587, 0.114));
    vec3 effect = mix(p1.rgb, p2.rgb, pattern) * (0.55 + 0.75 * luma);
    float opacity = clamp(p0.y, 0.0, 1.0) * mask;
    fragColor = vec4(mix(after.rgb, effect, opacity), after.a);
}
