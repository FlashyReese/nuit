package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.*;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.Utils;
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
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        RenderSystem.setShaderFog(fogParameters);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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
        RenderSystem.setShaderTexture(0, this.texture.getTextureId());
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return List.of(this.texture.getTextureId());
    }

    public Texture getTexture() {
        return this.texture;
    }
}
