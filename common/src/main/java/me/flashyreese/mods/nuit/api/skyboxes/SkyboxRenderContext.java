package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fStack;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

/**
 * Frame-local state and stable helper methods for skybox rendering.
 */
public final class SkyboxRenderContext {
    private final SkyboxRenderAccess skyboxRenderAccess;
    private final Matrix4fStack skyModelViewStack;
    private final float tickDelta;
    private final Camera camera;
    private final GpuBufferSlice fogParameters;
    private final Vector4fc fogColor;
    private float requestedSkyOccluderAlpha;

    @ApiStatus.Internal
    public SkyboxRenderContext(SkyboxRenderAccess skyboxRenderAccess, Matrix4fStack skyModelViewStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, Vector4fc fogColor) {
        this.skyboxRenderAccess = skyboxRenderAccess;
        this.skyModelViewStack = skyModelViewStack;
        this.tickDelta = tickDelta;
        this.camera = camera;
        this.fogParameters = fogParameters;
        this.fogColor = fogColor;
    }

    /**
     * @return the frame's sky model-view stack before skybox-specific transforms.
     */
    public Matrix4fStack skyModelViewStack() {
        return this.skyModelViewStack;
    }

    public float tickDelta() {
        return this.tickDelta;
    }

    public Camera camera() {
        return this.camera;
    }

    /**
     * @return the frame's fog color, which the sky target is cleared to and the sky occluder fades towards.
     */
    public Vector4fc fogColor() {
        return this.fogColor;
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
        this.renderSkyDisc(ARGB.vector3fFromRGB24(color));
    }

    /**
     * Draws the vanilla sky disc using a linear RGB color.
     */
    public void renderSkyDisc(Vector3fc color) {
        this.skyboxRenderAccess.renderSkyDisc(color);
    }

    /**
     * Draws the vanilla sky disc using a linear RGBA color.
     */
    public void renderSkyDisc(Vector4fc color) {
        this.skyboxRenderAccess.renderSkyDisc(color);
    }

    /**
     * Requests the vanilla sky occluder, which fades the sky below the horizon into the fog color. Its angles account
     * for the camera being below the world's minimum height or underwater.
     * <p>
     * The occluder is drawn once, after every active skybox, like vanilla draws it after the sun, moon and stars. Nuit
     * skyboxes request it through the {@code occludeBelowHorizon} property instead. Does nothing in dimensions without
     * a sky occluder.
     */
    public void renderSkyOccluder() {
        this.requestedSkyOccluderAlpha = 1.0F;
    }

    @ApiStatus.Internal
    public float requestedSkyOccluderAlpha() {
        return this.requestedSkyOccluderAlpha;
    }

    @ApiStatus.Internal
    public void renderRequestedSkyOccluder(float alpha) {
        this.skyboxRenderAccess.renderSkyOccluder(alpha);
    }

    /**
     * @deprecated vanilla replaced the below-horizon dark disc with the sky occluder; use {@link #renderSkyOccluder()}.
     */
    @Deprecated(forRemoval = true)
    public void renderDarkDisc() {
        this.renderSkyOccluder();
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
