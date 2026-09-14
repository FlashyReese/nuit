package me.flashyreese.mods.nuit.skybox.decorations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.skybox.TextureRegistrar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public class DecorationBox extends AbstractSkybox implements TextureRegistrar {
    public static Codec<DecorationBox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.decorations()).forGetter(DecorationBox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(DecorationBox::getConditions),
            ResourceLocation.CODEC.optionalFieldOf("sun", SkyRendererAccessor.getSun()).forGetter(DecorationBox::getSunTexture),
            ResourceLocation.CODEC.optionalFieldOf("moon", SkyRendererAccessor.getMoonPhases()).forGetter(DecorationBox::getMoonTexture),
            Codec.BOOL.optionalFieldOf("showSun", false).forGetter(DecorationBox::isSunEnabled),
            Codec.BOOL.optionalFieldOf("showMoon", false).forGetter(DecorationBox::isMoonEnabled),
            Codec.BOOL.optionalFieldOf("showStars", false).forGetter(DecorationBox::isStarsEnabled),
            Blend.CODEC.optionalFieldOf("blend", Blend.decorations()).forGetter(DecorationBox::getBlend)
    ).apply(instance, DecorationBox::new));

    private final ResourceLocation sunTexture;
    private final ResourceLocation moonTexture;
    private final boolean sunEnabled;
    private final boolean moonEnabled;
    private final boolean starsEnabled;
    private final Blend blend;

    public DecorationBox(Properties properties, Conditions conditions, ResourceLocation sun, ResourceLocation moon, boolean sunEnabled, boolean moonEnabled, boolean starsEnabled, Blend blend) {
        this.properties = properties;
        this.conditions = conditions;
        this.sunTexture = sun;
        this.moonTexture = moon;
        this.sunEnabled = sunEnabled;
        this.moonEnabled = moonEnabled;
        this.starsEnabled = starsEnabled;
        this.blend = blend;
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccessor, PoseStack poseStack, Matrix4f projectionMatrix,
                       float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        if (this.alpha <= 0.0F) {
            return;
        }

        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        try {
            NuitRenderBackend.beginSkybox(this.blend, this.alpha, GameRenderer::getPositionTexShader);
            try (NuitRenderBackend.TransformScope transform = NuitRenderBackend.pushTransform(poseStack)) {
                this.properties.rotation().apply(transform.poseStack(), level, this.properties.clock(), tickDelta);

                Matrix4f matrix4f2 = transform.poseStack().last().pose();

                if (this.sunEnabled) {
                    this.renderSun(matrix4f2);
                }

                if (this.moonEnabled) {
                    this.renderMoon(matrix4f2, level);
                }

                if (this.starsEnabled) {
                    this.renderStars(
                            skyRendererAccessor,
                            level,
                            transform.poseStack(),
                            projectionMatrix,
                            tickDelta,
                            fogCallback
                    );
                }
            }
        } finally {
            NuitRenderBackend.endSkybox();
        }
    }

    public void renderSun(Matrix4f matrix4f) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX
        );
        bufferBuilder.addVertex(matrix4f, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F);
        bufferBuilder.addVertex(matrix4f, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(matrix4f, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(matrix4f, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F);
        NuitRenderBackend.drawTextured(
                bufferBuilder.buildOrThrow(),
                GameRenderer::getPositionTexShader,
                this.sunTexture
        );
    }

    public void renderMoon(Matrix4f matrix4f, ClientLevel level) {
        int moonPhase = level.getMoonPhase();
        int xCoord = moonPhase % 4;
        int yCoord = moonPhase / 4 % 2;
        float startX = xCoord / 4.0F;
        float startY = yCoord / 2.0F;
        float endX = (xCoord + 1) / 4.0F;
        float endY = (yCoord + 1) / 2.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX
        );
        bufferBuilder.addVertex(matrix4f, -20.0F, -100.0F, 20.0F).setUv(endX, endY);
        bufferBuilder.addVertex(matrix4f, 20.0F, -100.0F, 20.0F).setUv(startX, endY);
        bufferBuilder.addVertex(matrix4f, 20.0F, -100.0F, -20.0F).setUv(startX, startY);
        bufferBuilder.addVertex(matrix4f, -20.0F, -100.0F, -20.0F).setUv(endX, startY);
        NuitRenderBackend.drawTextured(
                bufferBuilder.buildOrThrow(),
                GameRenderer::getPositionTexShader,
                this.moonTexture
        );
    }

    public void renderStars(SkyRendererAccessor skyRendererAccessor, ClientLevel level, PoseStack poseStack,
                            Matrix4f projectionMatrix, float tickDelta, Runnable fogCallback) {
        float brightness = level.getStarBrightness(tickDelta);
        if (brightness > 0.0F) {
            RenderSystem.setShaderColor(brightness, brightness, brightness, brightness);
            FogRenderer.setupNoFog();
            try {
                skyRendererAccessor.getStarsBuffer().bind();
                skyRendererAccessor.getStarsBuffer().drawWithShader(
                        poseStack.last().pose(),
                        projectionMatrix,
                        RenderSystem.getShader()
                );
            } finally {
                try {
                    VertexBuffer.unbind();
                } finally {
                    fogCallback.run();
                }
            }
        }
    }

    public ResourceLocation getSunTexture() {
        return this.sunTexture;
    }

    public ResourceLocation getMoonTexture() {
        return this.moonTexture;
    }

    public boolean isSunEnabled() {
        return this.sunEnabled;
    }

    public boolean isMoonEnabled() {
        return this.moonEnabled;
    }

    public boolean isStarsEnabled() {
        return this.starsEnabled;
    }

    public Blend getBlend() {
        return this.blend;
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        List<ResourceLocation> textures = new ArrayList<>(2);
        if (this.sunEnabled) {
            textures.add(this.sunTexture);
        }
        if (this.moonEnabled) {
            textures.add(this.moonTexture);
        }
        return textures;
    }
}
