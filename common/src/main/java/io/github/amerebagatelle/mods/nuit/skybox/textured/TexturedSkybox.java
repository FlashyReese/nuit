package io.github.amerebagatelle.mods.nuit.skybox.textured;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.components.Rotation;
import io.github.amerebagatelle.mods.nuit.mixin.RenderPipelinesAccessor;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.TextureRegistrar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.Objects;
import java.util.function.Function;

public abstract class TexturedSkybox extends AbstractSkybox implements TextureRegistrar {
    private final Rotation rotation;
    private final Blend blend;

    public static final Function<BlendFunction, RenderPipeline> TEXTURED_SKYBOX_PIPELINE_CONSUMER = (blendFunction) -> {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesAccessor.getMatricesColorSnippet());
        builder.withLocation(NuitClient.withId("pipeline/textured_skybox"));
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
     * @param skyRendererAccess Access to the skyRenderer as skyboxes often require it.
     * @param poseStack         The current PoseStack.
     * @param tickDelta         The current tick delta.
     */
    @Override
    public final void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        Vector4f colorModifier = this.blend.applyEquationAndGetColor(this.alpha);
        RenderSystem.setShaderColor(colorModifier.x, colorModifier.y, colorModifier.z, colorModifier.w);

        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        poseStack.pushPose();
        this.rotation.apply(poseStack, level);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(poseStack.last().pose());
        this.renderSkybox(skyRendererAccess, poseStack, tickDelta, camera, fogParameters);
        modelViewStack.popMatrix();
        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL46C.glBlendEquation(GL46C.GL_FUNC_ADD);
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, FogParameters fogParameters);
}
