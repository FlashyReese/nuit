package me.flashyreese.mods.nuit.render;

import me.flashyreese.mods.nuit.api.skyboxes.RenderableSkybox;
import net.minecraft.world.level.MoonPhase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class NuitSkyRenderState {
    public float partialTick;
    public long gameTime;
    public float sunAngle;
    public float skyAngle;
    public final Vector3f skyColor = new Vector3f();
    public final Vector4f sunriseAndSunsetColor = new Vector4f();
    public MoonPhase moonPhase = MoonPhase.FULL_MOON;
    public float starBrightness;
    public @Nullable EndFlash endFlash;
    public final List<ExtractedSkybox> skyboxes = new ArrayList<>();

    public record EndFlash(float intensity, float xAngle, float yAngle) {
    }

    /**
     * A skybox as it was when the frame was extracted.
     *
     * @param alpha    its alpha; {@code 1} for skyboxes without one
     * @param rotation its rotation from {@code properties.rotation}, or {@code null} for skyboxes without properties
     */
    public record ExtractedSkybox(RenderableSkybox skybox, float alpha, @Nullable Quaternionfc rotation) {
    }
}
