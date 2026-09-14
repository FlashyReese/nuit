#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec2 nextTexCoord;
layout(location = 2) flat in float frameBlend;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 currentColor = texture(Sampler0, texCoord0);
    vec4 nextColor = texture(Sampler0, nextTexCoord);
    vec4 color = mix(currentColor, nextColor, clamp(frameBlend, 0.0, 1.0));
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
