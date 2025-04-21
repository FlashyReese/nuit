package io.github.amerebagatelle.mods.nuit.skybox.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.components.Rotation;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.TextureRegistrar;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Objects;

public abstract class TexturedSkybox extends AbstractSkybox implements TextureRegistrar {
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
     * @param skyRendererAccess Access to the skyRenderer as skyboxes often require it.
     * @param poseStack         The current PoseStack.
     * @param tickDelta         The current tick delta.
     */
    @Override
    public final void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.depthMask(false);
        Utils.enableBlendingOverride();
        RenderSystem.enableBlend();
        this.blend.apply(this.alpha);

        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
        poseStack.pushPose();
        this.rotation.apply(poseStack, world);
        this.renderSkybox(skyRendererAccess, poseStack, tickDelta, camera, bufferSource, fogParameters);
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        Utils.disableBlendingOverride();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters);
}
