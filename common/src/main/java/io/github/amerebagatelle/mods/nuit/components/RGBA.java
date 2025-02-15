package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.util.CodecUtils;

public class RGBA extends RGB {
    public static final Codec<RGBA> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedFloat(0.0F, 1.0F).fieldOf("red").forGetter(RGB::getRed),
            CodecUtils.getClampedFloat(0.0F, 1.0F).fieldOf("green").forGetter(RGB::getGreen),
            CodecUtils.getClampedFloat(0.0F, 1.0F).fieldOf("blue").forGetter(RGB::getBlue),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("alpha", 1.0F).forGetter(RGBA::getAlpha)
    ).apply(instance, RGBA::new));

    private final float alpha;

    public RGBA(float red, float green, float blue, float alpha) {
        super(red, green, blue);
        this.alpha = alpha;
    }

    public static RGBA of() {
        return new RGBA(0, 0, 0, 0);
    }

    public float getAlpha() {
        return this.alpha;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RGBA rgba) {
            return super.equals(obj) && this.alpha == rgba.alpha;
        } else {
            return super.equals(obj);
        }
    }
}