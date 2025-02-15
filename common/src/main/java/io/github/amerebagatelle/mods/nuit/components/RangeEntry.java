package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public record RangeEntry(float min, float max) {
    public static final Codec<RangeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("min").forGetter(RangeEntry::min),
            Codec.FLOAT.fieldOf("max").forGetter(RangeEntry::max)
    ).apply(instance, RangeEntry::new));

    public RangeEntry {
        if (min > max) {
            throw new IllegalStateException("Maximum value is lower than the minimum value:\n" + this);
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
