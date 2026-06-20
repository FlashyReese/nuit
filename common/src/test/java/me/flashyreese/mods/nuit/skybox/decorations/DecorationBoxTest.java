package me.flashyreese.mods.nuit.skybox.decorations;

import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DecorationBoxTest {
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
                false,
                Blend.decorations()
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
                false,
                Blend.decorations()
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
                false,
                Blend.decorations()
        );

        Collection<Identifier> textures = decorationBox.getTexturesToRegister();
        assertEquals(8, textures.size());
        assertTrue(textures.contains(Identifier.withDefaultNamespace("textures/environment/celestial/moon/full_moon.png")));
        assertTrue(textures.contains(Identifier.withDefaultNamespace("textures/environment/celestial/moon/new_moon.png")));
    }
}
