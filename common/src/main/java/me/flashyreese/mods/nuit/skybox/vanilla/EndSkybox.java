package me.flashyreese.mods.nuit.skybox.vanilla;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;

public class EndSkybox extends AbstractSkybox {
    private static final float MIN_END_FLASH_INTENSITY = 0.00001F;
    public static Codec<EndSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions)
    ).apply(instance, EndSkybox::new));

    public EndSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
    }

    @Override
    public void render(SkyboxRenderContext context) {
        RenderPipeline pipeline = RenderPipelines.END_SKY;
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 24)) {
            BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            for (int face = 0; face < 6; ++face) {
                int color = ARGB.color((int) (255 * this.alpha), 0x282828);
                Matrix4f matrix4f = Utils.getMatrixForRotatedFace(face);
                builder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(color);
                builder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(color);
                builder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(color);
                builder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(color);
            }

            GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms();
            NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, "Sampler0", context.endSkyTexture());
        }

        this.renderEndFlash(context);
    }

    private void renderEndFlash(SkyboxRenderContext context) {
        if (!(context.camera().entity().level() instanceof ClientLevel level)) {
            return;
        }

        EndFlashState endFlashState = level.endFlashState();
        if (endFlashState == null) {
            return;
        }

        float intensity = endFlashState.getIntensity(context.tickDelta()) * this.alpha;
        if (intensity > MIN_END_FLASH_INTENSITY) {
            context.renderEndFlash(intensity, endFlashState.getXAngle(), endFlashState.getYAngle());
        }
    }
}
