package io.github.amerebagatelle.mods.nuit.skybox.decorations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    public void render(SkyRendererAccessor skyRendererAccessor, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters, Runnable fogCallback) {
        RenderSystem.enableBlend();
        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);

        // Custom Blender
        this.blend.applyBlendFunc(this.alpha);
        poseStack.pushPose();

        // static
        this.properties.rotation().rotateStack(poseStack, level);

        // Iris Compat
        //poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(IrisCompat.getSunPathRotation()));
        //poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(level.getSkyAngle(tickDelta) * 360.0F * this.decorations.getRotation().getRotationSpeed()));

        Matrix4f matrix4f2 = poseStack.last().pose();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);

        // Sun
        if (this.sunEnabled) {
            this.renderSun(matrix4f2);
        }

        // Moon
        if (this.moonEnabled) {
            this.renderMoon(matrix4f2, level);
        }

        // Stars
        if (this.starsEnabled) {
            this.renderStars(skyRendererAccessor, level, poseStack, tickDelta);
            bufferSource.endBatch();
        }

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public void renderSun(Matrix4f matrix4f) {
        VertexBuffer buffer = VertexBuffer.uploadStatic(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX, (vertexConsumer) -> {
            RenderSystem.setShaderTexture(0, this.sunTexture);
            vertexConsumer.addVertex(matrix4f, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F);
            vertexConsumer.addVertex(matrix4f, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F);
            vertexConsumer.addVertex(matrix4f, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F);
            vertexConsumer.addVertex(matrix4f, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F);
        });

        buffer.bind();
        buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    public void renderMoon(Matrix4f matrix4f, ClientLevel level) {
        int moonPhase = level.getMoonPhase();
        int xCoord = moonPhase % 4;
        int yCoord = moonPhase / 4 % 2;
        float startX = xCoord / 4.0F;
        float startY = yCoord / 2.0F;
        float endX = (xCoord + 1) / 4.0F;
        float endY = (yCoord + 1) / 2.0F;
        VertexBuffer buffer = VertexBuffer.uploadStatic(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX, (vertexConsumer) -> {
            RenderSystem.setShaderTexture(0, this.moonTexture);
            vertexConsumer.addVertex(matrix4f, -20.0F, -100.0F, 20.0F).setUv(endX, endY);
            vertexConsumer.addVertex(matrix4f, 20.0F, -100.0F, 20.0F).setUv(startX, endY);
            vertexConsumer.addVertex(matrix4f, 20.0F, -100.0F, -20.0F).setUv(startX, startY);
            vertexConsumer.addVertex(matrix4f, -20.0F, -100.0F, -20.0F).setUv(endX, startY);
        });

        buffer.bind();
        buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    public void renderStars(SkyRendererAccessor skyRendererAccessor, ClientLevel level, PoseStack poseStack, float tickDelta) {
        float i = 1.0F - level.getRainLevel(tickDelta);
        float brightness = level.getStarBrightness(tickDelta) * i;
        if (brightness > 0.0F) {
            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
            matrix4fStack.pushMatrix();
            matrix4fStack.mul(poseStack.last().pose());
            RenderSystem.setShaderColor(brightness, brightness, brightness, brightness);
            RenderSystem.setShaderFog(FogParameters.NO_FOG);
            skyRendererAccessor.getStarsBuffer().drawWithRenderType(RenderType.stars());
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