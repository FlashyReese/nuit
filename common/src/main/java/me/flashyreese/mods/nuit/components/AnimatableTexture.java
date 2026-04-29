package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import me.flashyreese.mods.nuit.util.CodecUtils;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;

import java.util.Map;

public class AnimatableTexture {
    public static final Codec<AnimatableTexture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Texture.CODEC.fieldOf("texture").forGetter(AnimatableTexture::getTexture),
            UVRange.CODEC.optionalFieldOf("uvRange", UVRange.of()).forGetter(AnimatableTexture::getUvRange),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("gridColumns", 1).forGetter(AnimatableTexture::getGridColumns),
            CodecUtils.getClampedInteger(1, Integer.MAX_VALUE).optionalFieldOf("gridRows", 1).forGetter(AnimatableTexture::getGridRows),
            CodecUtils.getClampedLong(1, Integer.MAX_VALUE).optionalFieldOf("duration", 24000L).forGetter(AnimatableTexture::getDuration),
            Codec.BOOL.optionalFieldOf("interpolate", true).forGetter(AnimatableTexture::isInterpolate),
            CodecUtils.unboundedMapFixed(Integer.class, Codec.LONG, Int2LongArrayMap::new).optionalFieldOf("frameDuration", CodecUtils.fastUtilInt2LongArrayMap()).forGetter(AnimatableTexture::getFrameDuration)
    ).apply(instance, AnimatableTexture::new));

    private final Texture texture;
    private final UVRange uvRange;
    private final int gridRows;
    private final int gridColumns;
    private final long duration;
    private final boolean interpolate;
    private final Map<Integer, Long> frameDuration;

    private UVRange currentFrame;
    private int index;
    private long nextTime;

    public AnimatableTexture(Texture texture, UVRange uvRange, int gridColumns, int gridRows, long duration, boolean interpolate, Map<Integer, Long> frameDuration) {
        this.texture = texture;
        this.uvRange = uvRange;
        this.gridColumns = gridColumns;
        this.gridRows = gridRows;
        this.duration = duration;
        this.interpolate = interpolate;
        this.frameDuration = frameDuration;
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

    public void tick() {
        if (this.nextTime <= Util.getEpochMillis() && !Minecraft.getInstance().isPaused()) {
            // Current Frame
            this.index = (this.index + 1) % (this.gridRows * this.gridColumns);
            this.currentFrame = this.calculateNextFrameUVRange(this.index);
            this.nextTime = Util.getEpochMillis() + this.frameDuration.getOrDefault(this.index + 1, this.duration);
        }
    }

    public UVRange getCurrentFrame() {
        return this.currentFrame;
    }

    private UVRange calculateNextFrameUVRange(int nextFrameIndex) {
        float frameWidth = 1.0F / this.gridColumns;
        float frameHeight = 1.0F / this.gridRows;
        float minU = (float) (nextFrameIndex / this.gridRows) * frameWidth;
        float maxU = minU + frameWidth;
        float minV = (float) (nextFrameIndex % this.gridRows) * frameHeight;
        float maxV = minV + frameHeight;
        return new UVRange(minU, minV, maxU, maxV);
    }
}
