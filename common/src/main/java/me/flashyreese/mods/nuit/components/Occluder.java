package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.util.CodecUtils;

import java.util.Optional;

public record Occluder(boolean enabled, Optional<RGB> color, float opacity) {
    public static final Codec<Occluder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Occluder::enabled),
            RGB.CODEC.optionalFieldOf("color").forGetter(Occluder::color),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("opacity", 1.0F).forGetter(Occluder::opacity)
    ).apply(instance, Occluder::new));

    public static Occluder of() {
        return new Occluder(true, Optional.empty(), 1.0F);
    }

    public static Occluder disabled() {
        return new Occluder(false, Optional.empty(), 1.0F);
    }
}
