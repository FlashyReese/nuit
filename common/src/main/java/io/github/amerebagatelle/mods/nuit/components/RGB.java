package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.util.CodecUtils;

public class RGB {
    public static final Codec<RGB> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedFloat(0.0F, 1.0F).fieldOf("red").forGetter(RGB::getRed),
            CodecUtils.getClampedFloat(0.0F, 1.0F).fieldOf("green").forGetter(RGB::getGreen),
            CodecUtils.getClampedFloat(0.0F, 1.0F).fieldOf("blue").forGetter(RGB::getBlue)
    ).apply(instance, RGB::new));

    private final float red;
    private final float green;
    private final float blue;

    public RGB(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public float getRed() {
        return this.red;
    }

    public float getBlue() {
        return this.blue;
    }

    public float getGreen() {
        return this.green;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RGB rgb) {
            return this.red == rgb.red && this.green == rgb.green && this.blue == rgb.blue;
        }
        return super.equals(obj);
    }
}
