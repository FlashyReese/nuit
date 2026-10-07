package me.flashyreese.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitRenderPipelines;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.joml.Vector4fc;


public class OverworldSkybox extends AbstractSkybox {
    public static Codec<OverworldSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.OVERWORLD_CODEC.optionalFieldOf("properties", Properties.overworld()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions)
    ).apply(instance, OverworldSkybox::new));

    public OverworldSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
    }

    @Override
    public void render(SkyboxRenderContext context) {
        context.applyFog();

        float alpha = context.alpha();
        Vector4fc sunriseOrSunsetColor = context.sunriseAndSunsetColor();
        context.renderSkyDisc(new Vector4f(context.skyColor(), alpha));
        if (sunriseOrSunsetColor.w() > 0.0F) {
            float sunAngle = context.skyAngle() * Mth.DEG_TO_RAD;
            this.renderSunriseAndSunset(
                    context.skyModelViewStack(),
                    sunAngle,
                    ARGB.colorFromVector4f(sunriseOrSunsetColor),
                    alpha
            );
        }

    }

    private void renderSunriseAndSunset(Matrix4fStack matrix4fStack, float sunAngle, int sunriseOrSunsetColor, float skyboxAlpha) {
        Matrix4f matrix = new Matrix4f(matrix4fStack);
        matrix.rotate(Axis.XP.rotationDegrees(90.0F));
        float zRotation = Mth.sin(sunAngle) < 0.0F ? 180.0F : 0.0F;
        matrix.rotate(Axis.ZP.rotationDegrees(zRotation));
        matrix.rotate(Axis.ZP.rotationDegrees(90.0F));

        RenderPipeline pipeline = RenderPipelines.SUNRISE_SUNSET;
        try (ByteBufferBuilder byteBufferBuilder = NuitRenderPipelines.byteBufferBuilder(pipeline, 18)) {
            BufferBuilder bufferBuilder = NuitRenderPipelines.bufferBuilder(byteBufferBuilder, pipeline);

            float alpha = ARGB.alphaFloat(sunriseOrSunsetColor) * skyboxAlpha;
            int color = ARGB.color(alpha, sunriseOrSunsetColor);
            bufferBuilder.addVertex(0.0F, 100.0F, 0.0F).setColor(color);

            int transparentColor = ARGB.transparent(sunriseOrSunsetColor);
            for (int i = 0; i <= 16; i++) {
                float angleRadians = (float) i * Mth.TWO_PI / 16.0F;
                float x = Mth.sin(angleRadians);
                float y = Mth.cos(angleRadians);
                float z = -y * 40.0F * alpha;
                bufferBuilder.addVertex(x * 120.0F, y * 120.0F, z).setColor(transparentColor);
            }
            Vector4f whiteColorModifier = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
            GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(matrix, whiteColorModifier);
            NuitRenderBackend.draw(pipeline, bufferBuilder.buildOrThrow(), dynamicTransforms);
        }
    }
}
