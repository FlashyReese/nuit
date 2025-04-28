package me.flashyreese.mods.nuit.components;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WeatherTest {
    @ParameterizedTest
    @EnumSource(Weather.class)
    public void testCorrectParse(Weather weatherType) {
        var jsonOb = JsonTestHelper.GSON.fromJson(weatherType.toString(), JsonPrimitive.class);
        assertDoesNotThrow(() -> Weather.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }
}
