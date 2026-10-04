#version 150

uniform sampler2D Sampler0;

layout(std140) uniform SaturationData {
    vec4 params;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 source = texture(Sampler0, texCoord);
    float luma = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    float spread = max(source.r, max(source.g, source.b)) - min(source.r, min(source.g, source.b));
    // Vibrance подтягивает только блёклые цвета: чем насыщеннее пиксель, тем слабее добавка.
    float amount = params.x + params.y * (1.0 - spread);
    vec3 color = mix(vec3(luma), source.rgb, amount);
    fragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
