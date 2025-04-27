package io.github.amerebagatelle.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.TriState;
import org.joml.Matrix4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class EndSkybox extends AbstractSkybox {
    public static Codec<EndSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions)
    ).apply(instance, EndSkybox::new));

    public EndSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccessor, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        VertexFormat vertexFormat = DefaultVertexFormat.POSITION_TEX_COLOR;
        VertexFormat.Mode vertexFormatMode = VertexFormat.Mode.QUADS;

        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * 24);
        BufferBuilder builder = new BufferBuilder(byteBufferBuilder, vertexFormatMode, vertexFormat);
        Matrix4f matrix4f = poseStack.last().pose();
        for (int i = 0; i < 6; ++i) {
            switch (i) {
                case 1 -> matrix4f.rotationX(1.5707964F);
                case 2 -> matrix4f.rotationX(-1.5707964F);
                case 3 -> matrix4f.rotationX(3.1415927F);
                case 4 -> matrix4f.rotationZ(1.5707964F);
                case 5 -> matrix4f.rotationZ(-1.5707964F);
            }

            int color = ARGB.color(0x282828, (int) (255 * this.alpha));
            builder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(color);
            builder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(color);
            builder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(color);
            builder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(color);
        }

        int indexCount = 0;
        GpuBuffer vertexBuffer = null;
        MeshData meshData = builder.build();
        if (meshData != null) {
            indexCount = meshData.drawState().indexCount();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "End skybox", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, meshData.vertexBuffer());
        }

        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(vertexFormatMode);
        if (vertexBuffer != null) {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(SkyRenderer.END_SKY_LOCATION);
            abstractTexture.setFilter(TriState.FALSE, false);
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.getDepthTexture(), OptionalDouble.empty())) {
                renderPass.setPipeline(RenderPipelines.END_SKY);
                renderPass.setVertexBuffer(0, null);
                renderPass.setIndexBuffer(autoStorageIndexBuffer.getBuffer(indexCount), autoStorageIndexBuffer.type());
                renderPass.bindSampler("Sampler0", abstractTexture.getTexture());
                renderPass.drawIndexed(0, indexCount);
            } finally {
                vertexBuffer.close();
                meshData.close();
            }
        }
    }

    @Override
    public void close() {
    }
}