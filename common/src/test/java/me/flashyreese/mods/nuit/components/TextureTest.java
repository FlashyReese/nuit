package me.flashyreese.mods.nuit.components;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TextureTest {
    @ParameterizedTest
    @ValueSource(strings = {
            """
                    "test:id"
                    """
    })
    public void testCorrectParse(String json) {
        var jsonOb = JsonTestHelper.GSON.fromJson(json, JsonPrimitive.class);
        assertDoesNotThrow(() -> Texture.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }
}
