package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public class Conditions {
    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Condition.create(ResourceLocation.CODEC).optionalFieldOf("biomes", Condition.of()).forGetter(Conditions::getBiomes),
            Condition.create(ResourceLocation.CODEC).optionalFieldOf("worlds", Condition.of()).forGetter(Conditions::getWorlds),
            Condition.create(ResourceLocation.CODEC).optionalFieldOf("dimensions", Condition.of()).forGetter(Conditions::getDimensions),
            Condition.create(ResourceLocation.CODEC).optionalFieldOf("effects", Condition.of()).forGetter(Conditions::getEffects),
            Condition.create(Weather.CODEC).optionalFieldOf("weathers", Condition.of()).forGetter(Conditions::getWeathers),
            Condition.create(MinMaxEntry.CODEC).optionalFieldOf("xRanges", Condition.of()).forGetter(Conditions::getXRanges),
            Condition.create(MinMaxEntry.CODEC).optionalFieldOf("yRanges", Condition.of()).forGetter(Conditions::getYRanges),
            Condition.create(MinMaxEntry.CODEC).optionalFieldOf("zRanges", Condition.of()).forGetter(Conditions::getZRanges)
    ).apply(instance, Conditions::new));

    private final Condition<ResourceLocation> biomes;
    private final Condition<ResourceLocation> worlds;
    private final Condition<ResourceLocation> dimensions;
    private final Condition<ResourceLocation> effects;
    private final Condition<Weather> weathers;
    private final Condition<MinMaxEntry> xRanges;
    private final Condition<MinMaxEntry> yRanges;
    private final Condition<MinMaxEntry> zRanges;

    public Conditions(Condition<ResourceLocation> biomes, Condition<ResourceLocation> worlds, Condition<ResourceLocation> dimensions, Condition<ResourceLocation> effects, Condition<Weather> weathers, Condition<MinMaxEntry> xRanges, Condition<MinMaxEntry> yRanges, Condition<MinMaxEntry> zRanges) {
        this.biomes = biomes;
        this.worlds = worlds;
        this.dimensions = dimensions;
        this.effects = effects;
        this.weathers = weathers;
        this.xRanges = xRanges;
        this.yRanges = yRanges;
        this.zRanges = zRanges;
    }

    public Condition<ResourceLocation> getBiomes() {
        return this.biomes;
    }

    public Condition<ResourceLocation> getWorlds() {
        return this.worlds;
    }

    public Condition<ResourceLocation> getDimensions() {
        return this.dimensions;
    }

    public Condition<ResourceLocation> getEffects() {
        return this.effects;
    }

    public Condition<Weather> getWeathers() {
        return this.weathers;
    }

    public Condition<MinMaxEntry> getXRanges() {
        return this.xRanges;
    }

    public Condition<MinMaxEntry> getYRanges() {
        return this.yRanges;
    }

    public Condition<MinMaxEntry> getZRanges() {
        return this.zRanges;
    }

    public static Conditions of() {
        return new Conditions(Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of(), Condition.of());
    }
}
