package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.IrisCompat;
import me.flashyreese.mods.nuit.components.*;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitShaders;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiTexturedSkybox extends TexturedSkybox {
    public static Codec<MultiTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            AnimatableTexture.CODEC.listOf().optionalFieldOf("animatableTextures", new ArrayList<>()).forGetter(MultiTexturedSkybox::getAnimations)
    ).apply(instance, MultiTexturedSkybox::new));
    protected final List<AnimatableTexture> animatableTextures;

    private final float quadSize = 100F;
    private final UVRange quad = new UVRange(-this.quadSize, -this.quadSize, this.quadSize, this.quadSize);

    public MultiTexturedSkybox(Properties properties, Conditions conditions, List<AnimatableTexture> animatableTextures) {
        super(properties, conditions);
        this.animatableTextures = animatableTextures;
    }

    public MultiTexturedSkybox(Properties properties, Conditions conditions, Blend blend, List<AnimatableTexture> animatableTextures) {
        this(properties.withBlend(blend), conditions, animatableTextures);
    }

    @Override
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, Matrix4f projectionMatrix,
                             float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        long gameTime = level.getGameTime();
        for (AnimatableTexture animatableTexture : this.animatableTextures) {
            animatableTexture.update(gameTime, tickDelta);
        }

        boolean shaderPackStateQueried = false;
        boolean shaderPackInUse = false;
        for (AnimatableTexture animatableTexture : this.animatableTextures) {
            UVRange currentFrame = animatableTexture.getCurrentFrame();
            if (currentFrame == null) {
                continue;
            }

            boolean hasInterpolatedFrame = animatableTexture.hasInterpolatedFrame();
            if (hasInterpolatedFrame && !shaderPackStateQueried) {
                shaderPackInUse = IrisCompat.isShaderPackInUse();
                shaderPackStateQueried = true;
            }
            if (hasInterpolatedFrame && shaderPackInUse) {
                this.renderIrisCompatibleInterpolatedTexture(poseStack, animatableTexture);
                continue;
            }

            boolean useFrameBlendShader = hasInterpolatedFrame && NuitShaders.getFrameBlendedSkybox() != null;
            this.renderTextureFrame(
                    poseStack,
                    animatableTexture,
                    currentFrame,
                    useFrameBlendShader ? animatableTexture.getNextFrame() : null,
                    useFrameBlendShader ? animatableTexture.getFrameBlend() : 0.0F,
                    useFrameBlendShader
            );
        }
    }

    private void renderIrisCompatibleInterpolatedTexture(
            PoseStack poseStack,
            AnimatableTexture animatableTexture
    ) {
        // Iris replaces Nuit's shader with the shader-pack sky program, so use weighted two-pass blending there.
        float frameBlend = Mth.clamp(animatableTexture.getFrameBlend(), 0.0F, 1.0F);
        try {
            this.renderWeightedTextureFrame(
                    poseStack,
                    animatableTexture,
                    animatableTexture.getCurrentFrame(),
                    1.0F - frameBlend
            );
            this.renderWeightedTextureFrame(
                    poseStack,
                    animatableTexture,
                    animatableTexture.getNextFrame(),
                    frameBlend
            );
        } finally {
            this.properties.blend().apply(this.alpha);
        }
    }

    private void renderWeightedTextureFrame(
            PoseStack poseStack,
            AnimatableTexture animatableTexture,
            UVRange frame,
            float alphaWeight
    ) {
        if (alphaWeight <= 0.0F) {
            return;
        }
        this.properties.blend().apply(this.alpha * alphaWeight);
        this.renderTextureFrame(poseStack, animatableTexture, frame, null, 0.0F, false);
    }

    private void renderTextureFrame(
            PoseStack poseStack,
            AnimatableTexture animatableTexture,
            UVRange animationCurrentFrame,
            UVRange animationNextFrame,
            float frameBlend,
            boolean useFrameBlendShader
    ) {
        VertexFormat vertexFormat = useFrameBlendShader
                ? NuitShaders.FRAME_BLENDED_SKYBOX_FORMAT
                : DefaultVertexFormat.POSITION_TEX;
        UVRange textureUvRange = animatableTexture.getUvRange();
        BufferBuilder bufferBuilder = null;

        for (int face = 0; face < 6; ++face) {
            UVRange faceUVRange = Utils.TEXTURE_FACES[face];
            UVRange intersect = Utils.findUVIntersection(faceUVRange, textureUvRange);
            if (intersect == null) {
                continue;
            }

            if (bufferBuilder == null) {
                bufferBuilder = Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS,
                        vertexFormat
                );
            }

            poseStack.pushPose();
            try {
                Utils.rotateSkyBoxByFace(poseStack, face);
                Matrix4f matrix4f = poseStack.last().pose();
                UVRange position = Utils.mapUVRanges(faceUVRange, this.quad, intersect);
                UVRange currentFrame = Utils.mapUVRanges(
                        textureUvRange,
                        animationCurrentFrame,
                        intersect
                );
                if (useFrameBlendShader) {
                    UVRange nextFrame = Utils.mapUVRanges(
                            textureUvRange,
                            animationNextFrame,
                            intersect
                    );
                    addFrameBlendedVertices(
                            bufferBuilder,
                            matrix4f,
                            position,
                            currentFrame,
                            nextFrame,
                            frameBlend
                    );
                } else {
                    addTexturedVertices(bufferBuilder, matrix4f, position, currentFrame);
                }
            } finally {
                poseStack.popPose();
            }
        }

        if (bufferBuilder != null) {
            NuitRenderBackend.drawTextured(
                    bufferBuilder.buildOrThrow(),
                    useFrameBlendShader
                            ? NuitShaders::getFrameBlendedSkybox
                            : GameRenderer::getPositionTexShader,
                    animatableTexture.getTexture().getTextureId()
            );
        }
    }

    private void addTexturedVertices(
            BufferBuilder builder,
            Matrix4f matrix4f,
            UVRange position,
            UVRange currentFrame
    ) {
        builder.addVertex(matrix4f, position.minU(), -this.quadSize, position.minV()).setUv(currentFrame.minU(), currentFrame.minV());
        builder.addVertex(matrix4f, position.minU(), -this.quadSize, position.maxV()).setUv(currentFrame.minU(), currentFrame.maxV());
        builder.addVertex(matrix4f, position.maxU(), -this.quadSize, position.maxV()).setUv(currentFrame.maxU(), currentFrame.maxV());
        builder.addVertex(matrix4f, position.maxU(), -this.quadSize, position.minV()).setUv(currentFrame.maxU(), currentFrame.minV());
    }

    private void addFrameBlendedVertices(
            BufferBuilder builder,
            Matrix4f matrix4f,
            UVRange position,
            UVRange currentFrame,
            UVRange nextFrame,
            float frameBlend
    ) {
        addFrameBlendedVertex(builder, matrix4f, position.minU(), position.minV(), currentFrame.minU(), currentFrame.minV(), nextFrame.minU(), nextFrame.minV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, position.minU(), position.maxV(), currentFrame.minU(), currentFrame.maxV(), nextFrame.minU(), nextFrame.maxV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, position.maxU(), position.maxV(), currentFrame.maxU(), currentFrame.maxV(), nextFrame.maxU(), nextFrame.maxV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, position.maxU(), position.minV(), currentFrame.maxU(), currentFrame.minV(), nextFrame.maxU(), nextFrame.minV(), frameBlend);
    }

    private static void addFrameBlendedVertex(
            BufferBuilder builder,
            Matrix4f matrix4f,
            float x,
            float z,
            float currentU,
            float currentV,
            float nextU,
            float nextV,
            float frameBlend
    ) {
        int blend = Math.round(Mth.clamp(frameBlend, 0.0F, 1.0F) * 255.0F);
        builder.addVertex(matrix4f, x, -100.0F, z)
                .setUv(currentU, currentV)
                .setUv1(packUv(nextU), packUv(nextV))
                .setColor(blend, 255, 255, 255);
    }

    private static int packUv(float uv) {
        return Math.round(Mth.clamp(uv, 0.0F, 1.0F) * Short.MAX_VALUE);
    }

    public List<AnimatableTexture> getAnimations() {
        return this.animatableTextures;
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return this.animatableTextures.stream().map(texture -> texture.getTexture().getTextureId()).toList();
    }
}
