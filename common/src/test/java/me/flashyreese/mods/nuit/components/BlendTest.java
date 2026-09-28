package me.flashyreese.mods.nuit.components;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlendTest {
    @ParameterizedTest
    @ValueSource(strings = {
            """
                    {
                    }
                    """,
            """
                    {
                        "blend": "add"
                    }
                    """,
            """
                    {
                        "blend": "subtract"
                    }
                    """,
            """
                    {
                        "blend": "multiply"
                    }
                    """,
            """
                    {
                        "blend": "screen"
                    }
                    """,
            """
                    {
                        "blend": "replace"
                    }
                    """,
            """
                    {
                        "blend": "normal"
                    }
                    """,
            """
                    {
                        "blend": "burn"
                    }
                    """,
            """
                    {
                        "blend": "dodge"
                    }
                    """,
            """
                    {
                        "blend": "disable"
                    }
                    """,
            """
                    {
                        "blend": "decorations"
                    }
                    """,
            """
                    {
                        "blend": "custom",
                        "blender": {
                        }
                    }
                    """,
            """
                    {
                        "blend": "custom",
                        "blender": {
                            "separateFunction": true
                        }
                    }
                    """,
            """
                    {
                        "blend": "custom",
                        "blender": {
                            "sourceFactor": 1
                        }
                    }
                    """,
            """
                    {
                        "blend": "custom",
                        "blender": {
                            "equation": 32775
                        }
                    }
                    """,
            """
                    {
                        "blend": "custom",
                        "blender": {
                            "alphaEnabled": false
                        }
                    }
                    """
    })
    public void testCorrectParse(String json) {
        var jsonOb = JsonTestHelper.readJson(json);
        assertDoesNotThrow(() -> Blend.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "alpha", "normal"})
    public void acceptsNormalAliases(String type) {
        Blend blend = Blend.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"type\":\"" + type + "\"}")
        ).getOrThrow();
        assertEquals(type, blend.getType());
    }

    @Test
    public void normalFactoryUsesCanonicalName() {
        assertEquals("normal", Blend.normal().getType());
    }

    @Test
    public void propertiesReadBlendFromPropertiesObject() {
        Properties properties = Properties.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"blend\":{\"type\":\"add\"}}")
        ).getOrThrow();

        assertEquals("add", properties.blend().getType());
    }
}
