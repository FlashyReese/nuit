package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.util.CodecUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public record Properties(int layer, Fade fade, int transitionInDuration, int transitionOutDuration, Fog fog,
                         boolean renderSunSkyTint, Rotation rotation) {
    public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("layer", 0).forGetter(Properties::layer),
            Fade.CODEC.fieldOf("fade").forGetter(Properties::fade),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("transitionInDuration", 20).forGetter(Properties::transitionInDuration),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("transitionOutDuration", 20).forGetter(Properties::transitionOutDuration),
            Fog.CODEC.optionalFieldOf("fog", Fog.of()).forGetter(Properties::fog),
            Codec.BOOL.optionalFieldOf("sunSkyTint", true).forGetter(Properties::renderSunSkyTint),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.of()).forGetter(Properties::rotation)
    ).apply(instance, Properties::new));

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Properties of() {
        return new Properties(0, Fade.of(), 20, 20, Fog.of(), true, Rotation.of());
    }

    public static Properties decorations() {
        return new Properties(0, Fade.of(), 20, 20, Fog.of(), true, Rotation.decorations());
    }
}
