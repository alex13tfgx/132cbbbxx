#version 150

// Размытие движения по репроекции: для каждого пикселя по глубине восстанавливаем
// его положение в мире, проецируем в прошлый кадр и размываем вдоль разницы.
// Порт post/motion_blur из Pulse (1.21.11) на core-шейдер 1.21.4.

uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

uniform mat4 MvInverse;
uniform mat4 ProjInverse;
uniform mat4 PrevModelView;
uniform mat4 PrevProjection;
// Смещение камеры с прошлого кадра. Считается на CPU в double: абсолютные
// координаты мира во float теряют точность уже на тысячах блоков.
uniform vec3 CameraDelta;
uniform vec2 ViewRes;
uniform float BlendFactor;
uniform float InverseSamples;
uniform float HandDepthThreshold;
uniform int Samples;
uniform int HalfSamples;

in vec2 TexCoord;
out vec4 fragColor;

vec3 reproject(vec3 screenPos) {
    vec3 ndc = screenPos * 2.0 - 1.0;
    vec4 view4 = ProjInverse * vec4(ndc, 1.0);
    vec3 viewPos = view4.xyz / view4.w;

    vec3 worldPos = (MvInverse * vec4(viewPos, 1.0)).xyz + CameraDelta;
    vec4 prev = PrevProjection * (PrevModelView * vec4(worldPos, 1.0));

    return (prev.xyz / prev.w) * 0.5 + 0.5;
}

// Резкий рывок камеры не должен размазывать весь экран в полосу.
vec2 clampLength(vec2 velocity) {
    float lenSq = dot(velocity, velocity);
    return lenSq > 0.16 ? velocity * (0.4 * inversesqrt(lenSq)) : velocity;
}

// Дизеринг шага выборки: без него при малом числе сэмплов видны «лесенки».
float noise(vec2 pos) {
    return fract(52.9829189 * fract(0.06711056 * pos.x + 0.00583715 * pos.y));
}

void main() {
    float depth = texture(DepthSampler, TexCoord).r;

    if (Samples <= 0 || depth < HandDepthThreshold) {
        fragColor = vec4(texture(Sampler0, TexCoord).rgb, 1.0);
        return;
    }

    vec2 velocity = clampLength(TexCoord - reproject(vec3(TexCoord, depth)).xy);
    vec2 baseStep = BlendFactor * velocity * InverseSamples;

    vec3 sum = vec3(0.0);
    vec2 seed = TexCoord * ViewRes;
    for (int i = 0; i < Samples; ++i) {
        float fi = float(i);
        float jitter = noise(seed + vec2(fi, fi * 1.4));
        // Сэмплы разложены по обе стороны от пикселя — размытие центрировано.
        float offset = fi - float(HalfSamples) + jitter;
        vec3 c = texture(Sampler0, TexCoord + offset * baseStep).rgb;
        // Смешиваем в квадратах и возвращаем корнем — яркие пятна не тускнеют.
        sum += c * c;
    }
    fragColor = vec4(sqrt(sum * InverseSamples), 1.0);
}
