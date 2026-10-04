#version 150

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform vec3 uColor2;
uniform float uAlpha;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
uniform float uStars;
uniform float uMode;
uniform vec2 uCameraDir;
uniform float uFov;

out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}
mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        v += amp * noise(p);
        p *= 2.0;
        amp *= 0.5;
    }
    return v;
}

// direction of the current fragment in world-ish space
vec3 rayDir() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;
    float tanV = tan(radians(uFov) * 0.5);
    vec3 ray = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    ray = rotX(uCameraDir.y) * ray;
    ray = rotY(uCameraDir.x) * ray;
    return normalize(ray);
}

float starField(vec3 dir, float density, float twinkle) {
    vec2 g = dir.xz / max(0.15, abs(dir.y) + 0.15) * (40.0 + uScale * 8.0);
    vec2 cell = floor(g);
    float h = hash(cell);
    float star = step(1.0 - density * 0.02, h);
    float tw = 0.6 + 0.4 * sin(uTime * (2.0 + twinkle) + h * 40.0);
    vec2 f = fract(g) - 0.5;
    float d = 1.0 - smoothstep(0.0, 0.18, length(f));
    return star * d * tw;
}

void main() {
    vec3 dir = rayDir();
    float t = uTime * uSpeed;
    float height = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 col = mix(uColor2, uColor, height);
    float a = uAlpha;

    if (uMode < 0.5) {
        // Aurora
        vec2 p = vec2(atan(dir.x, dir.z) * (0.5 + uScale * 0.15), dir.y * 2.0);
        float bands = fbm(p * 2.0 + vec2(t * 0.4, t * 0.1));
        bands += 0.5 * fbm(p * 4.0 - vec2(t * 0.2, 0.0));
        float curtain = smoothstep(0.2, 0.9, bands) * smoothstep(1.0, 0.15, abs(dir.y - 0.25));
        vec3 aurora = mix(uColor, uColor2, bands) * curtain * (1.5 + uIntensity * 20.0);
        col = mix(col * 0.4, col * 0.4 + aurora, 1.0);
        col += starField(dir, uStars * 0.6, 1.0) * uStars;
    } else if (uMode < 1.5) {
        // Night
        col = mix(uColor2 * 0.15, uColor * 0.3, height);
        col += starField(dir, uStars, 1.5) * (0.8 + uStars);
        float glow = pow(max(0.0, 1.0 - abs(dir.y)), 3.0);
        col += uColor * glow * 0.15 * (1.0 + uIntensity * 10.0);
    } else if (uMode < 2.5) {
        // Snow
        col = mix(uColor2, uColor, height) * 0.7;
        vec2 sp = dir.xy * (30.0 + uScale * 10.0);
        sp.y += t * 6.0;
        float flakes = 0.0;
        for (int i = 0; i < 3; i++) {
            vec2 q = sp + float(i) * 17.3;
            vec2 cell = floor(q);
            float h = hash(cell);
            vec2 f = fract(q) - 0.5;
            f.x += 0.3 * sin(t + h * 30.0);
            flakes += step(0.985, h) * (1.0 - smoothstep(0.0, 0.25, length(f)));
        }
        col += vec3(flakes) * (0.8 + uIntensity * 10.0);
    } else if (uMode < 3.5) {
        // Sky (smooth gradient)
        col = mix(uColor2, uColor, pow(height, 0.7));
        float sun = pow(max(0.0, dir.y), 8.0);
        col += uColor * sun * 0.3;
        col += fbm(dir.xz * (2.0 + uScale) + t * 0.1) * 0.05 * uColor;
    } else if (uMode < 4.5) {
        // Star
        col = mix(uColor2 * 0.1, uColor * 0.2, height);
        col += starField(dir, uStars * 1.6 + 0.4, 2.0) * (1.0 + uStars);
        col += starField(dir, uStars * 0.4, 0.5) * 0.5;
    } else {
        // Glow
        float pulse = 0.5 + 0.5 * sin(t * 1.5);
        float rad = pow(max(0.0, 1.0 - length(dir.xy)), 2.0);
        col = mix(uColor2, uColor, height) * 0.4;
        col += mix(uColor, uColor2, pulse) * rad * (1.0 + uIntensity * 25.0);
        col += starField(dir, uStars * 0.5, 1.0) * uStars * 0.5;
    }

    col = clamp(col, 0.0, 1.0);
    fragColor = vec4(col, a);
}
