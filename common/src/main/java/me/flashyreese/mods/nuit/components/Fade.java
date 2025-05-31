package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import me.flashyreese.mods.nuit.util.CodecUtils;

import java.util.Map;

public record Fade(long duration, Map<Long, Float> keyFrames) {
    public static final Codec<Fade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedLong(1, Long.MAX_VALUE).optionalFieldOf("duration", 24000L).forGetter(Fade::duration),
            CodecUtils.unboundedMapFixed(Long.class, CodecUtils.getClampedFloat(0F, 1F), Long2FloatOpenHashMap::new)
                    .optionalFieldOf("keyFrames", CodecUtils.fastUtilLong2FloatOpenHashMap())
                    .forGetter(Fade::keyFrames)
    ).apply(instance, Fade::new));

    public Fade(long duration, Map<Long, Float> keyFrames) {
        this.duration = duration;
        this.keyFrames = keyFrames;
        this.validateKeyFrames();
    }

    public static Fade of() {
        return new Fade( 24000L, CodecUtils.fastUtilLong2FloatOpenHashMap());
    }

    private void validateKeyFrames() {
        // Validate that the keyframes are between 0 and the duration
        for (Long keyFrame : this.keyFrames.keySet()) {
            try {
                if (keyFrame < 0 || keyFrame >= this.duration) {
                    throw new IllegalArgumentException("Keyframes must be between 0 and duration");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Keyframes must be numeric", e);
            }
        }
    }
}