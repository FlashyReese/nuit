package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;

/**
 * Selects the alpha sources that control sound playback and whether they fade its volume.
 */
public enum SoundVolumeMode {
    FADE("fade", AlphaSource.FADE, VolumeResponse.FOLLOW_ALPHA),
    CONDITION("condition", AlphaSource.CONDITION, VolumeResponse.FOLLOW_ALPHA),
    FADE_AND_CONDITION("fade_and_condition", AlphaSource.BOTH, VolumeResponse.FOLLOW_ALPHA),
    FULL_DURING_FADE("full_during_fade", AlphaSource.FADE, VolumeResponse.FULL_WHEN_ACTIVE),
    FULL_DURING_CONDITION("full_during_condition", AlphaSource.CONDITION, VolumeResponse.FULL_WHEN_ACTIVE),
    FULL_DURING_FADE_AND_CONDITION("full_during_fade_and_condition", AlphaSource.BOTH,
            VolumeResponse.FULL_WHEN_ACTIVE);

    public static final Codec<SoundVolumeMode> CODEC =
            Codec.STRING.comapFlatMap(SoundVolumeMode::fromString, SoundVolumeMode::toString);

    private final String name;
    private final AlphaSource alphaSource;
    private final VolumeResponse volumeResponse;

    SoundVolumeMode(String name, AlphaSource alphaSource, VolumeResponse volumeResponse) {
        this.name = name;
        this.alphaSource = alphaSource;
        this.volumeResponse = volumeResponse;
    }

    public boolean usesFadeAlpha() {
        return switch (this.alphaSource) {
            case FADE, BOTH -> true;
            case CONDITION -> false;
        };
    }

    public boolean usesConditionAlpha() {
        return switch (this.alphaSource) {
            case CONDITION, BOTH -> true;
            case FADE -> false;
        };
    }

    public float calculate(float fadeAlpha, float conditionAlpha) {
        float alpha = this.alphaSource.calculate(fadeAlpha, conditionAlpha);
        return this.volumeResponse.apply(alpha);
    }

    private static float clampAlpha(float alpha) {
        return Float.isFinite(alpha) ? Math.clamp(alpha, 0.0F, 1.0F) : 0.0F;
    }

    private static DataResult<SoundVolumeMode> fromString(String name) {
        for (SoundVolumeMode mode : values()) {
            if (mode.name.equals(name)) {
                return DataResult.success(mode);
            }
        }
        return DataResult.error(() -> "Unknown sound volume mode '" + name + "'. Expected one of: " +
                Arrays.toString(values()));
    }

    @Override
    public String toString() {
        return this.name;
    }

    private enum AlphaSource {
        FADE,
        CONDITION,
        BOTH;

        private float calculate(float fadeAlpha, float conditionAlpha) {
            return switch (this) {
                case FADE -> clampAlpha(fadeAlpha);
                case CONDITION -> clampAlpha(conditionAlpha);
                case BOTH -> clampAlpha(fadeAlpha) * clampAlpha(conditionAlpha);
            };
        }
    }

    private enum VolumeResponse {
        FOLLOW_ALPHA,
        FULL_WHEN_ACTIVE;

        private float apply(float alpha) {
            return switch (this) {
                case FOLLOW_ALPHA -> alpha;
                case FULL_WHEN_ACTIVE -> alpha > 0.0F ? 1.0F : 0.0F;
            };
        }
    }
}
