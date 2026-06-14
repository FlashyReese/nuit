package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.*;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitRenderPipelines;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
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
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice dynamicTransforms, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        RenderSystem.setShaderFog(fogParameters);
        RenderPipeline texturedPipeline = NuitRenderPipelines.texturedSkybox(this.getBlend().getBlendFunction());
        RenderPipeline frameBlendedPipeline = null;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (AnimatableTexture animatableTexture : this.animatableTextures) {
            animatableTexture.update(level.getGameTime(), tickDelta);
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
                    UVRange intersectionOnCurrentFrame = Utils.mapUVRanges(animatableTexture.getUvRange(), animatableTexture.getCurrentFrame(), intersect);

                    if (interpolate) {
                        UVRange intersectionOnNextFrame = Utils.mapUVRanges(animatableTexture.getUvRange(), animatableTexture.getNextFrame(), intersect);
                        addFrameBlendedVertices(builder, matrix4f, intersectionOnCurrentTexture, intersectionOnCurrentFrame, intersectionOnNextFrame, animatableTexture.getFrameBlend());
                    } else {
                        addTexturedVertices(builder, matrix4f, intersectionOnCurrentTexture, intersectionOnCurrentFrame);
                    }

                    quads++;
                }

                if (quads > 0) {
                    NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, "Sampler0", animatableTexture.getTexture().getTextureId());
                }
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
        return this.animatableTextures.stream().map(texture -> texture.getTexture().getTextureId()).toList();
    }
}
