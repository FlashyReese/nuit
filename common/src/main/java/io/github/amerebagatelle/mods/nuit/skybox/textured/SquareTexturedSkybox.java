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

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class SquareTexturedSkybox extends TexturedSkybox implements AutoCloseable {
    public static Codec<SquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(TexturedSkybox::getBlend),
            Texture.CODEC.fieldOf("texture").forGetter(SquareTexturedSkybox::getTexture)
    ).apply(instance, SquareTexturedSkybox::new));
    protected Texture texture;

    public SquareTexturedSkybox(Properties properties, Conditions conditions, Blend blend, Texture texture) {
        super(properties, conditions, blend);
        this.texture = texture;
        buildSky();
    }

    private int indexCount = 0;
    private RenderSystem.AutoStorageIndexBuffer skyIndices;
    private GpuBuffer vertexBuffer = null;

    private void buildSky() {
        PoseStack poseStack = new PoseStack();

        VertexFormat vertexFormat = DefaultVertexFormat.POSITION_TEX;
        VertexFormat.Mode vertexFormatMode = VertexFormat.Mode.QUADS;

        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * 24);
        BufferBuilder builder = new BufferBuilder(byteBufferBuilder, vertexFormatMode, vertexFormat);
        for (int face = 0; face < 6; face++) {
            // 0 = bottom | 1 = north | 2 = south | 3 = top | 4 = east | 5 = west
            UVRange tex = Utils.TEXTURE_FACES[face];
            poseStack.pushPose();
            Utils.rotateSkyBoxByFace(poseStack, face);
            Matrix4f matrix4f = poseStack.last().pose();
            builder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(tex.minU(), tex.minV());
            builder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(tex.minU(), tex.maxV());
            builder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(tex.maxU(), tex.maxV());
            builder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(tex.maxU(), tex.minV());
            poseStack.popPose();
        }

        skyIndices = RenderSystem.getSequentialBuffer(vertexFormatMode);
        try (MeshData meshData = builder.build()) {
            if (meshData != null) {
                indexCount = meshData.drawState().indexCount();
                vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Square textured skybox", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            }
        }
    }

    @Override
    public void renderSkybox(SkyRenderer skyRenderer, PoseStack poseStack, float tickDelta, Camera camera, FogParameters fogParameters) {
        RenderSystem.setShaderFog(fogParameters);
        if (vertexBuffer != null) {
            GpuTexture texture = Minecraft.getInstance().getTextureManager().getTexture(this.texture.getTextureId()).getTexture();
            RenderPipeline pipeline = TEXTURED_SKYBOX_PIPELINE_CONSUMER.apply(this.getBlend().getBlendFunction());
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.getDepthTexture(), OptionalDouble.empty())) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(skyIndices.getBuffer(indexCount), skyIndices.type());
                renderPass.bindSampler("Sampler0", texture);
                renderPass.drawIndexed(0, indexCount);
            }
        }
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return List.of(this.texture.getTextureId());
    }

    public Texture getTexture() {
        return this.texture;
    }

    @Override
    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
