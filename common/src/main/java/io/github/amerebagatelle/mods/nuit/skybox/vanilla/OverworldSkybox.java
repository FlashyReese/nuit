package io.github.amerebagatelle.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class OverworldSkybox extends AbstractSkybox {
    public static Codec<OverworldSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions)
    ).apply(instance, OverworldSkybox::new));

    public OverworldSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        RenderSystem.setShaderFog(fogParameters);
        ClientLevel level = (ClientLevel) camera.getEntity().level();
        float sunAngle = level.getSunAngle(tickDelta);
        float timeOfDay = level.getTimeOfDay(tickDelta);
        int sunriseOrSunsetColor = level.effects().getSunriseOrSunsetColor(timeOfDay);
        int skyColor = level.getSkyColor(camera.getPosition(), tickDelta);

        // Light Sky
        this.renderLightSky(skyColor, skyRendererAccess);

        if (level.effects().isSunriseOrSunset(timeOfDay)) {
            if (SkyboxManager.getInstance().isEnabled() && NuitApi.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorationBox && decorationBox.getProperties().rotation().skyboxRotation())) {
                sunAngle = Mth.positiveModulo(level.getDayTime() / 24000F + 0.75F, 1);
            }
            this.renderSunriseAndSunset(poseStack, bufferSource, sunAngle, sunriseOrSunsetColor);
        }

        bufferSource.endBatch();

        // Dark Sky
        double eyeHeight = camera.getEntity().getEyePosition(tickDelta).y - level.getLevelData().getHorizonHeight(level);
        if (eyeHeight < 0.0) {
            this.renderDarkSky(poseStack, skyRendererAccess);
        }
    }

    private void renderSunriseAndSunset(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float sunAngle, int sunriseOrSunsetColor) {
        poseStack.pushPose();

        // Rotate to orient the effect properly
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        float zRotation = Mth.sin(sunAngle) < 0.0F ? 180.0F : 0.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

        Matrix4f transformationMatrix = poseStack.last().pose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.sunriseSunset());

        sunriseOrSunsetColor = ARGB.color(sunriseOrSunsetColor, (int) (this.alpha * 255));

        float alpha = ARGB.alphaFloat(sunriseOrSunsetColor);
        vertexConsumer.addVertex(transformationMatrix, 0.0F, 100.0F, 0.0F).setColor(sunriseOrSunsetColor);

        int transparentColor = ARGB.transparent(sunriseOrSunsetColor);

        for (int i = 0; i <= 16; i++) {
            float angleRadians = (float) i * ((float) Math.PI * 2F) / 16.0F;
            float x = Mth.sin(angleRadians);
            float y = Mth.cos(angleRadians);
            float z = -y * 40.0F * alpha;

            vertexConsumer.addVertex(transformationMatrix, x * 120.0F, y * 120.0F, z).setColor(transparentColor);
        }

        poseStack.popPose();
    }


    private void renderLightSky(int skyColor, SkyRendererAccessor skyRendererAccessor) {
        RenderSystem.setShaderColor(ARGB.redFloat(skyColor), ARGB.greenFloat(skyColor), ARGB.blueFloat(skyColor), this.alpha);
        skyRendererAccessor.getTopSkyBuffer().drawWithRenderType(RenderType.sky());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderDarkSky(PoseStack poseStack, SkyRendererAccessor skyRendererAccessor) {
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, this.alpha);
        poseStack.pushPose();
        poseStack.translate(0.0F, 12.0F, 0.0F);
        skyRendererAccessor.getBottomSkyBuffer().drawWithRenderType(RenderType.sky());
        poseStack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
