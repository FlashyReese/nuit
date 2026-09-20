package me.flashyreese.mods.nuit.components;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlendTest {
    @ParameterizedTest
    @ValueSource(strings = {"", "normal", "alpha", "add", "subtract", "multiply", "screen", "burn", "dodge", "replace", "disable", "decorations"})
    public void parsesAndEncodesNamedModes(String type) {
        JsonObject json = new JsonObject();
        json.addProperty("blend", type);

        Properties properties = Properties.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(type, properties.blend().getType());
        assertEquals(new JsonPrimitive(type), Blend.CODEC.encodeStart(JsonOps.INSTANCE, properties.blend()).getOrThrow());
    }

    @Test
    public void omittedBlendUsesNormal() {
        Properties properties = Properties.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals("normal", properties.blend().getType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"type\":\"add\"}", "{}", "12", "true", "[]"})
    public void rejectsNonStringBlendValues(String json) {
        JsonObject properties = new JsonObject();
        properties.add("blend", JsonParser.parseString(json));

        assertTrue(Properties.CODEC.parse(JsonOps.INSTANCE, properties).error().isPresent());
    }
}
