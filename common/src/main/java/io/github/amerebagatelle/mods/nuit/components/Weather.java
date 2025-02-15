package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;

import java.util.Arrays;
import java.util.Objects;

public enum Weather {
    CLEAR("clear"),
    RAIN("rain"),
    BIOME_RAIN("rain_biome"),
    SNOW("snow"),
    THUNDER("thunder");

    public static final Codec<Weather> CODEC = Codec.STRING.xmap(Weather::fromString, Weather::toString);
    private final String name;

    Weather(String name) {
        this.name = name;
    }

    public static Weather fromString(String name) {
        return Objects.requireNonNull(Arrays.stream(Weather.values()).filter(weather -> name.equals(weather.name)).findFirst().orElse(Weather.CLEAR));
    }

    @Override
    public String toString() {
        return this.name;
    }
}
