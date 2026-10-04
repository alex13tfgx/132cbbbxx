#version 150

layout(std140) uniform SkyData {
    mat4 InvViewProj;
    vec4 P1; // time, intensity, speed, scale
    vec4 P2; // stars, mode
    vec4 C1;
    vec4 C2;
};

in vec2 ndc;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(17.0, 9.0);
        a *= 0.5;
    }
    return v;
}

float stars(vec3 d, float density, float t) {
    vec3 p = d * 60.0;
    vec3 i = floor(p);
    vec3 f = fract(p) - 0.5;
    float h = hash(i.xy + i.z * 17.3);
    float s = step(1.0 - density * 0.06, h);
    return s * smoothstep(0.18, 0.0, length(f)) * (0.6 + 0.4 * sin(t * 3.0 + h * 50.0));
}

void main() {
    vec4 w = InvViewProj * vec4(ndc, 1.0, 1.0);
    vec3 d = normalize(w.xyz / w.w);
    float y = d.y;
    if (y < -0.02) {
        discard;
    }
    float up = clamp(y, 0.0, 1.0);
    float time = P1.x * P1.z;
    float sc = P1.w;
    vec3 c1 = C1.rgb;
    vec3 c2 = C2.rgb;
    int mode = int(P2.y + 0.5);
    vec3 col;

    if (mode == 0) {
        col = mix(vec3(0.01, 0.02, 0.05), vec3(0.0, 0.0, 0.02), up);
        vec2 uv = d.xz / (abs(y) + 0.35) * sc;
        for (int i = 0; i < 2; i++) {
            float fi = float(i);
            float n = fbm(vec2(uv.x * 0.8 + time * 0.15 + fi * 7.0, uv.y * 0.4 + fi * 3.0));
            float band = smoothstep(0.35, 0.75, n) * smoothstep(0.0, 0.3, y) * (1.0 - smoothstep(0.5, 1.0, y));
            col += mix(c1, c2, fi) * band * 0.7 / (1.0 + fi * 0.4);
        }
        col += stars(d, P2.x, time);
    } else if (mode == 1) {
        col = mix(vec3(0.02, 0.02, 0.06), vec3(0.0, 0.0, 0.015), up) + c1 * 0.12 * exp(-abs(y) * 6.0);
        col += stars(d, P2.x, time) * 1.2;
        vec3 moon = normalize(vec3(0.4, 0.5, -0.6));
        float m = dot(d, moon);
        col += vec3(0.9, 0.95, 1.0) * smoothstep(0.9975, 0.999, m) + c1 * 0.25 * pow(max(m, 0.0), 48.0);
    } else if (mode == 2) {
        col = mix(vec3(0.55, 0.62, 0.72), vec3(0.25, 0.3, 0.4), up);
        col = mix(col, c1 * 0.7, 0.25);
        vec2 uv = d.xz / (y + 0.3) * sc;
        float cl = fbm(uv * 0.5 + time * 0.05);
        col = mix(col, vec3(0.9, 0.93, 0.97), smoothstep(0.4, 0.8, cl) * 0.6 * smoothstep(0.0, 0.2, y));
        for (int i = 0; i < 2; i++) {
            float fi = float(i);
            vec2 g = ndc * vec2(1.7, 1.0) * (6.0 + fi * 5.0);
            g.y += time * (0.6 + fi * 0.3);
            g.x += sin(time * 0.3 + fi) * 0.5;
            vec2 id = floor(g);
            vec2 f = fract(g) - 0.5;
            float h = hash(id + fi * 13.0);
            vec2 off = (vec2(hash(id + 1.0), hash(id + 2.0)) - 0.5) * 0.5;
            col += step(0.92, h) * smoothstep(0.12 + 0.04 * fi, 0.0, length(f - off)) * 0.8;
        }
    } else if (mode == 3) {
        col = mix(vec3(0.62, 0.78, 0.95), vec3(0.15, 0.38, 0.8), pow(up, 0.6));
        col = mix(col, c1, 0.15);
        vec3 sun = normalize(vec3(0.3, 0.5, 0.4));
        float s = max(dot(d, sun), 0.0);
        col += vec3(1.0, 0.9, 0.7) * (pow(s, 600.0) * 3.0 + pow(s, 16.0) * 0.25);
        vec2 uv = d.xz / (y + 0.25) * sc;
        float cl = fbm(uv * 0.8 + vec2(time * 0.05, 0.0));
        col = mix(col, vec3(1.0), smoothstep(0.5, 0.85, cl) * 0.8 * smoothstep(0.0, 0.15, y));
    } else if (mode == 4) {
        col = mix(c2 * 0.3, vec3(0.0), up) + vec3(0.01, 0.01, 0.03);
        vec2 uv = d.xz / (abs(y) + 0.4) * sc;
        float n = fbm(uv * 1.2 + time * 0.02);
        col += mix(c1, c2, n) * pow(n, 2.5) * 0.5 * smoothstep(-0.1, 0.4, y);
        col += stars(d, min(1.0, P2.x * 1.6), time) * 1.4 + stars(d * 1.7 + 3.0, P2.x, time * 1.3);
    } else {
        float a = atan(d.x, d.z);
        float r = acos(clamp(y, -1.0, 1.0));
        float k = 0.5 + 0.5 * sin(r * 6.0 - time * 1.5 + sin(a * 3.0 + time * 0.5) * 1.2);
        col = mix(c2 * 0.3, c1, k) * (0.35 + 0.65 * smoothstep(-0.2, 0.8, y)) + c1 * 0.15 * exp(-abs(y) * 4.0);
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), clamp(P1.y, 0.0, 1.0));
}
