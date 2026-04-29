package me.flashyreese.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.api.NuitApi;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.skybox.decorations.DecorationBox;
import me.flashyreese.mods.nuit.util.BufferUploader;
import me.flashyreese.mods.nuit.util.DynamicTransformsBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Matrix4fStack;

public class OverworldSkybox extends AbstractSkybox {
    public static Codec<OverworldSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions)
    ).apply(instance, OverworldSkybox::new));

    public OverworldSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccessor, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        RenderSystem.setShaderFog(fogParameters);

        ClientLevel level = (ClientLevel) camera.entity().level();
        float sunAngle = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.SUN_ANGLE);
        int sunriseOrSunsetColor = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR);
        int skyColor = level.environmentAttributes().getValue(EnvironmentAttributes.SKY_COLOR, camera.position());

        // Light Sky
        ((SkyRenderer) skyRendererAccessor).renderSkyDisc(skyColor);
        if (level.environmentAttributes().getDimensionValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR) > 0) {
            if (NuitApi.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorationBox && decorationBox.getProperties().rotation().skyboxRotation())) {
                sunAngle = Mth.positiveModulo(level.getDayTime() / 24000F + 0.75F, 1);
            }

            this.renderSunriseAndSunset(matrix4fStack, sunAngle, sunriseOrSunsetColor);
        }

        // Dark Sky
        double eyeHeight = camera.entity().getEyePosition(tickDelta).y - level.getLevelData().getHorizonHeight(level);
        if (eyeHeight < 0.0) {
            ((SkyRenderer) skyRendererAccessor).renderDarkDisc();
        }
    }

    private void renderSunriseAndSunset(Matrix4fStack matrix4fStack, float sunAngle, int sunriseOrSunsetColor) {
        matrix4fStack.pushMatrix();

        // Rotate to orient the effect properly
        matrix4fStack.rotate(Axis.XP.rotationDegrees(90.0F));
        float zRotation = Mth.sin(sunAngle) < 0.0F ? 180.0F : 0.0F;
        matrix4fStack.rotate(Axis.ZP.rotationDegrees(zRotation));
        matrix4fStack.rotate(Axis.ZP.rotationDegrees(90.0F));

        RenderPipeline pipeline = RenderPipelines.SUNRISE_SUNSET;
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 17)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());

            float alpha = ARGB.alphaFloat(sunriseOrSunsetColor) * this.alpha;
            bufferBuilder.addVertex(matrix4fStack, 0.0F, 100.0F, 0.0F).setColor(sunriseOrSunsetColor);

            int transparentColor = ARGB.transparent(sunriseOrSunsetColor);
            for (int i = 0; i <= 16; i++) {
                float angleRadians = (float) i * ((float) Math.PI * 2F) / 16.0F;
                float x = Mth.sin(angleRadians);
                float y = Mth.cos(angleRadians);
                float z = -y * 40.0F * alpha;
                bufferBuilder.addVertex(matrix4fStack, x * 120.0F, y * 120.0F, z).setColor(transparentColor);
            }
            GpuBufferSlice dynamicTransforms = new DynamicTransformsBuilder().build();
            BufferUploader.drawWithShader(pipeline, bufferBuilder.buildOrThrow(), (pass) -> pass.setUniform("DynamicTransforms", dynamicTransforms));
        }
        matrix4fStack.popMatrix();
    }
}
