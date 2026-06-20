package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public class Conditions {
    private static final Map<Identifier, Identifier> LEGACY_WORLD_TO_SKYBOX = Map.of(
            Identifier.withDefaultNamespace("overworld"), Identifier.withDefaultNamespace("overworld"),
            Identifier.withDefaultNamespace("the_nether"), Identifier.withDefaultNamespace("none"),
            Identifier.withDefaultNamespace("nether"), Identifier.withDefaultNamespace("none"),
            Identifier.withDefaultNamespace("the_end"), Identifier.withDefaultNamespace("end")
    );
    private static final Codec<Condition<Identifier>> SKYBOX_CONDITION_CODEC = Condition.create(Identifier.CODEC);
    private static final Codec<Condition<Identifier>> LEGACY_WORLD_CONDITION_CODEC = SKYBOX_CONDITION_CODEC.xmap(Conditions::normalizeLegacyWorldCondition, condition -> Condition.of());

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Condition.create(Identifier.CODEC).optionalFieldOf("biomes", Condition.of()).forGetter(Conditions::getBiomes),
            SKYBOX_CONDITION_CODEC.optionalFieldOf("skyboxes", Condition.of()).forGetter(Conditions::getSkyboxes),
            LEGACY_WORLD_CONDITION_CODEC.optionalFieldOf("worlds", Condition.of()).forGetter(conditions -> Condition.of()),
            Condition.create(Identifier.CODEC).optionalFieldOf("dimensions", Condition.of()).forGetter(Conditions::getDimensions),
            Condition.create(Identifier.CODEC).optionalFieldOf("effects", Condition.of()).forGetter(Conditions::getEffects),
            Condition.create(Weather.CODEC).optionalFieldOf("weather", Condition.of()).forGetter(Conditions::getWeathers),
            Condition.create(RangeEntry.CODEC).optionalFieldOf("xRanges", Condition.of()).forGetter(Conditions::getXRanges),
            Condition.create(RangeEntry.CODEC).optionalFieldOf("yRanges", Condition.of()).forGetter(Conditions::getYRanges),
            Condition.create(RangeEntry.CODEC).optionalFieldOf("zRanges", Condition.of()).forGetter(Conditions::getZRanges)
    ).apply(instance, Conditions::create));

    private final Condition<Identifier> biomes;
    private final Condition<Identifier> skyboxes;
    private final Condition<Identifier> dimensions;
    private final Condition<Identifier> effects;
    private final Condition<Weather> weathers;
    private final Condition<RangeEntry> xRanges;
    private final Condition<RangeEntry> yRanges;
    private final Condition<RangeEntry> zRanges;

    public Conditions(Condition<Identifier> biomes, Condition<Identifier> skyboxes, Condition<Identifier> dimensions, Condition<Identifier> effects, Condition<Weather> weathers, Condition<RangeEntry> xRanges, Condition<RangeEntry> yRanges, Condition<RangeEntry> zRanges) {
        this.biomes = biomes;
        this.skyboxes = skyboxes;
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

    public Condition<Identifier> getSkyboxes() {
        return this.skyboxes;
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

    private static Conditions create(Condition<Identifier> biomes, Condition<Identifier> skyboxes, Condition<Identifier> legacyWorlds, Condition<Identifier> dimensions, Condition<Identifier> effects, Condition<Weather> weathers, Condition<RangeEntry> xRanges, Condition<RangeEntry> yRanges, Condition<RangeEntry> zRanges) {
        Condition<Identifier> resolvedSkyboxes = skyboxes.entries().isEmpty() ? legacyWorlds : skyboxes;
        return new Conditions(biomes, resolvedSkyboxes, dimensions, effects, weathers, xRanges, yRanges, zRanges);
    }

    private static Condition<Identifier> normalizeLegacyWorldCondition(Condition<Identifier> condition) {
        List<Identifier> entries = new ObjectArrayList<>(condition.entries().size());
        for (Identifier entry : condition.entries()) {
            entries.add(LEGACY_WORLD_TO_SKYBOX.getOrDefault(entry, entry));
        }
        return new Condition<>(condition.excludes(), entries);
    }
}
