package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.util.CodecUtils;

public class UVRange {
    public static final Codec<UVRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("minU", 0F).forGetter(UVRange::getMinU),
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("minV", 0F).forGetter(UVRange::getMinV),
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("maxU", 1.0F).forGetter(UVRange::getMaxU),
            CodecUtils.getClampedFloat(0F, 1.0F).optionalFieldOf("maxV", 1.0F).forGetter(UVRange::getMaxV)
    ).apply(instance, UVRange::new));

    private final float minU;
    private final float minV;
    private final float maxU;
    private final float maxV;

    public UVRange(float minU, float minV, float maxU, float maxV) {
        this.minU = minU;
        this.minV = minV;
        this.maxU = maxU;
        this.maxV = maxV;
    }

    public float getMinU() {
        return this.minU;
    }

    public float getMaxU() {
        return this.maxU;
    }

    public float getMinV() {
        return this.minV;
    }

    public float getMaxV() {
        return this.maxV;
    }

    public static UVRange of() {
        return new UVRange(0, 0, 1, 1);
    }
}

