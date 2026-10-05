#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2; // глубина после руки: < 1.0 только там, где рука

layout(std140) uniform HandFxData {
    vec4 p0; // time, opacity, mode (0 web, 1 plasma, 2 flow), scale
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

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = rot * p * 2.0 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

/** Маска руки: глубина руки (без дыр, даже если цвет предмета совпал с фоном) и разница кадров для сглаженного края. */
float handMask(vec2 uv) {
    float depth = texture(Sampler2, uv).r < 0.99999 ? 1.0 : 0.0;
    float diff = smoothstep(0.004, 0.035, length(texture(Sampler1, uv).rgb - texture(Sampler0, uv).rgb));
    return max(depth, diff);
}

/** Режим «Переливание»: текущие разводы внутри, яркая кромка и свечение вокруг. */
vec4 flow(vec4 after, float mask) {
    vec2 texel = vec2(1.0 / p3.x, 1.0 / p3.y) * max(1.0, p3.y / 1080.0);
    float time = p0.x;
    float opacity = clamp(p0.y, 0.0, 1.0);
    vec3 tint = p1.rgb;

    // Соседи на двух радиусах: минимум — для внутренней кромки, взвешенный максимум — для свечения снаружи.
    float minN = 1.0;
    float glow = 0.0;
    for (int i = 0; i < 8; i++) {
        float ang = float(i) * 0.785398;
        vec2 dir = vec2(cos(ang), sin(ang));
        float m1 = handMask(texCoord + dir * texel * 1.5);
        float m2 = handMask(texCoord + dir * texel * 5.0);
        minN = min(minN, m1);
        glow = max(glow, max(m1, m2 * 0.45));
    }

    if (mask < 0.001) {
        // Снаружи предмета: мягкое свечение цвета шейдера.
        return vec4(after.rgb + tint * glow * 0.55 * opacity, after.a);
    }

    // Разводы: fbm с двойным искажением координат — медленно текут и переливаются.
    float scale = max(p0.w, 0.05);
    vec2 uv = (texCoord - 0.5) * vec2(p3.x / p3.y, 1.0) * 3.0 / scale;
    vec2 q = vec2(fbm(uv + vec2(0.0, time * 0.12)), fbm(uv + vec2(5.2, 1.3) - time * 0.10));
    vec2 r = vec2(fbm(uv + 3.5 * q + vec2(1.7, 9.2) + time * 0.15), fbm(uv + 3.5 * q + vec2(8.3, 2.8) - time * 0.13));
    float v = fbm(uv + 3.0 * r);

    vec3 dark = tint * 0.12;
    vec3 light = mix(tint, vec3(1.0), 0.35);
    vec3 col = mix(dark, tint * 0.85, smoothstep(0.25, 0.65, v));
    // Яркие прожилки там, где искажение сильнее всего.
    float streak = smoothstep(0.55, 0.85, length(r) * 0.75 + v * 0.35);
    col = mix(col, light, streak * 0.8);

    // Кромка по контуру предмета.
    float edge = clamp(mask - minN, 0.0, 1.0);
    col = mix(col, mix(tint, vec3(1.0), 0.65), edge);

    float a = clamp(opacity * 1.25, 0.0, 1.0) * mask;
    return vec4(mix(after.rgb, col, max(a, edge * mask)), after.a);
}

void main() {
    vec4 before = texture(Sampler0, texCoord);
    vec4 after = texture(Sampler1, texCoord);
    float mask = handMask(texCoord);

    if (p0.z > 1.5) {
        fragColor = flow(after, mask);
        return;
    }

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
