package me.flashyreese.mods.nuit.skybox.decorations;

import com.mojang.serialization.JsonOps;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.JsonTestHelper;
import me.flashyreese.mods.nuit.components.Properties;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DecorationBoxTest {
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"properties\":{}}", "{\"properties\":{\"layer\":1}}"})
    public void omittedBlendKeepsDecorationDefaults(String json) {
        DecorationBox skybox = DecorationBox.CODEC.parse(JsonOps.INSTANCE, JsonTestHelper.readJson(json)).getOrThrow();

        assertEquals("decorations", skybox.getProperties().blend().getType());
        assertEquals(Properties.decorations().rotation(), skybox.getProperties().rotation());
    }

    @Test
    public void explicitBlendPreservesDefaultRotation() {
        var json = JsonTestHelper.readJson("""
                {"properties":{"blend":"normal","layer":2}}
                """);
        DecorationBox skybox = DecorationBox.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals("normal", skybox.getProperties().blend().getType());
        assertEquals(2, skybox.getProperties().layer());
        assertEquals(Properties.decorations().rotation(), skybox.getProperties().rotation());

        var encoded = DecorationBox.CODEC.encodeStart(JsonOps.INSTANCE, skybox).getOrThrow().getAsJsonObject();
        assertEquals("normal", encoded.getAsJsonObject("properties").get("blend").getAsString());
        DecorationBox decoded = DecorationBox.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals("normal", decoded.getProperties().blend().getType());
        assertEquals(skybox.getProperties().rotation(), decoded.getProperties().rotation());
    }

    @Test
    public void explicitRotationOverridesDecorationDefault() {
        var json = JsonTestHelper.readJson("""
                {"properties":{"rotation":{"skyboxRotation":true}}}
                """);
        DecorationBox skybox = DecorationBox.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertTrue(skybox.getProperties().rotation().skyboxRotation());
        assertEquals("decorations", skybox.getProperties().blend().getType());
    }

    @Test
    public void disabledDecorationsDoNotRegisterTextures() {
        DecorationBox decorationBox = new DecorationBox(
                Properties.of(),
                Conditions.of(),
                Identifier.fromNamespaceAndPath("test", "textures/sky/sun.png"),
                Identifier.fromNamespaceAndPath("test", "textures/sky/moon.png"),
                false,
                false,
                false,
                false
        );

        assertTrue(decorationBox.getTexturesToRegister().isEmpty());
    }

    @Test
    public void enabledCustomSunAndMoonRegisterTextures() {
        Identifier sun = Identifier.fromNamespaceAndPath("test", "textures/sky/sun.png");
        Identifier moon = Identifier.fromNamespaceAndPath("test", "textures/sky/moon.png");
        DecorationBox decorationBox = new DecorationBox(
                Properties.of(),
                Conditions.of(),
                sun,
                moon,
                true,
                true,
                false,
                false
        );

        assertEquals(List.of(sun, moon), List.copyOf(decorationBox.getTexturesToRegister()));
    }

    @Test
    public void defaultMoonRegistersAllMoonPhaseTextures() {
        DecorationBox decorationBox = new DecorationBox(
                Properties.of(),
                Conditions.of(),
                Identifier.fromNamespaceAndPath("test", "textures/sky/sun.png"),
                Identifier.withDefaultNamespace("textures/environment/celestial/moon/full_moon.png"),
                false,
                true,
                false,
                false
        );

        Collection<Identifier> textures = decorationBox.getTexturesToRegister();
        assertEquals(8, textures.size());
        assertTrue(textures.contains(Identifier.withDefaultNamespace("textures/environment/celestial/moon/full_moon.png")));
        assertTrue(textures.contains(Identifier.withDefaultNamespace("textures/environment/celestial/moon/new_moon.png")));
    }
}
