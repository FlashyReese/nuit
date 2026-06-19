package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.IrisCompat;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.components.AnimatableTexture;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Texture;
import me.flashyreese.mods.nuit.components.UVRange;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitRenderPipelines;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;

public class MultiTexturedSkybox extends TexturedSkybox {
    public static Codec<MultiTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(TexturedSkybox::getBlend),
            AnimatableTexture.CODEC.listOf().optionalFieldOf("animatableTextures", new ArrayList<>()).forGetter(MultiTexturedSkybox::getAnimations)
    ).apply(instance, MultiTexturedSkybox::new));
    private static final int PACKED_UV_MAX = Short.MAX_VALUE;
    protected final List<AnimatableTexture> animatableTextures;
    private final float quadSize = 100F;
    private final UVRange quad = new UVRange(-this.quadSize, -this.quadSize, this.quadSize, this.quadSize);

    public MultiTexturedSkybox(Properties properties, Conditions conditions, Blend blend, List<AnimatableTexture> animatableTextures) {
        super(properties, conditions, blend);
        this.animatableTextures = animatableTextures;
    }

    @Override
    public void renderSkybox(SkyboxRenderContext context, Matrix4fStack modelViewStack, GpuBufferSlice dynamicTransforms) {
        context.applyFog();
        RenderPipeline texturedPipeline = NuitRenderPipelines.texturedSkybox(this.getBlend().getBlendFunction());
        RenderPipeline frameBlendedPipeline = null;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (AnimatableTexture animatableTexture : this.animatableTextures) {
            animatableTexture.update(level.getGameTime(), context.tickDelta());
        }
        for (AnimatableTexture animatableTexture : this.animatableTextures) {
            if (animatableTexture.getCurrentFrame() == null) {
                continue;
            }

            boolean interpolate = shouldInterpolate(animatableTexture);
            if (interpolate && frameBlendedPipeline == null) {
                frameBlendedPipeline = NuitRenderPipelines.frameBlendedTexturedSkybox(this.getBlend().getBlendFunction());
            }

            RenderPipeline pipeline = interpolate ? frameBlendedPipeline : texturedPipeline;
            if (interpolate && IrisCompat.isShaderPackInUse()) {
                this.renderIrisCompatibleInterpolatedTexture(texturedPipeline, modelViewStack, animatableTexture);
                continue;
            }

            this.renderTextureFrame(pipeline, dynamicTransforms, animatableTexture, animatableTexture.getCurrentFrame(), interpolate ? animatableTexture.getNextFrame() : null, animatableTexture.getFrameBlend());
        }
    }

    private void renderIrisCompatibleInterpolatedTexture(RenderPipeline pipeline, Matrix4fStack modelViewStack, AnimatableTexture animatableTexture) {
        // Iris replaces Nuit's shader with the shader-pack sky program, so use weighted two-pass blending there.
        float frameBlend = Mth.clamp(animatableTexture.getFrameBlend(), 0.0F, 1.0F);
        this.renderWeightedTextureFrame(pipeline, modelViewStack, animatableTexture, animatableTexture.getCurrentFrame(), 1.0F - frameBlend);
        this.renderWeightedTextureFrame(pipeline, modelViewStack, animatableTexture, animatableTexture.getNextFrame(), frameBlend);
    }

    private void renderWeightedTextureFrame(RenderPipeline pipeline, Matrix4fStack modelViewStack, AnimatableTexture animatableTexture, UVRange frame, float alphaWeight) {
        if (alphaWeight <= 0.0F) {
            return;
        }

        GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(modelViewStack), this.getBlend().getColorModifier(this.alpha * alphaWeight));
        this.renderTextureFrame(pipeline, dynamicTransforms, animatableTexture, frame, null, 0.0F);
    }

    private void renderTextureFrame(RenderPipeline pipeline, GpuBufferSlice dynamicTransforms, AnimatableTexture animatableTexture, UVRange currentFrame, UVRange nextFrame, float frameBlend) {
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 24)) {
            BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            int quads = 0;

            for (int face = 0; face < 6; ++face) {
                Matrix4f matrix4f = Utils.getMatrixForRotatedFace(face);
                UVRange faceUVRange = Utils.TEXTURE_FACES[face];
                UVRange intersect = Utils.findUVIntersection(faceUVRange, animatableTexture.getUvRange()); // todo: cache this intersections so we don't waste gpu cycles
                if (intersect == null) {
                    continue;
                }

                UVRange intersectionOnCurrentTexture = Utils.mapUVRanges(faceUVRange, this.quad, intersect);
                UVRange intersectionOnCurrentFrame = Utils.mapUVRanges(animatableTexture.getUvRange(), currentFrame, intersect);

                if (nextFrame != null) {
                    UVRange intersectionOnNextFrame = Utils.mapUVRanges(animatableTexture.getUvRange(), nextFrame, intersect);
                    this.addFrameBlendedVertices(builder, matrix4f, intersectionOnCurrentTexture, intersectionOnCurrentFrame, intersectionOnNextFrame, frameBlend);
                } else {
                    this.addTexturedVertices(builder, matrix4f, intersectionOnCurrentTexture, intersectionOnCurrentFrame);
                }

                quads++;
            }

            if (quads > 0) {
                NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, "Sampler0", animatableTexture.getTexture().getTextureId());
            }
        }
    }

    private static boolean shouldInterpolate(AnimatableTexture animatableTexture) {
        return animatableTexture.isInterpolate()
                && animatableTexture.hasMultipleFrames()
                && animatableTexture.getNextFrame() != null
                && animatableTexture.getFrameBlend() > 0.0F;
    }

    private void addTexturedVertices(BufferBuilder builder, Matrix4f matrix4f, UVRange textureQuad, UVRange currentFrame) {
        builder.addVertex(matrix4f, textureQuad.minU(), -this.quadSize, textureQuad.minV()).setUv(currentFrame.minU(), currentFrame.minV());
        builder.addVertex(matrix4f, textureQuad.minU(), -this.quadSize, textureQuad.maxV()).setUv(currentFrame.minU(), currentFrame.maxV());
        builder.addVertex(matrix4f, textureQuad.maxU(), -this.quadSize, textureQuad.maxV()).setUv(currentFrame.maxU(), currentFrame.maxV());
        builder.addVertex(matrix4f, textureQuad.maxU(), -this.quadSize, textureQuad.minV()).setUv(currentFrame.maxU(), currentFrame.minV());
    }

    private void addFrameBlendedVertices(BufferBuilder builder, Matrix4f matrix4f, UVRange textureQuad, UVRange currentFrame, UVRange nextFrame, float frameBlend) {
        addFrameBlendedVertex(builder, matrix4f, textureQuad.minU(), textureQuad.minV(), currentFrame.minU(), currentFrame.minV(), nextFrame.minU(), nextFrame.minV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, textureQuad.minU(), textureQuad.maxV(), currentFrame.minU(), currentFrame.maxV(), nextFrame.minU(), nextFrame.maxV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, textureQuad.maxU(), textureQuad.maxV(), currentFrame.maxU(), currentFrame.maxV(), nextFrame.maxU(), nextFrame.maxV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, textureQuad.maxU(), textureQuad.minV(), currentFrame.maxU(), currentFrame.minV(), nextFrame.maxU(), nextFrame.minV(), frameBlend);
    }

    private void addFrameBlendedVertex(BufferBuilder builder, Matrix4f matrix4f, float x, float z, float currentU, float currentV, float nextU, float nextV, float frameBlend) {
        builder.addVertex(matrix4f, x, -this.quadSize, z)
                .setUv(currentU, currentV)
                .setUv1(packUv(nextU), packUv(nextV))
                .setLineWidth(frameBlend);
    }

    private static int packUv(float uv) {
        return Math.round(Mth.clamp(uv, 0.0F, 1.0F) * PACKED_UV_MAX);
    }

    public List<AnimatableTexture> getAnimations() {
        return this.animatableTextures;
    }

    @Override
    public List<Identifier> getTexturesToRegister() {
        return this.animatableTextures.stream().map(AnimatableTexture::getTexture).map(Texture::getTextureId).toList();
    }
}
