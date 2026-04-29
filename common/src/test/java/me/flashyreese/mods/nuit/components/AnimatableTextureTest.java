package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnimatableTextureTest {
    @ParameterizedTest
    @ValueSource(strings = {
            """
                    {
                        "texture": "test:identifier"
                    }
                    """,
            """
                    {
                        "texture": "test:identifier",
                        "gridColumns": 1
                    }
                    """,
            """
                    {
                        "texture": "test:identifier",
                        "gridRows": 1
                    }
                    """,
            """
                    {
                        "texture": "test:identifier",
                        "duration": 1
                    }
                    """,
            """
                    {
                        "texture": "test:identifier",
                        "interpolate": true
                    }
                    """,
            """
                    {
                        "texture": "test:identifier",
                        "frameDuration": {
                            "1": 10,
                            "2": 20
                        }
                    }
                    """
    })
    public void testCorrectParse(String json) {
        var jsonOb = JsonTestHelper.readJson(json);
        assertDoesNotThrow(() -> AnimatableTexture.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    {
                    }
                    """,
    })
    public void testIncorrectParse(String json) {
        var jsonOb = JsonTestHelper.readJson(json);
        assertThrows(IllegalStateException.class, () -> AnimatableTexture.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }
}
