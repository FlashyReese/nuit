package me.flashyreese.mods.nuit;

import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.clock.ClockSource;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Fade;
import me.flashyreese.mods.nuit.components.Fog;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.decorations.DecorationBox;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SkyboxManagerTest {
    private static final ResourceLocation SUN = ResourceLocation.fromNamespaceAndPath("minecraft", "sun.png");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("minecraft", "moon.png");

    @Test
    public void onlyRotatingSunDecorationsControlGlobalCelestialEffects() {
        assertTrue(SkyboxManager.celestialControllerFor(decoration(true, false, 1.0F)).isPresent());
        assertFalse(SkyboxManager.celestialControllerFor(decoration(false, true, 1.0F)).isPresent());
        assertFalse(SkyboxManager.celestialControllerFor(decoration(true, false, 0.0F)).isPresent());
    }

    @Test
    public void replacesFindsAndRemovesSkyboxesWithoutExposingMutableViews() {
        SkyboxManager manager = new SkyboxManager();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "sky");
        Skybox first = new TestSkybox();
        Skybox second = new TestSkybox();

        manager.addSkybox(id, first);
        assertSame(first, manager.getSkybox(id).orElseThrow());
        manager.addSkybox(id, second);
        assertSame(second, manager.getSkybox(id).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> manager.getSkyboxes().clear());

        assertTrue(manager.removeSkybox(id));
        assertFalse(manager.removeSkybox(id));
        assertTrue(manager.getSkybox(id).isEmpty());

        manager.addPermanentSkybox(id, first);
        assertSame(first, manager.getSkybox(id).orElseThrow());
        assertTrue(manager.removePermanentSkybox(id));
        assertEquals(0, manager.getActiveSkyboxes().size());
        assertThrows(UnsupportedOperationException.class, () -> manager.getActiveSkyboxes().add(first));
    }

    private static DecorationBox decoration(boolean showSun, boolean showMoon, float speed) {
        Rotation rotation = new Rotation(
                true,
                Map.of(),
                Map.of(0L, new Quaternionf()),
                24000L,
                speed
        );
        Properties properties = new Properties(
                0,
                ClockSource.defaultClock(),
                Fade.of(),
                20,
                20,
                Fog.of(),
                true,
                true,
                rotation,
                Blend.decorations()
        );
        return new DecorationBox(
                properties,
                Conditions.of(),
                SUN,
                MOON,
                showSun,
                showMoon,
                false
        );
    }

    private static final class TestSkybox implements Skybox {
        @Override
        public void render(
                SkyRendererAccessor skyRendererAccessor,
                PoseStack poseStack,
                Matrix4f projectionMatrix,
                float tickDelta,
                Camera camera,
                boolean thickFog,
                Runnable fogCallback
        ) {
        }

        @Override
        public void tick(ClientLevel clientLevel) {
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
