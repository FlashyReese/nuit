package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.clock.ClockSource;
import me.flashyreese.mods.nuit.util.CodecUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;

public record Properties(int layer, ClockSource clock, Fade fade, int transitionInDuration, int transitionOutDuration, Fog fog,
                         boolean renderSunSkyTint, boolean visibleUnderwater, Rotation rotation, Blend blend) {
    public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("layer", 0).forGetter(Properties::layer),
            ClockSource.CODEC.optionalFieldOf("clock", ClockSource.defaultClock()).forGetter(Properties::clock),
            Fade.CODEC.optionalFieldOf("fade", Fade.of()).forGetter(Properties::fade),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("transitionInDuration", 20).forGetter(Properties::transitionInDuration),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("transitionOutDuration", 20).forGetter(Properties::transitionOutDuration),
            Fog.CODEC.optionalFieldOf("fog", Fog.of()).forGetter(Properties::fog),
            Codec.BOOL.optionalFieldOf("sunSkyTint", true).forGetter(Properties::renderSunSkyTint),
            Codec.BOOL.optionalFieldOf("visibleUnderwater", true).forGetter(Properties::visibleUnderwater),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.of()).forGetter(Properties::rotation),
            Blend.CODEC.optionalFieldOf("blend", Blend.decorations()).forGetter(Properties::blend)
    ).apply(instance, Properties::new));

    public Properties(int layer, Fade fade, int transitionInDuration, int transitionOutDuration, Fog fog,
                      boolean renderSunSkyTint, boolean visibleUnderwater, Rotation rotation, Blend blend) {
        this(layer, ClockSource.defaultClock(), fade, transitionInDuration, transitionOutDuration, fog, renderSunSkyTint, visibleUnderwater, rotation, blend);
    }

    public static Properties of() {
        return new Properties(0, ClockSource.defaultClock(), Fade.of(), 20, 20, Fog.of(), true, true, Rotation.of(), Blend.normal());
    }

    public static Properties decorations() {
        return new Properties(0, ClockSource.defaultClock(), Fade.of(), 20, 20, Fog.of(), true, true, Rotation.decorations(), Blend.decorations());
    }

    @Override
    public @NotNull String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
