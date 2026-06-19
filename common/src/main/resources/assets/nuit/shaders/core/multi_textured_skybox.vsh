#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in float FrameBlend;

out vec2 texCoord0;
out vec2 nextTexCoord;
flat out float frameBlend;

const float PACKED_UV_MAX = 32767.0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    nextTexCoord = vec2(UV1) / PACKED_UV_MAX;
    frameBlend = FrameBlend;
}
