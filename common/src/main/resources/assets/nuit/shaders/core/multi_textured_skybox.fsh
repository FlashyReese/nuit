#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec2 nextTexCoord;
flat in float frameBlend;

out vec4 fragColor;

void main() {
    vec4 currentColor = texture(Sampler0, texCoord0);
    vec4 nextColor = texture(Sampler0, nextTexCoord);
    vec4 color = mix(currentColor, nextColor, clamp(frameBlend, 0.0, 1.0));
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
