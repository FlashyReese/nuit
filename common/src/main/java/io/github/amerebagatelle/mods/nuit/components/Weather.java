package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;

import java.util.Arrays;
import java.util.Objects;

public enum Weather {
    NO_PRECIPITATION("clear"),
    WORLD_PRECIPITATION("rain"),
    WORLD_THUNDERSTORM("thunder"),
    RAIN_IN_BIOME("rain_biome"),
    THUNDER_IN_RAIN_BIOME("rain_thunder"),
    SNOW_IN_BIOME("snow"),
    THUNDER_IN_SNOW_BIOME("snow_thunder");

    public static final Codec<Weather> CODEC = Codec.STRING.xmap(Weather::fromString, Weather::toString);
    private final String name;

    Weather(String name) {
        this.name = name;
    }

    public static Weather fromString(String name) {
        return Objects.requireNonNull(Arrays.stream(Weather.values()).filter(weather -> name.equals(weather.name)).findFirst().orElse(Weather.NO_PRECIPITATION));
    }

    @Override
    public String toString() {
        return this.name;
    }
}
