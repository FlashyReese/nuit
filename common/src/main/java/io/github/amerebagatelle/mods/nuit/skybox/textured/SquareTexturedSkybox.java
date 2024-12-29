package io.github.amerebagatelle.mods.nuit.skybox.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.components.*;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

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
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters, Runnable fogCallback) {
        VertexBuffer buffer = VertexBuffer.uploadStatic(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX, (vertexConsumer) -> {
            RenderSystem.setShaderTexture(0, this.texture.getTextureId());
            for (int face = 0; face < 6; face++) {
                // 0 = bottom | 1 = north | 2 = south | 3 = top | 4 = east | 5 = west
                UVRange tex = Utils.TEXTURE_FACES[face];
                poseStack.pushPose();
                Utils.rotateSkyBoxByFace(poseStack, face);
                Matrix4f matrix4f = poseStack.last().pose();
                vertexConsumer.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(tex.getMinU(), tex.getMinV());
                vertexConsumer.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(tex.getMinU(), tex.getMaxV());
                vertexConsumer.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(tex.getMaxU(), tex.getMaxV());
                vertexConsumer.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(tex.getMaxU(), tex.getMinV());
                poseStack.popPose();
            }
        });

        buffer.bind();
        buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return List.of(this.texture.getTextureId());
    }

    public Texture getTexture() {
        return this.texture;
    }
}
