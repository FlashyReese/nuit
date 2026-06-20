package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import me.flashyreese.mods.nuit.util.CodecUtils;
import net.minecraft.util.Mth;

import java.util.Map;

public class AnimatableTexture {
    public static final Codec<AnimatableTexture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Texture.CODEC.fieldOf("texture").forGetter(AnimatableTexture::getTexture),
            UVRange.CODEC.optionalFieldOf("uvRange", UVRange.of()).forGetter(AnimatableTexture::getUvRange),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("gridColumns", 1).forGetter(AnimatableTexture::getGridColumns),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("gridRows", 1).forGetter(AnimatableTexture::getGridRows),
            CodecUtils.getClampedLong(1, Integer.MAX_VALUE).optionalFieldOf("duration", 24000L).forGetter(AnimatableTexture::getDuration),
            Codec.BOOL.optionalFieldOf("interpolate", false).forGetter(AnimatableTexture::isInterpolate),
            CodecUtils.unboundedMapFixed(Integer.class, Codec.LONG, Int2LongArrayMap::new).optionalFieldOf("frameDuration", CodecUtils.fastUtilInt2LongArrayMap()).forGetter(AnimatableTexture::getFrameDuration)
    ).apply(instance, AnimatableTexture::new));

    private final Texture texture;
    private final UVRange uvRange;
    private final int gridRows;
    private final int gridColumns;
    private final long duration;
    private final boolean interpolate;
    private final Map<Integer, Long> frameDuration;
    private final int frameCount;
    private final long animationDuration;

    private UVRange currentFrame;
    private UVRange nextFrame;
    private int index;
    private float frameBlend;

    public AnimatableTexture(Texture texture, UVRange uvRange, int gridColumns, int gridRows, long duration, boolean interpolate, Map<Integer, Long> frameDuration) {
        this.texture = texture;
        this.uvRange = uvRange;
        this.gridColumns = gridColumns;
        this.gridRows = gridRows;
        this.duration = duration;
        this.interpolate = interpolate;
        this.frameDuration = frameDuration;
        this.frameCount = calculateFrameCount(gridColumns, gridRows);
        this.animationDuration = calculateAnimationDurationMillis(this.frameCount, duration, frameDuration);
    }

    public Texture getTexture() {
        return this.texture;
    }

    public UVRange getUvRange() {
        return this.uvRange;
    }

    public int getGridColumns() {
        return this.gridColumns;
    }

    public int getGridRows() {
        return this.gridRows;
    }

    public long getDuration() {
        return this.duration;
    }

    public boolean isInterpolate() {
        return this.interpolate;
    }

    public Map<Integer, Long> getFrameDuration() {
        return this.frameDuration;
    }

    public void update(long gameTime, float tickDelta) {
        if (this.frameCount == 1) {
            this.setCurrentFrame(0, 0.0F);
            return;
        }

        if (this.animationDuration <= 0L) {
            this.setCurrentFrame(0, 0.0F);
            return;
        }

        // Durations use vanilla 20 TPS tick units on purpose. Do not use the dynamic tick-rate
        // manager here unless sky animations should ignore game-speed changes.
        double animationTime = ((double) gameTime + tickDelta) * 50.0D;
        double cycleTime = Mth.positiveModulo(animationTime, this.animationDuration);
        long frameStart = 0L;
        for (int frameIndex = 0; frameIndex < this.frameCount; frameIndex++) {
            long frameDuration = this.getFrameDurationMillis(frameIndex);
            long frameEnd = saturatingAdd(frameStart, frameDuration);
            if (cycleTime < frameEnd || frameIndex == this.frameCount - 1) {
                float frameBlend = this.interpolate ? (float) Mth.clamp((cycleTime - frameStart) / (double) frameDuration, 0.0D, 1.0D) : 0.0F;
                this.setCurrentFrame(frameIndex, frameBlend);
                return;
            }
            frameStart = frameEnd;
        }
    }

    public UVRange getCurrentFrame() {
        return this.currentFrame;
    }

    public UVRange getNextFrame() {
        return this.nextFrame;
    }

    public float getFrameBlend() {
        return this.interpolate ? this.frameBlend : 0.0F;
    }

    public boolean hasMultipleFrames() {
        return this.frameCount > 1;
    }

    private void setCurrentFrame(int frameIndex, float frameBlend) {
        if (this.currentFrame == null || this.index != frameIndex) {
            this.index = frameIndex;
            this.currentFrame = this.calculateFrameUVRange(frameIndex);
            this.nextFrame = this.calculateFrameUVRange(this.getNextFrameIndex());
        }
        this.frameBlend = frameBlend;
    }

    private int getNextFrameIndex() {
        return (this.index + 1) % this.frameCount;
    }

    private static int calculateFrameCount(int gridColumns, int gridRows) {
        return (int) Mth.clamp((long) gridRows * gridColumns, 1L, Integer.MAX_VALUE);
    }

    private long getFrameDurationMillis(int frameIndex) {
        return Math.max(1L, this.frameDuration.getOrDefault(frameIndex + 1, this.duration));
    }

    private static long calculateAnimationDurationMillis(int frameCount, long defaultDuration, Map<Integer, Long> frameDuration) {
        long sanitizedDefaultDuration = Math.max(1L, defaultDuration);
        long totalDuration = saturatingMultiply(frameCount, sanitizedDefaultDuration);
        for (Map.Entry<Integer, Long> entry : frameDuration.entrySet()) {
            int frameNumber = entry.getKey();
            if (frameNumber < 1 || frameNumber > frameCount) {
                continue;
            }

            long overrideDuration = Math.max(1L, entry.getValue());
            if (overrideDuration >= sanitizedDefaultDuration) {
                totalDuration = saturatingAdd(totalDuration, overrideDuration - sanitizedDefaultDuration);
            } else {
                totalDuration -= sanitizedDefaultDuration - overrideDuration;
            }
        }
        return totalDuration;
    }

    private static long saturatingMultiply(int left, long right) {
        if (left <= 0 || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private UVRange calculateFrameUVRange(int nextFrameIndex) {
        float frameWidth = 1.0F / this.gridColumns;
        float frameHeight = 1.0F / this.gridRows;
        float minU = (float) (nextFrameIndex / this.gridRows) * frameWidth;
        float maxU = minU + frameWidth;
        float minV = (float) (nextFrameIndex % this.gridRows) * frameHeight;
        float maxV = minV + frameHeight;
        return new UVRange(minU, minV, maxU, maxV);
    }
}
