package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.*;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.List;

public class SquareTexturedSkybox extends TexturedSkybox {
    public static Codec<SquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            Texture.CODEC.fieldOf("texture").forGetter(SquareTexturedSkybox::getTexture)
    ).apply(instance, SquareTexturedSkybox::new));

    protected Texture texture;

    public SquareTexturedSkybox(Properties properties, Conditions conditions, Texture texture) {
        super(properties, conditions);
        this.texture = texture;
    }

    public SquareTexturedSkybox(Properties properties, Conditions conditions, Blend blend, Texture texture) {
        this(properties.withBlend(blend), conditions, texture);
    }

    @Override
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, Matrix4f projectionMatrix,
                             float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX
        );
        for (int face = 0; face < 6; face++) {
            UVRange tex = Utils.TEXTURE_FACES[face];
            poseStack.pushPose();
            try {
                Utils.rotateSkyBoxByFace(poseStack, face);
                Matrix4f matrix4f = poseStack.last().pose();
                bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(tex.minU(), tex.minV());
                bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(tex.minU(), tex.maxV());
                bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(tex.maxU(), tex.maxV());
                bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(tex.maxU(), tex.minV());
            } finally {
                poseStack.popPose();
            }
        }
        NuitRenderBackend.drawTextured(
                bufferBuilder.buildOrThrow(),
                GameRenderer::getPositionTexShader,
                this.texture.getTextureId()
        );
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return List.of(this.texture.getTextureId());
    }

    public Texture getTexture() {
        return this.texture;
    }
}
