package me.flashyreese.mods.nuit.skybox.decorations;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.OverrideUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL46C;

import java.util.Objects;

public class DecorationBox extends AbstractSkybox {
    public static Codec<DecorationBox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.decorations()).forGetter(DecorationBox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(DecorationBox::getConditions),
            Identifier.CODEC.optionalFieldOf("sun", Identifier.withDefaultNamespace("textures/environment/sun.png")).forGetter(DecorationBox::getSunTexture),
            Identifier.CODEC.optionalFieldOf("moon", Identifier.withDefaultNamespace("textures/environment/moon_phases.png")).forGetter(DecorationBox::getMoonTexture),
            Codec.BOOL.optionalFieldOf("showSun", false).forGetter(DecorationBox::isSunEnabled),
            Codec.BOOL.optionalFieldOf("showMoon", false).forGetter(DecorationBox::isMoonEnabled),
            Codec.BOOL.optionalFieldOf("showStars", false).forGetter(DecorationBox::isStarsEnabled),
            Blend.CODEC.optionalFieldOf("blend", Blend.decorations()).forGetter(DecorationBox::getBlend)
    ).apply(instance, DecorationBox::new));
    private final Identifier sunTexture;
    private final Identifier moonTexture;
    private final boolean sunEnabled;
    private final boolean moonEnabled;
    private final boolean starsEnabled;
    private final Blend blend;

    public DecorationBox(Properties properties, Conditions conditions, Identifier sun, Identifier moon, boolean sunEnabled, boolean moonEnabled, boolean starsEnabled, Blend blend) {
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
    public void render(SkyRendererAccessor skyRendererAccessor, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        RenderSystem.setShaderFog(fogParameters);
        ClientLevel level = Objects.requireNonNull((ClientLevel) camera.entity().level());

        OverrideUtils.enableBlendingOverride(this.blend.getBlendFunction());
        poseStack.pushPose();
        this.properties.rotation().apply(poseStack, level);

        // poseStack.mulPose(Axis.YP.rotation(-90F));
        // poseStack.mulPose(Axis.YP.rotation(level.getTimeOfDay(tickDelta) * 360.0F));
        // Iris Compat
        // poseStack.mulPose(Axis.ZP.rotationDegrees(IrisCompat.getSunPathRotation()));
        // poseStack.mulPose(Axis.XP.rotationDegrees(level.getSunAngle(tickDelta) * 360.0F * this.properties.rotation().speed()));

        if (this.sunEnabled) {
            this.renderSun(bufferSource, poseStack);
        }

        if (this.moonEnabled) {
            this.renderMoon(camera.attributeProbe().getValue(EnvironmentAttributes.MOON_PHASE, tickDelta).index(), bufferSource, poseStack);
        }

        if (this.sunEnabled || this.moonEnabled) {
            bufferSource.endBatch();
        }

        if (this.starsEnabled) {
            skyRendererAccessor.invokeRenderStars(camera.attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, tickDelta), poseStack);
        }

        poseStack.popPose();
        OverrideUtils.disableBlendingOverride();
        GL46C.glBlendEquation(GL46C.GL_FUNC_ADD);
    }

    private void renderSun(MultiBufferSource multiBufferSource, PoseStack poseStack) {
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderTypes.entityTranslucent(this.sunTexture));
        int i = ARGB.white(this.alpha);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, -30.0F, 100.0F, -30.0F, 0.0F, 0.0F, i);
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, 30.0F, 100.0F, -30.0F, 1.0F, 0.0F, i);
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, 30.0F, 100.0F, 30.0F, 1.0F, 1.0F, i);
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, -30.0F, 100.0F, 30.0F, 0.0F, 1.0F, i);
    }

    private void renderMoon(int moonPhase, MultiBufferSource multiBufferSource, PoseStack poseStack) {
        int xCoord = moonPhase % 4;
        int yCoord = moonPhase / 4 % 2;
        float startX = xCoord / 4.0F;
        float startY = yCoord / 2.0F;
        float endX = (xCoord + 1) / 4.0F;
        float endY = (yCoord + 1) / 2.0F;
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderTypes.entityTranslucent(this.moonTexture));
        int p = ARGB.white(this.alpha);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, -20.0F, -100.0F, 20.0F, endX, endY, p);
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, 20.0F, -100.0F, 20.0F, startX, endY, p);
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, 20.0F, -100.0F, -20.0F, startX, startY, p);
        this.addCelestialVertex(vertexConsumer, pose, matrix4f, -20.0F, -100.0F, -20.0F, endX, startY, p);
    }

    private void addCelestialVertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, Matrix4f matrix4f, float x, float y, float z, float u, float v, int color) {
        vertexConsumer.addVertex(matrix4f, x, y, z)
                .setUv(u, v)
                .setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public Identifier getSunTexture() {
        return this.sunTexture;
    }

    public Identifier getMoonTexture() {
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
