package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public class Conditions {
    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Condition.create(Identifier.CODEC).optionalFieldOf("biomes", Condition.of()).forGetter(Conditions::getBiomes),
            Condition.create(Identifier.CODEC).optionalFieldOf("worlds", Condition.of()).forGetter(Conditions::getWorlds),
            Condition.create(Identifier.CODEC).optionalFieldOf("dimensions", Condition.of()).forGetter(Conditions::getDimensions),
            Condition.create(Identifier.CODEC).optionalFieldOf("effects", Condition.of()).forGetter(Conditions::getEffects),
            Condition.create(Weather.CODEC).optionalFieldOf("weather", Condition.of()).forGetter(Conditions::getWeathers),
            Condition.create(RangeEntry.CODEC).optionalFieldOf("xRanges", Condition.of()).forGetter(Conditions::getXRanges),
            Condition.create(RangeEntry.CODEC).optionalFieldOf("yRanges", Condition.of()).forGetter(Conditions::getYRanges),
            Condition.create(RangeEntry.CODEC).optionalFieldOf("zRanges", Condition.of()).forGetter(Conditions::getZRanges)
    ).apply(instance, Conditions::new));

    private final Condition<Identifier> biomes;
    private final Condition<Identifier> worlds;
    private final Condition<Identifier> dimensions;
    private final Condition<Identifier> effects;
    private final Condition<Weather> weathers;
    private final Condition<RangeEntry> xRanges;
    private final Condition<RangeEntry> yRanges;
    private final Condition<RangeEntry> zRanges;

    public Conditions(Condition<Identifier> biomes, Condition<Identifier> worlds, Condition<Identifier> dimensions, Condition<Identifier> effects, Condition<Weather> weathers, Condition<RangeEntry> xRanges, Condition<RangeEntry> yRanges, Condition<RangeEntry> zRanges) {
        this.biomes = biomes;
        this.worlds = worlds;
        this.dimensions = dimensions;
        this.effects = effects;
        this.weathers = weathers;
        this.xRanges = xRanges;
        this.yRanges = yRanges;
        this.zRanges = zRanges;
    }

    public static Conditions of() {
        return new Conditions(Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of());
    }

    public Condition<Identifier> getBiomes() {
        return this.biomes;
    }

    public Condition<Identifier> getWorlds() {
        return this.worlds;
    }

    public Condition<Identifier> getDimensions() {
        return this.dimensions;
    }

    public Condition<Identifier> getEffects() {
        return this.effects;
    }

    public Condition<Weather> getWeathers() {
        return this.weathers;
    }

    public Condition<RangeEntry> getXRanges() {
        return this.xRanges;
    }

    public Condition<RangeEntry> getYRanges() {
        return this.yRanges;
    }

    public Condition<RangeEntry> getZRanges() {
        return this.zRanges;
    }
}
