package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.render.NuitSkyRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.MoonPhase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

/**
 * Frame-local state and stable helper methods for skybox rendering.
 * <p>
 * Level values are extracted before rendering starts, so skyboxes should read them from here rather than from the
 * level, camera or player while rendering.
 */
public final class SkyboxRenderContext {
    private static final Quaternionfc IDENTITY_ROTATION = new Quaternionf();

    private final SkyboxRenderAccess skyboxRenderAccess;
    private final NuitSkyRenderState renderState;
    private final Matrix4fStack skyModelViewStack;
    private final GpuBufferSlice fogParameters;
    private final Vector4fc fogColor;
    private @Nullable NuitSkyRenderState.ExtractedSkybox currentSkybox;
    private float requestedSkyOccluderAlpha;

    @ApiStatus.Internal
    public SkyboxRenderContext(SkyboxRenderAccess skyboxRenderAccess, NuitSkyRenderState renderState, Matrix4fStack skyModelViewStack, GpuBufferSlice fogParameters, Vector4fc fogColor) {
        this.skyboxRenderAccess = skyboxRenderAccess;
        this.renderState = renderState;
        this.skyModelViewStack = skyModelViewStack;
        this.fogParameters = fogParameters;
        this.fogColor = fogColor;
    }

    @ApiStatus.Internal
    public void setCurrentSkybox(@Nullable NuitSkyRenderState.ExtractedSkybox currentSkybox) {
        this.currentSkybox = currentSkybox;
    }

    /**
     * @return the frame's sky model-view stack before skybox-specific transforms.
     */
    public Matrix4fStack skyModelViewStack() {
        return this.skyModelViewStack;
    }

    public float tickDelta() {
        return this.renderState.partialTick;
    }

    public long gameTime() {
        return this.renderState.gameTime;
    }

    public float sunAngle() {
        return this.renderState.sunAngle;
    }

    public float skyAngle() {
        return this.renderState.skyAngle;
    }

    public Vector3fc skyColor() {
        return this.renderState.skyColor;
    }

    public Vector4fc sunriseAndSunsetColor() {
        return this.renderState.sunriseAndSunsetColor;
    }

    public MoonPhase moonPhase() {
        return this.renderState.moonPhase;
    }

    public float starBrightness() {
        return this.renderState.starBrightness;
    }

    public float endFlashIntensity() {
        NuitSkyRenderState.EndFlash endFlash = this.renderState.endFlash;
        return endFlash != null ? endFlash.intensity() : 0.0F;
    }

    public float endFlashXAngle() {
        NuitSkyRenderState.EndFlash endFlash = this.renderState.endFlash;
        return endFlash != null ? endFlash.xAngle() : 0.0F;
    }

    public float endFlashYAngle() {
        NuitSkyRenderState.EndFlash endFlash = this.renderState.endFlash;
        return endFlash != null ? endFlash.yAngle() : 0.0F;
    }

    /**
     * @return the alpha of the skybox being rendered, as extracted for this frame; {@code 1} for skyboxes without one.
     */
    public float alpha() {
        return this.currentSkybox != null ? this.currentSkybox.alpha() : 1.0F;
    }

    /**
     * @return the rotation of the skybox being rendered from its {@code properties.rotation}, as extracted for this
     * frame; identity for skyboxes without properties.
     */
    public Quaternionfc rotation() {
        Quaternionfc rotation = this.currentSkybox != null ? this.currentSkybox.rotation() : null;
        return rotation != null ? rotation : IDENTITY_ROTATION;
    }

    /**
     * @return a copy of {@link #skyModelViewStack()} with {@link #rotation()} applied.
     */
    public Matrix4f rotatedModelViewMatrix() {
        return new Matrix4f(this.skyModelViewStack).rotate(this.rotation());
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
