package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.util.CodecUtils;

public record UVRange(float minU, float minV, float maxU, float maxV) {
    public static final Codec<UVRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("minU", 0F).forGetter(UVRange::minU),
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("minV", 0F).forGetter(UVRange::minV),
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("maxU", 1.0F).forGetter(UVRange::maxU),
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("maxV", 1.0F).forGetter(UVRange::maxV)
    ).apply(instance, UVRange::new));

    public static UVRange of() {
        return new UVRange(0, 0, 1, 1);
    }
}