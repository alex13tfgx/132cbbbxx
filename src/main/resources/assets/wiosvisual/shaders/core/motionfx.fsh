#version 150

// Размытие движения камеры: позиция пикселя восстанавливается из глубины, проецируется
// матрицей прошлого кадра — разница даёт скорость пикселя на экране, вдоль неё и усредняем.
uniform sampler2D Sampler0; // копия кадра мира
uniform sampler2D Sampler1; // глубина мира

layout(std140) uniform MotionBlurData {
    mat4 InvCurr; // (проекция * поворот камеры) текущего кадра, обратная
    mat4 PrevVP;  // проекция * поворот прошлого кадра * сдвиг камеры за кадр
    vec4 Params;  // сила, число выборок, ширина, высота
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 base = texture(Sampler0, texCoord);
    float depth = texture(Sampler1, texCoord).r;
    vec4 pos = InvCurr * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    pos /= pos.w;
    vec4 prev = PrevVP * pos;
    if (prev.w <= 0.0001) {
        fragColor = base;
        return;
    }
    vec2 prevUv = prev.xy / prev.w * 0.5 + 0.5;
    vec2 vel = (prevUv - texCoord) * Params.x;
    // Ограничение: при резком рывке не размазываем больше 12% экрана.
    float len = length(vel);
    if (len > 0.12) {
        vel *= 0.12 / len;
    }
    // Меньше полупикселя — размывать нечего, экономим выборки.
    if (length(vel * Params.zw) < 0.5) {
        fragColor = base;
        return;
    }
    int n = int(Params.y);
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 100; i++) {
        if (i >= n) {
            break;
        }
        float t = float(i) / float(n - 1) - 0.5;
        acc += texture(Sampler0, texCoord + vel * t).rgb;
    }
    fragColor = vec4(acc / float(n), base.a);
}
