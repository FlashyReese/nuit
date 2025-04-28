package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RGBATest {
    @ParameterizedTest
    @ValueSource(strings = {
            """
                    {
                        "red": 0.0,
                        "blue": 1.0,
                        "green": 0.0
                    }
                    """,
            """
                    {
                        "red": 0.0,
                        "blue": 1.0,
                        "green": 0.0,
                        "alpha": 0.5
                    }
                    """
    })
    public void testCorrectParse(String json) {
        var jsonOb = JsonTestHelper.readJson(json);
        assertDoesNotThrow(() -> RGBA.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    {
                    }
                    """,
            """
                    {
                        "red": 0.0
                    }
                    """,
            """
                    {
                        "blue": 1.0
                    }
                    """,
            """
                    {
                        "blue": 0.0,
                        "green": 0.0
                    }
                    """,
            """
                    {
                        "alpha": 0.5
                    }
                    """
    })
    public void testIncorrectParse(String json) {
        var jsonOb = JsonTestHelper.readJson(json);
        assertThrows(IllegalStateException.class, () -> RGBA.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }
}
