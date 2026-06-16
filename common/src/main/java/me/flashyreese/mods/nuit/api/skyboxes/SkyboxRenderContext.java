package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fStack;

/**
 * Frame-local state and stable helper methods for skybox rendering.
 */
public final class SkyboxRenderContext {
    private final SkyboxRenderAccess skyboxRenderAccess;
    private final Matrix4fStack matrixStack;
    private final float tickDelta;
    private final Camera camera;
    private final GpuBufferSlice fogParameters;

    @ApiStatus.Internal
    public SkyboxRenderContext(SkyboxRenderAccess skyboxRenderAccess, Matrix4fStack matrixStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters) {
        this.skyboxRenderAccess = skyboxRenderAccess;
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
        this.camera = camera;
        this.fogParameters = fogParameters;
    }

    public Matrix4fStack matrixStack() {
        return this.matrixStack;
    }

    public float tickDelta() {
        return this.tickDelta;
    }

    public Camera camera() {
        return this.camera;
    }

    /**
     * Applies the current vanilla fog uniforms before drawing sky geometry.
     */
    public void applyFog() {
        RenderSystem.setShaderFog(this.fogParameters);
    }

    /**
     * Draws the vanilla sky disc.
     */
    public void renderSkyDisc(int color) {
        this.skyboxRenderAccess.renderSkyDisc(color);
    }

    /**
     * Draws the vanilla below-horizon dark disc.
     */
    public void renderDarkDisc() {
        this.skyboxRenderAccess.renderDarkDisc();
    }

    /**
     * Draws vanilla stars with the supplied transform.
     */
    public void renderStars(float brightness, PoseStack poseStack) {
        this.skyboxRenderAccess.renderStars(brightness, poseStack);
    }

    /**
     * Draws vanilla End flash with the supplied intensity and rotation.
     */
    public void renderEndFlash(float intensity, float xAngle, float yAngle) {
        this.skyboxRenderAccess.renderEndFlash(intensity, xAngle, yAngle);
    }

    /**
     * @return vanilla End sky texture.
     */
    public Identifier endSkyTexture() {
        return this.skyboxRenderAccess.endSkyTexture();
    }
}
