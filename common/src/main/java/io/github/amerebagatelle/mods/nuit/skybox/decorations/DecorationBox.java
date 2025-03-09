package io.github.amerebagatelle.mods.nuit.skybox.decorations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Objects;

public class DecorationBox extends AbstractSkybox {
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
    public void render(SkyRendererAccessor skyRendererAccessor, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderFog(fogParameters);
        ClientLevel level = Objects.requireNonNull((ClientLevel) camera.getEntity().level());

        this.blend.apply(this.alpha);
        poseStack.pushPose();
        this.properties.rotation().apply(poseStack, level);

        // Iris Compat
        //poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(IrisCompat.getSunPathRotation()));
        //poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(level.getSkyAngle(tickDelta) * 360.0F * this.decorations.getRotation().getRotationSpeed()));

        Matrix4f matrix4f2 = poseStack.last().pose();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        if (this.sunEnabled) {
            this.renderSun(matrix4f2);
        }

        if (this.moonEnabled) {
            this.renderMoon(matrix4f2, level.getMoonPhase());
        }

        if (this.starsEnabled) {
            this.renderStars(skyRendererAccessor, level, matrix4f2, fogParameters, tickDelta);
        }

        poseStack.popPose();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public void renderSun(Matrix4f matrix4f) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix4f, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F);
        builder.addVertex(matrix4f, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F);
        builder.addVertex(matrix4f, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F);
        builder.addVertex(matrix4f, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F);
        RenderSystem.setShaderTexture(0, this.sunTexture);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public void renderMoon(Matrix4f matrix4f, int moonPhase) {
        int xCoord = moonPhase % 4;
        int yCoord = moonPhase / 4 % 2;
        float startX = xCoord / 4.0F;
        float startY = yCoord / 2.0F;
        float endX = (xCoord + 1) / 4.0F;
        float endY = (yCoord + 1) / 2.0F;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix4f, -20.0F, -100.0F, 20.0F).setUv(endX, endY);
        builder.addVertex(matrix4f, 20.0F, -100.0F, 20.0F).setUv(startX, endY);
        builder.addVertex(matrix4f, 20.0F, -100.0F, -20.0F).setUv(startX, startY);
        builder.addVertex(matrix4f, -20.0F, -100.0F, -20.0F).setUv(endX, startY);
        RenderSystem.setShaderTexture(0, this.moonTexture);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public void renderStars(SkyRendererAccessor skyRendererAccessor, ClientLevel level, Matrix4f matrix4f, FogParameters fogParameters, float tickDelta) {
        float rainLevel = 1.0F - level.getRainLevel(tickDelta);
        float brightness = level.getStarBrightness(tickDelta) * rainLevel;
        if (brightness > 0.0F) {
            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
            matrix4fStack.pushMatrix();
            matrix4fStack.mul(matrix4f);
            RenderSystem.setShaderColor(brightness, brightness, brightness, brightness);
            RenderSystem.setShaderFog(FogParameters.NO_FOG);
            skyRendererAccessor.getStarsBuffer().bind();
            skyRendererAccessor.getStarsBuffer().drawWithShader(RenderSystem.getModelViewStack(), RenderSystem.getProjectionMatrix(), RenderSystem.setShader(CoreShaders.POSITION));
            VertexBuffer.unbind();
            RenderSystem.setShaderFog(fogParameters);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            matrix4fStack.popMatrix();
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
}