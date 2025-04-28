package io.github.amerebagatelle.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.components.*;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

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
    public void renderSkybox(SkyRenderer skyRenderer, PoseStack poseStack, float tickDelta, Camera camera, FogParameters fogParameters) {
        RenderSystem.setShaderFog(fogParameters);
        for (int face = 0; face < 6; ++face) {
            // 0 = bottom | 1 = north | 2 = south | 3 = top | 4 = east | 5 = west
            // List of UV ranges for each face of the cube
            poseStack.pushPose();
            Utils.rotateSkyBoxByFace(poseStack, face);
            Matrix4f matrix4f = poseStack.last().pose();

            // animations
            UVRange faceUVRange = Utils.TEXTURE_FACES[face];
            for (AnimatableTexture animatableTexture : this.animatableTextures) {
                animatableTexture.tick();
                UVRange intersect = Utils.findUVIntersection(faceUVRange, animatableTexture.getUvRange()); // todo: cache this intersections so we don't waste gpu cycles
                if (intersect != null && animatableTexture.getCurrentFrame() != null) {
                    UVRange intersectionOnCurrentTexture = Utils.mapUVRanges(faceUVRange, this.quad, intersect);
                    UVRange intersectionOnCurrentFrame = Utils.mapUVRanges(animatableTexture.getUvRange(), animatableTexture.getCurrentFrame(), intersect);

                    // Render the quad at the calculated position
                    VertexFormat vertexFormat = DefaultVertexFormat.POSITION_TEX;
                    VertexFormat.Mode vertexFormatMode = VertexFormat.Mode.QUADS;

                    ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * 4);
                    BufferBuilder builder = new BufferBuilder(byteBufferBuilder, vertexFormatMode, vertexFormat);
                    builder.addVertex(matrix4f, intersectionOnCurrentTexture.minU(), -this.quadSize, intersectionOnCurrentTexture.minV()).setUv(intersectionOnCurrentFrame.minU(), intersectionOnCurrentFrame.minV());
                    builder.addVertex(matrix4f, intersectionOnCurrentTexture.minU(), -this.quadSize, intersectionOnCurrentTexture.maxV()).setUv(intersectionOnCurrentFrame.minU(), intersectionOnCurrentFrame.maxV());
                    builder.addVertex(matrix4f, intersectionOnCurrentTexture.maxU(), -this.quadSize, intersectionOnCurrentTexture.maxV()).setUv(intersectionOnCurrentFrame.maxU(), intersectionOnCurrentFrame.maxV());
                    builder.addVertex(matrix4f, intersectionOnCurrentTexture.maxU(), -this.quadSize, intersectionOnCurrentTexture.minV()).setUv(intersectionOnCurrentFrame.maxU(), intersectionOnCurrentFrame.minV());

                    int indexCount = 0;
                    GpuBuffer vertexBuffer = null;
                    MeshData meshData = builder.build();
                    if (meshData != null) {
                        indexCount = meshData.drawState().indexCount();
                        vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Multi textured skybox", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, meshData.vertexBuffer());
                    }

                    if (vertexBuffer != null) {
                        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(vertexFormatMode);
                        GpuTexture texture = Minecraft.getInstance().getTextureManager().getTexture(animatableTexture.getTexture().getTextureId()).getTexture();
                        RenderPipeline pipeline = TEXTURED_SKYBOX_PIPELINE_CONSUMER.apply(this.getBlend().getBlendFunction());
                        RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
                        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.getDepthTexture(), OptionalDouble.empty())) {
                            renderPass.setPipeline(pipeline);
                            renderPass.setVertexBuffer(0, vertexBuffer);
                            renderPass.setIndexBuffer(autoStorageIndexBuffer.getBuffer(indexCount), autoStorageIndexBuffer.type());
                            renderPass.bindSampler("Sampler0", texture);
                            renderPass.drawIndexed(0, indexCount);
                        } finally {
                            vertexBuffer.close();
                            meshData.close();
                        }
                    }
                }
            }

            poseStack.popPose();
        }
    }

    public List<AnimatableTexture> getAnimations() {
        return this.animatableTextures;
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return this.animatableTextures.stream().map(texture -> texture.getTexture().getTextureId()).toList();
    }

    @Override
    public void close() {
    }
}
