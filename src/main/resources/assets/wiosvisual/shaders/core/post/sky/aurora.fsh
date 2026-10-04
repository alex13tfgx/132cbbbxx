#version 150

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
uniform vec2 uCameraDir;
uniform float uFov;

out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}
mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
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

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;

    float t = uTime * uSpeed * 0.25;
    vec2 q = vec2(atan(rayW.z, rayW.x), rayW.y);
    q *= uScale * 0.35;

    float h = rayW.y;
    float bands = 0.0;

    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float n = fbm(vec2(q.x * 1.5 + t + fi * 3.1, q.y * 0.5 + t * 0.5));
        float center = 0.12 + fi * 0.18 + n * 0.28;
        float band = exp(-pow((h - center) / 0.06, 2.0));
        bands += band;
    }

    bands *= smoothstep(-0.05, 0.4, rayW.y);

    float shimmer = 0.6 + 0.4 * sin(t * 2.0 + q.x * 4.0);
    float glow = bands * shimmer;

    vec3 color = uColor * glow * (1.0 + uIntensity * 20.0) + uColor * 0.05;

    fragColor = vec4(color, uAlpha * clamp(bands, 0.0, 1.0));
}
