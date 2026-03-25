package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.*;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.BufferUploader;
import me.flashyreese.mods.nuit.util.DynamicTransformsBuilder;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;

public class SquareTexturedSkybox extends TexturedSkybox {
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
    }


    @Override
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, DynamicTransformsBuilder transformsBuilder, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        RenderSystem.setShaderFog(fogParameters);
        RenderPipeline pipeline = TEXTURED_SKYBOX_PIPELINE_CONSUMER.apply(this.getBlend().getBlendFunction());
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 24)) {
            BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            for (int face = 0; face < 6; face++) {
                UVRange tex = Utils.TEXTURE_FACES[face];
                Matrix4f matrix4f = Utils.getMatrixForRotatedFace(face);
                builder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(tex.minU(), tex.minV());
                builder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(tex.minU(), tex.maxV());
                builder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(tex.maxU(), tex.maxV());
                builder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(tex.maxU(), tex.minV());
            }
            GpuBufferSlice dynamicTransforms = transformsBuilder.build();
            GpuTextureView textureView = Minecraft.getInstance().getTextureManager().getTexture(this.texture.getTextureId()).getTextureView();
            BufferUploader.drawWithShader(pipeline, builder.buildOrThrow(), (pass) -> {
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.bindSampler("Sampler0", textureView);
            });
        }
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return List.of(this.texture.getTextureId());
    }

    public Texture getTexture() {
        return this.texture;
    }
}
