#version 150

uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;
uniform sampler2D HistorySampler;
uniform float BlurRadius;
uniform float Threshold;
uniform vec4  TintColor;
uniform vec4  TintColor2;
uniform int   Mode;
uniform float Time;
uniform float ShaderScale;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i+vec2(1,0)), f.x),
               mix(hash(i+vec2(0,1)), hash(i+vec2(1,1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += noise(p)*a; p = p*2.02+vec2(8.4,5.7); a *= 0.5; }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0, a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p)*2.0-1.0);
        v += r*a; p = p*2.18+vec2(3.1,9.2); a *= 0.52;
    }
    return v;
}

vec4 plasmaColor(vec2 uv, vec3 tint, float opacity) {
    vec2 p = uv * 6.0;
    float t = Time * 1.5;

    float v = 0.0;
    v += sin(p.x + t);
    v += sin((p.y + t) * 0.5);
    v += sin((p.x + p.y + t) * 0.5);
    vec2 c = p + vec2(sin(t * 0.33), cos(t * 0.5)) * 3.0;
    v += sin(sqrt(c.x * c.x + c.y * c.y + 1.0) + t);
    v *= 0.25;

    float intensity = 0.5 + 0.5 * sin(v * 3.14159265);
    float swirl = 0.5 + 0.5 * cos(v * 6.2831853 + t);

    vec3 col = mix(tint * 0.45, mix(tint, vec3(1.0), 0.55), intensity);
    col += swirl * tint * 0.25;

    float fill = clamp(0.35 + intensity * 0.8, 0.0, 1.0);
    float alpha = clamp(opacity * fill, 0.0, 1.0);
    return vec4(col, alpha);
}

float handDepth(vec2 uv) {
    return texture(DepthSampler, uv).r;
}

bool isHandPixel(vec2 uv) {
    float depth = handDepth(uv);
    if (depth < 0.001) {
        return false;
    }
    return depth < 0.9999;
}

float nearbyHand(vec2 uv, float radiusPx) {
    vec2 texel = radiusPx / textureSize(DepthSampler, 0);
    float v = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 dir = vec2(cos(a), sin(a));
        if (isHandPixel(uv + dir * texel)) {
            v += 1.0;
        }
    }
    return v / 8.0;
}

/*
 * EmberHands (Mode 4): пламя над предметом в руке и мягкий ореол вокруг.
 * BlurRadius — радиус ореола, ShaderScale — высота пламени, Threshold — длина следа,
 * Time уже умножено на скорость. TintColor.a > 0.5 — свой цвет, иначе цвет предмета.
 */
/** Цвет огня: свой цвет, либо насыщенный цвет предмета; бесцветные предметы горят оранжевым. */
vec3 fireBase(vec3 sampleColor, float hasSample) {
    vec3 orange = vec3(1.0, 0.42, 0.08);
    if (TintColor.a > 0.5) {
        return TintColor.rgb;
    }
    if (hasSample < 0.5) {
        return orange;
    }
    float hi = max(max(sampleColor.r, sampleColor.g), sampleColor.b);
    float lo = min(min(sampleColor.r, sampleColor.g), sampleColor.b);
    float sat = hi <= 0.001 ? 0.0 : (hi - lo) / hi;
    vec3 tinted = sampleColor / max(hi, 0.001);
    // Серые, белые и чёрные предметы не дают цвета — плавно уходим в оранжевый.
    return mix(orange, tinted, smoothstep(0.18, 0.45, sat));
}

vec4 emberColor(vec2 uv) {
    vec2 size = vec2(textureSize(DepthSampler, 0));
    vec2 texel = 1.0 / size;
    // Настройки заданы в пикселях экрана 1080p — приводим к текущему разрешению.
    float px = size.y / 1080.0;
    float height = ShaderScale * px;
    float trail = clamp(Threshold / 28.0, 0.05, 1.0);

    float flame = 0.0;
    vec3 itemColor = vec3(0.0);
    float itemWeight = 0.0;
    const int STEPS = 12;
    for (int k = 1; k <= STEPS; k++) {
        float f = float(k) / float(STEPS);
        float lift = f * height;
        // Пламя извивается сильнее по мере подъёма и течёт вверх со временем.
        float wobble = (noise(vec2(uv.x * size.x * 0.035, uv.y * size.y * 0.02 - Time * 2.6)) - 0.5) * lift * 0.9;
        vec2 probe = uv - vec2(wobble * texel.x, lift * texel.y);
        if (isHandPixel(probe)) {
            float body = 1.0 - f;
            float lick = fbm(vec2(uv.x * size.x * 0.018, uv.y * size.y * (0.012 / (0.4 + trail)) - Time * 1.9));
            float strength = body * smoothstep(0.25 - trail * 0.2, 0.85, lick + body * 0.45);
            if (strength > flame) {
                flame = strength;
            }
            itemColor += texture(Sampler0, probe).rgb;
            itemWeight += 1.0;
            break;
        }
    }

    float halo = 0.0;
    for (int ring = 1; ring <= 3; ring++) {
        float r = BlurRadius * px * float(ring) / 3.0;
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.7854 + float(ring) * 0.39;
            vec2 probe = uv + vec2(cos(a), sin(a)) * r * texel;
            if (isHandPixel(probe)) {
                halo += (1.0 - float(ring - 1) / 3.0) / 8.0;
                if (itemWeight < 6.0) {
                    itemColor += texture(Sampler0, probe).rgb;
                    itemWeight += 1.0;
                }
            }
        }
    }
    halo = clamp(halo / 1.8, 0.0, 1.0);

    if (flame <= 0.001 && halo <= 0.001) {
        return vec4(0.0);
    }
    vec3 base = fireBase(itemWeight > 0.0 ? itemColor / itemWeight : vec3(0.0), itemWeight > 0.0 ? 1.0 : 0.0);

    float heat = clamp(flame * 1.2, 0.0, 1.0);
    // Светлеет только самый язык пламени, и то слегка: иначе огонь уходит в белое.
    vec3 fire = mix(base * 0.7, mix(base, vec3(1.0, 0.85, 0.55), 0.22), heat * heat);
    float flicker = 0.85 + 0.15 * noise(vec2(Time * 3.0, 1.7));
    float alpha = clamp((flame * 1.05 + halo * 0.55) * flicker, 0.0, 1.0);
    vec3 color = (fire * flame + base * 0.85 * halo) / max(flame + halo, 0.001);
    return vec4(color, alpha);
}

void main() {
    vec2 uv = TexCoord;
    vec3 src = texture(Sampler0, uv).rgb;
    bool hand = isHandPixel(uv);

    
    if (Mode == 5) {
        // Накопление шлейфа: прошлый слой огня всплывает вверх, извивается и гаснет,
        // сверху ложится огонь текущего кадра. TintColor2.x — затухание, TintColor2.y — подъём в пикселях.
        vec2 size = vec2(textureSize(DepthSampler, 0));
        vec2 texel = 1.0 / size;
        float rise = TintColor2.y * size.y / 1080.0;
        float sway = (noise(vec2(uv.y * size.y * 0.03 + Time * 2.0, Time * 0.7)) - 0.5) * rise * 1.4;
        // Прошлый след слегка размываем: при быстром взмахе отдельные копии меча
        // сливаются в сплошную гладкую полосу.
        vec2 hUv = uv - vec2(sway * texel.x, rise * texel.y);
        float spread = 1.6 * size.y / 1080.0;
        vec4 history = texture(HistorySampler, hUv) * 0.36;
        history += texture(HistorySampler, hUv + vec2( spread, 0.0) * texel) * 0.16;
        history += texture(HistorySampler, hUv + vec2(-spread, 0.0) * texel) * 0.16;
        history += texture(HistorySampler, hUv + vec2(0.0,  spread) * texel) * 0.16;
        history += texture(HistorySampler, hUv + vec2(0.0, -spread) * texel) * 0.16;
        history *= TintColor2.x;
        vec4 now;
        if (hand) {
            // Сам предмет оставляет след своей формы, окрашенный в цвет огня.
            vec3 base = fireBase(src, 1.0);
            now = vec4(base, 0.55);
        } else {
            now = emberColor(uv);
        }
        vec4 nowPre = vec4(now.rgb * now.a, now.a);
        // Новый слой ложится поверх старого по прозрачности: переходы мягкие, а цвета
        // не складываются в белое (премультиплицированное «over»).
        OutColor = nowPre + history * (1.0 - nowPre.a);
        return;
    }

    if (Mode == 6) {
        // Наложение накопленного огня на кадр; рука остаётся поверх огня.
        if (hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        vec4 trail = texture(HistorySampler, uv);
        float a = clamp(trail.a, 0.0, 1.0);
        vec3 col = trail.rgb / max(trail.a, 0.001);
        OutColor = vec4(mix(src, col, a * 0.85) + col * a * 0.18, 1.0);
        return;
    }

    if (Mode == 4) {
        if (hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        vec4 ember = emberColor(uv);
        OutColor = vec4(mix(src, ember.rgb, ember.a * 0.9) + ember.rgb * ember.a * 0.2, 1.0);
        return;
    }

    if (!hand && Mode != 1) {
        OutColor = vec4(src, 1.0);
        return;
    }

    if (Mode == 0) {
        if (!hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        vec3 effect = mix(src, TintColor.rgb, TintColor.a * 0.65);
        OutColor = vec4(effect, 1.0);

    } else if (Mode == 1) {
        float near = nearbyHand(uv, BlurRadius * 0.35);
        if (hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        float glow = smoothstep(Threshold, Threshold + 0.4, near);
        if (glow < Threshold) {
            OutColor = vec4(src, 1.0);
            return;
        }
        float blend = clamp(pow(glow, 0.6) * TintColor.a * 1.2, 0.0, 1.0);
        OutColor = vec4(mix(src, TintColor.rgb, blend), 1.0);

    } else if (Mode == 2) {
        if (!hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        float t = Time;
        vec2 flow = uv * (3.0 * ShaderScale), drift = vec2(t * 0.20, -t * 0.15);
        vec2 warp = vec2(fbm(flow * 0.90 + drift * 0.75 + vec2(0, 4.1)), fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8)));
        vec2 q = flow + (warp - 0.5) * 1.8;
        float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
        float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
        float sA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + t * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
        float sB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - t * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);
        float energy = clamp(mist * 0.22 + veins * 0.88 + sA * 0.55 + sB * 0.32, 0.0, 1.0);
        float core = smoothstep(0.18, 0.98, energy);
        float accent = pow(clamp(max(veins, sA), 0.0, 1.0), 1.25);
        vec3 effect = mix(TintColor.rgb, mix(TintColor.rgb, vec3(1.0), 0.4), clamp(core * 0.75 + sB * 0.25, 0.0, 1.0));
        effect *= 0.35 + core * 0.75 + accent * 0.22;
        float blend = clamp(TintColor.a * (0.35 + core * 0.75 + accent * 0.22) * 0.92, 0.0, 1.0);
        OutColor = vec4(mix(src, effect, blend), 1.0);

    } else if (Mode == 3) {
        if (!hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        vec4 plasma = plasmaColor(uv * (4.0 * ShaderScale), TintColor.rgb, TintColor.a);
        float blend = clamp(plasma.a, 0.0, 1.0);
        OutColor = vec4(mix(src, plasma.rgb, blend), 1.0);

    } else {
        OutColor = vec4(src, 1.0);
    }
}
