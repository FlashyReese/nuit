package io.github.amerebagatelle.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.renderer.*;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

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
        level.getRainLevel(tickDelta);
        int sunriseOrSunsetColor = level.effects().getSunriseOrSunsetColor(timeOfDay);
        int skyColor = level.getSkyColor(camera.getPosition(), tickDelta);

        // Light Sky
        RenderSystem.setShaderColor(ARGB.redFloat(skyColor), ARGB.greenFloat(skyColor), ARGB.blueFloat(skyColor), this.alpha);
        skyRendererAccess.getTopSkyBuffer().drawWithRenderType(RenderType.sky());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (level.effects().isSunriseOrSunset(timeOfDay)) {
            if (SkyboxManager.getInstance().isEnabled() && NuitApi.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorationBox && decorationBox.getProperties().rotation().skyboxRotation())) {
                sunAngle = Mth.positiveModulo(level.getDayTime() / 24000F + 0.75F, 1);
            }
            ((SkyRenderer) skyRendererAccess).renderSunriseAndSunset(poseStack, bufferSource, sunAngle, sunriseOrSunsetColor);
        }

        bufferSource.endBatch();

        // Dark Sky
        double eyeHeight = camera.getEntity().getEyePosition(tickDelta).y - level.getLevelData().getHorizonHeight(level);
        if (eyeHeight < 0.0) {
            ((SkyRenderer) skyRendererAccess).renderDarkDisc(poseStack);
        }
    }
}
