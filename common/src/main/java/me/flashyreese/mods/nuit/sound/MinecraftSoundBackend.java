package me.flashyreese.mods.nuit.sound;

import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.components.SoundSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;

/**
 * Uses Minecraft's sound engine for resource loading, streaming, device management and pausing.
 */
public final class MinecraftSoundBackend implements SoundPlayback.Backend {
    private ResourceSoundInstance soundInstance;
    private boolean missingResource;

    @Override
    public boolean play(SoundSettings settings, float volume) {
        if (this.isActive()) {
            this.setVolume(volume);
            return true;
        }
        if (this.missingResource) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.getResourceManager().getResource(settings.file()).isEmpty()) {
            this.missingResource = true;
            NuitClient.getLogger().warn("Cannot play Nuit sound: resource {} was not found", settings.file());
            return false;
        }

        this.stop();
        ResourceSoundInstance sound = new ResourceSoundInstance(settings, volume);
        if (client.getSoundManager().play(sound) == SoundEngine.PlayResult.NOT_STARTED) {
            sound.stopPlayback();
            return false;
        }
        this.soundInstance = sound;
        return true;
    }

    @Override
    public void setVolume(float volume) {
        if (this.soundInstance != null) {
            this.soundInstance.setVolume(volume);
        }
    }

    @Override
    public boolean isActive() {
        return this.soundInstance != null && !this.soundInstance.isStopped() && Minecraft.getInstance().getSoundManager().isActive(this.soundInstance);
    }

    @Override
    public void stop() {
        if (this.soundInstance != null) {
            this.soundInstance.stopPlayback();
            Minecraft.getInstance().getSoundManager().stop(this.soundInstance);
            this.soundInstance = null;
        }
    }

    @Override
    public void reset() {
        this.stop();
        this.missingResource = false;
    }
}
