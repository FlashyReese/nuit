package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.mixin.RenderPipelinesAccessor;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.skybox.TextureRegistrar;
import me.flashyreese.mods.nuit.util.DynamicTransformsBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.Objects;
import java.util.function.Function;

public abstract class TexturedSkybox extends AbstractSkybox implements TextureRegistrar {
    public static final Function<BlendFunction, RenderPipeline> TEXTURED_SKYBOX_PIPELINE_CONSUMER = (blendFunction) -> {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesAccessor.getMatricesProjectSnippet());
        builder.withLocation(ResourceLocation.tryBuild(NuitClient.MOD_ID, "pipeline/textured_skybox"));
        builder.withVertexShader("core/position_tex");
        builder.withFragmentShader("core/position_tex");
        builder.withDepthWrite(false);
        if (blendFunction != null) {
            builder.withBlend(blendFunction);
        } else {
            builder.withoutBlend();
        }
        builder.withSampler("Sampler0");
        builder.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS);
        return builder.build();
    };
    private final Rotation rotation;
    private final Blend blend;

    protected TexturedSkybox(Properties properties, Conditions conditions, Blend blend) {
        super(properties, conditions);
        this.blend = blend;
        this.rotation = properties.rotation();
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public Blend getBlend() {
        return this.blend;
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param poseStack         The current PoseStack.
     * @param skyRendererAccess Access to the skyRenderer as skyboxes often require it.
     * @param tickDelta         The current tick delta.
     * @param bufferSource
     */
    @Override
    public final void render(SkyRendererAccessor skyRendererAccess, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        Vector4f colorModifier = this.blend.applyEquationAndGetColor(this.alpha);
        DynamicTransformsBuilder transformsBuilder = new DynamicTransformsBuilder()
                .withShaderColor(colorModifier);

        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        matrix4fStack.pushMatrix();
        // TODO/NOTE: Should matrix4fStack inherit the current pose from poseStack?
        //  (currently idk if poseStack contains anything so I just ignored it)
        this.rotation.apply(matrix4fStack, level);
        this.renderSkybox(skyRendererAccess, matrix4fStack, tickDelta, camera, transformsBuilder, fogParameters, bufferSource);
        matrix4fStack.popMatrix();

        GL46C.glBlendEquation(GL46C.GL_FUNC_ADD); // Fixme: avoid direct gl calls
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(SkyRendererAccessor skyRendererAccess, Matrix4fStack matrix4f, float tickDelta, Camera camera, DynamicTransformsBuilder transformsBuilder, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource);
}
