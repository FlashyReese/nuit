package me.flashyreese.mods.nuit.sound;

import me.flashyreese.mods.nuit.components.SoundSettings;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.jspecify.annotations.NonNull;

/**
 * A non-positional Ogg resource using Minecraft's Ambient/Environment volume control.
 */
public final class ResourceSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private boolean stopped;

    public ResourceSoundInstance(Identifier file) {
        this(new SoundSettings(file), 1.0F);
    }

    public ResourceSoundInstance(SoundSettings settings, float volume) {
        super(settings.file(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.setVolume(volume);
        this.pitch = 1.0F;
        this.looping = settings.loop();
        this.delay = 0;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.sound = new Sound(
                Sound.SOUND_LISTER.fileToId(settings.file()),
                ConstantFloat.of(1.0F),
                ConstantFloat.of(1.0F),
                1,
                Sound.Type.FILE,
                true,
                false,
                16
        );
        this.soundEvent = new WeighedSoundEvents(this.identifier, null);
        this.soundEvent.addSound(this.sound);
    }

    @Override
    public WeighedSoundEvents getOrResolve(@NonNull SoundManager soundManager) {
        return this.soundEvent;
    }

    public void setVolume(float volume) {
        this.volume = Float.isFinite(volume) ? Math.clamp(volume, 0.0F, 1.0F) : 0.0F;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    public void stopPlayback() {
        this.stopped = true;
        this.looping = false;
    }
}
