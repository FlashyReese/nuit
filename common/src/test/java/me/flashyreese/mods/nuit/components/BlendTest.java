package me.flashyreese.mods.nuit.components;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
        //assertDoesNotThrow(() -> Blend.CODEC.decode(JsonOps.INSTANCE, jsonOb).getOrThrow().getFirst());
    }
}
