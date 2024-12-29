package io.github.amerebagatelle.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import io.github.amerebagatelle.mods.nuit.api.NuitApi;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.decorations.DecorationBox;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Objects;

public class OverworldSkybox extends AbstractSkybox {
    public static Codec<OverworldSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions)
    ).apply(instance, OverworldSkybox::new));

    public OverworldSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters, Runnable fogCallback) {
        fogCallback.run();
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = Objects.requireNonNull(client.level);

        int skyColor = world.getSkyColor(client.gameRenderer.getMainCamera().getPosition(), tickDelta);
        Vec3 vec3d = new Vec3(ARGB.red(skyColor), ARGB.green(skyColor), ARGB.blue(skyColor));
        float f = (float) vec3d.x;
        float g = (float) vec3d.y;
        float h = (float) vec3d.z;
        RenderSystem.setShaderFog(fogParameters);
        RenderSystem.depthMask(false);

        // Light Sky
        RenderSystem.setShaderColor(f, g, h, this.alpha);
        skyRendererAccess.getTopSkyBuffer().bind();
        skyRendererAccess.getTopSkyBuffer().drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.enableBlend();

        float skyAngleRadian = world.getSunAngle(tickDelta);
        if (SkyboxManager.getInstance().isEnabled() && NuitApi.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorationBox && decorationBox.getProperties().rotation().skyboxRotation())) {
            float skyAngle = Mth.positiveModulo(world.getDayTime() / 24000F + 0.75F, 1);
            skyAngleRadian = skyAngle * (float) (Math.PI * 2);
        }

        int fs = world.effects().getSunriseOrSunsetColor(tickDelta);
        if (fs != 0) {
            float skyColorR = ARGB.redFloat(skyColor);
            float skyColorG = ARGB.greenFloat(skyColor);
            float skyColorB = ARGB.blueFloat(skyColor);
            float skyColorA = ARGB.alphaFloat(skyColor);

            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            float i = Mth.sin(skyAngleRadian) < 0.0F ? 180.0F : 0.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(i));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            Matrix4f matrix4f = poseStack.last().pose();

            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.sunriseSunset());
            vertexConsumer.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(skyColorR, skyColorG, skyColorB, skyColorA * this.alpha);
            for (int n = 0; n <= 16; ++n) {
                float o = (float) n * (float) (Math.PI * 2) / 16.0F;
                float p = Mth.sin(o);
                float q = Mth.cos(o);
                vertexConsumer.addVertex(matrix4f, p * 120.0F, q * 120.0F, -q * 40.0F * skyColorA).setColor(skyColorR, skyColorG, skyColorB, 0.0F);
            }

            BufferUploader.drawWithShader(((BufferBuilder) vertexConsumer).buildOrThrow());
            poseStack.popPose();
        }

        // Dark Sky
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        double d = Objects.requireNonNull(client.player).getEyePosition(tickDelta).y - world.getLevelData().getHorizonHeight(world);
        if (d < 0.0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 12.0F, 0.0F);
            skyRendererAccess.getBottomSkyBuffer().bind();
            skyRendererAccess.getBottomSkyBuffer().drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            poseStack.popPose();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
