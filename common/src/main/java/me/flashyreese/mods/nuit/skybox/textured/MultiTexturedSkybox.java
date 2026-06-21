package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class MultiTexturedSkybox extends TexturedSkybox {
    public static Codec<MultiTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(TexturedSkybox::getBlend),
            AnimatableTexture.CODEC.listOf().optionalFieldOf("animatableTextures", new ArrayList<>()).forGetter(MultiTexturedSkybox::getAnimations)
    ).apply(instance, MultiTexturedSkybox::new));
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
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlendFunction blendFunction = this.getBlend().getBlendFunction();
        RenderPipeline texturedPipeline = null;
        RenderPipeline frameBlendedPipeline = null;
        boolean shaderPackStateQueried = false;
        boolean shaderPackInUse = false;
        long gameTime = level.getGameTime();
        float tickDelta = context.tickDelta();

        for (AnimatableTexture animatableTexture : this.animatableTextures) {
            animatableTexture.update(gameTime, tickDelta);
            UVRange currentFrame = animatableTexture.getCurrentFrame();
            if (currentFrame == null) {
                continue;
            }

            if (animatableTexture.hasInterpolatedFrame()) {
                if (!shaderPackStateQueried) {
                    shaderPackInUse = IrisCompat.isShaderPackInUse();
                    shaderPackStateQueried = true;
                }

                UVRange nextFrame = animatableTexture.getNextFrame();
                float frameBlend = animatableTexture.getFrameBlend();
                if (shaderPackInUse) {
                    if (texturedPipeline == null) {
                        texturedPipeline = NuitRenderPipelines.texturedSkybox(blendFunction);
                    }

                    this.renderIrisCompatibleInterpolatedTexture(
                            texturedPipeline,
                            modelViewStack,
                            animatableTexture,
                            currentFrame,
                            nextFrame,
                            frameBlend
                    );
                } else {
                    if (frameBlendedPipeline == null) {
                        frameBlendedPipeline = NuitRenderPipelines.frameBlendedTexturedSkybox(blendFunction);
                    }

                    this.renderTextureFrame(
                            frameBlendedPipeline,
                            dynamicTransforms,
                            animatableTexture,
                            currentFrame,
                            nextFrame,
                            frameBlend
                    );
                }
                continue;
            }

            if (texturedPipeline == null) {
                texturedPipeline = NuitRenderPipelines.texturedSkybox(blendFunction);
            }

            this.renderTextureFrame(
                    texturedPipeline,
                    dynamicTransforms,
                    animatableTexture,
                    currentFrame,
                    null,
                    0.0F
            );
        }
    }

    private void renderIrisCompatibleInterpolatedTexture(RenderPipeline pipeline, Matrix4fStack modelViewStack, AnimatableTexture animatableTexture,
                                                         UVRange currentFrame, UVRange nextFrame, float frameBlend) {
        // Iris replaces Nuit's shader with the shader-pack sky program, so use weighted two-pass blending there.
        this.renderWeightedTextureFrame(pipeline, modelViewStack, animatableTexture, currentFrame, 1.0F - frameBlend);
        this.renderWeightedTextureFrame(pipeline, modelViewStack, animatableTexture, nextFrame, frameBlend);
    }

    private void renderWeightedTextureFrame(RenderPipeline pipeline, Matrix4fStack modelViewStack, AnimatableTexture animatableTexture, UVRange frame, float alphaWeight) {
        if (alphaWeight <= 0.0F) {
            return;
        }

        GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(modelViewStack), this.getBlend().getColorModifier(this.alpha * alphaWeight));
        this.renderTextureFrame(pipeline, dynamicTransforms, animatableTexture, frame, null, 0.0F);
    }

    private void renderTextureFrame(RenderPipeline pipeline, GpuBufferSlice dynamicTransforms, AnimatableTexture animatableTexture, UVRange currentFrame, UVRange nextFrame, float frameBlend) {
        if (nextFrame != null) {
            this.renderFrameBlendedTextureFrame(pipeline, dynamicTransforms, animatableTexture, currentFrame, nextFrame, frameBlend);
            return;
        }

        try (ByteBufferBuilder byteBufferBuilder = NuitRenderPipelines.byteBufferBuilder(pipeline, 24)) {
            BufferBuilder builder = NuitRenderPipelines.bufferBuilder(byteBufferBuilder, pipeline);
            UVRange textureUvRange = animatableTexture.getUvRange();
            int quads = 0;

            for (int face = 0; face < 6; ++face) {
                UVRange faceUVRange = Utils.TEXTURE_FACES[face];
                UVRange intersect = Utils.findUVIntersection(faceUVRange, textureUvRange);
                if (intersect == null) {
                    continue;
                }

                Matrix4f matrix4f = Utils.getMatrixForRotatedFace(face);
                UVRange intersectionOnCurrentTexture = Utils.mapUVRanges(faceUVRange, this.quad, intersect);
                UVRange intersectionOnCurrentFrame = Utils.mapUVRanges(textureUvRange, currentFrame, intersect);
                this.addTexturedVertices(builder, matrix4f, intersectionOnCurrentTexture, intersectionOnCurrentFrame);
                quads++;
            }

            if (quads > 0) {
                NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, NuitRenderBackend.SAMPLER0_NAME, animatableTexture.getTexture().getTextureId());
            }
        }
    }

    private void renderFrameBlendedTextureFrame(RenderPipeline pipeline, GpuBufferSlice dynamicTransforms, AnimatableTexture animatableTexture, UVRange currentFrame, UVRange nextFrame, float frameBlend) {
        try (ByteBufferBuilder byteBufferBuilder = NuitRenderPipelines.byteBufferBuilder(pipeline, 24)) {
            FrameBlendedMeshBuilder builder = new FrameBlendedMeshBuilder(byteBufferBuilder, pipeline);
            UVRange textureUvRange = animatableTexture.getUvRange();
            int quads = 0;

            for (int face = 0; face < 6; ++face) {
                UVRange faceUVRange = Utils.TEXTURE_FACES[face];
                UVRange intersect = Utils.findUVIntersection(faceUVRange, textureUvRange);
                if (intersect == null) {
                    continue;
                }

                Matrix4f matrix4f = Utils.getMatrixForRotatedFace(face);
                UVRange intersectionOnCurrentTexture = Utils.mapUVRanges(faceUVRange, this.quad, intersect);
                UVRange intersectionOnCurrentFrame = Utils.mapUVRanges(textureUvRange, currentFrame, intersect);
                UVRange intersectionOnNextFrame = Utils.mapUVRanges(textureUvRange, nextFrame, intersect);
                this.addFrameBlendedVertices(builder, matrix4f, intersectionOnCurrentTexture, intersectionOnCurrentFrame, intersectionOnNextFrame, frameBlend);
                quads++;
            }

            if (quads > 0) {
                NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, NuitRenderBackend.SAMPLER0_NAME, animatableTexture.getTexture().getTextureId());
            }
        }
    }

    private void addTexturedVertices(BufferBuilder builder, Matrix4f matrix4f, UVRange textureQuad, UVRange currentFrame) {
        builder.addVertex(matrix4f, textureQuad.minU(), -this.quadSize, textureQuad.minV()).setUv(currentFrame.minU(), currentFrame.minV());
        builder.addVertex(matrix4f, textureQuad.minU(), -this.quadSize, textureQuad.maxV()).setUv(currentFrame.minU(), currentFrame.maxV());
        builder.addVertex(matrix4f, textureQuad.maxU(), -this.quadSize, textureQuad.maxV()).setUv(currentFrame.maxU(), currentFrame.maxV());
        builder.addVertex(matrix4f, textureQuad.maxU(), -this.quadSize, textureQuad.minV()).setUv(currentFrame.maxU(), currentFrame.minV());
    }

    private void addFrameBlendedVertices(FrameBlendedMeshBuilder builder, Matrix4f matrix4f, UVRange textureQuad, UVRange currentFrame, UVRange nextFrame, float frameBlend) {
        addFrameBlendedVertex(builder, matrix4f, textureQuad.minU(), textureQuad.minV(), currentFrame.minU(), currentFrame.minV(), nextFrame.minU(), nextFrame.minV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, textureQuad.minU(), textureQuad.maxV(), currentFrame.minU(), currentFrame.maxV(), nextFrame.minU(), nextFrame.maxV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, textureQuad.maxU(), textureQuad.maxV(), currentFrame.maxU(), currentFrame.maxV(), nextFrame.maxU(), nextFrame.maxV(), frameBlend);
        addFrameBlendedVertex(builder, matrix4f, textureQuad.maxU(), textureQuad.minV(), currentFrame.maxU(), currentFrame.minV(), nextFrame.maxU(), nextFrame.minV(), frameBlend);
    }

    private void addFrameBlendedVertex(FrameBlendedMeshBuilder builder, Matrix4f matrix4f, float x, float z, float currentU, float currentV, float nextU, float nextV, float frameBlend) {
        builder.addVertex(matrix4f, x, -this.quadSize, z, currentU, currentV, nextU, nextV, frameBlend);
    }

    public List<AnimatableTexture> getAnimations() {
        return this.animatableTextures;
    }

    @Override
    public List<Identifier> getTexturesToRegister() {
        return this.animatableTextures.stream().map(AnimatableTexture::getTexture).map(Texture::getTextureId).toList();
    }

    private static final class FrameBlendedMeshBuilder {
        private final ByteBufferBuilder byteBufferBuilder;
        private final VertexFormat vertexFormat;
        private final int vertexSize;
        private final int positionOffset;
        private final int uv0Offset;
        private final int nextUvOffset;
        private final int frameBlendOffset;
        private final Vector3f transformedPosition = new Vector3f();
        private int vertexCount;

        private FrameBlendedMeshBuilder(ByteBufferBuilder byteBufferBuilder, RenderPipeline pipeline) {
            this.byteBufferBuilder = byteBufferBuilder;
            this.vertexFormat = NuitRenderPipelines.vertexFormat(pipeline);
            this.vertexSize = this.vertexFormat.getVertexSize();
            this.positionOffset = this.vertexFormat.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME).offset();
            this.uv0Offset = this.vertexFormat.getElement(DefaultVertexFormat.UV0_SEMANTIC_NAME).offset();
            this.nextUvOffset = this.vertexFormat.getElement(NuitRenderPipelines.NEXT_UV_SEMANTIC_NAME).offset();
            this.frameBlendOffset = this.vertexFormat.getElement(NuitRenderPipelines.FRAME_BLEND_SEMANTIC_NAME).offset();
        }

        private void addVertex(Matrix4f matrix, float x, float y, float z, float currentU, float currentV, float nextU, float nextV, float frameBlend) {
            long pointer = this.byteBufferBuilder.reserve(this.vertexSize);
            matrix.transformPosition(x, y, z, this.transformedPosition);

            MemoryUtil.memPutFloat(pointer + this.positionOffset, this.transformedPosition.x());
            MemoryUtil.memPutFloat(pointer + this.positionOffset + 4L, this.transformedPosition.y());
            MemoryUtil.memPutFloat(pointer + this.positionOffset + 8L, this.transformedPosition.z());
            MemoryUtil.memPutFloat(pointer + this.uv0Offset, currentU);
            MemoryUtil.memPutFloat(pointer + this.uv0Offset + 4L, currentV);
            MemoryUtil.memPutFloat(pointer + this.nextUvOffset, nextU);
            MemoryUtil.memPutFloat(pointer + this.nextUvOffset + 4L, nextV);
            MemoryUtil.memPutFloat(pointer + this.frameBlendOffset, frameBlend);
            this.vertexCount++;
        }

        private MeshData buildOrThrow() {
            int indexCount = PrimitiveTopology.QUADS.indexCount(this.vertexCount);
            return new MeshData(
                    this.byteBufferBuilder.build(),
                    new MeshData.DrawState(this.vertexFormat, this.vertexCount, indexCount, PrimitiveTopology.QUADS, IndexType.least(indexCount))
            );
        }
    }
}
