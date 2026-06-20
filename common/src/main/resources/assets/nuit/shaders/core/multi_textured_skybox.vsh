#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec2 NextUV;
in float FrameBlend;

out vec2 texCoord0;
out vec2 nextTexCoord;
flat out float frameBlend;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    nextTexCoord = NextUV;
    frameBlend = FrameBlend;
}
