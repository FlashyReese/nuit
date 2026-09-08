package me.flashyreese.mods.nuit.sound;

import me.flashyreese.mods.nuit.components.SoundSettings;

import java.util.Objects;

/**
 * Controls one sound instance independently of Minecraft's audio device. Tick once per client world tick.
 */
public final class SoundPlayback {
    private static final int RETRY_INTERVAL_TICKS = 20;

    private final SoundSettings settings;
    private final Backend backend;
    private boolean enabled;
    private boolean triggered;
    private int delayRemaining;
    private int retryRemaining;

    public SoundPlayback(SoundSettings settings, Backend backend) {
        this.settings = Objects.requireNonNull(settings);
        this.backend = Objects.requireNonNull(backend);
    }

    /**
     * Playback is enabled by the skybox's fade and conditions. The caller applies the delay to fade
     * keyframes; without keyframes, startDelayTicks delays playback after it becomes enabled.
     * A fade-in may start at zero volume. An unclipped, non-looping sound may finish after playback
     * is disabled.
     */
    public void tick(boolean enabled, float fadeVolume, int startDelayTicks) {
        float volume = this.settings.clipToFade() ? clampVolume(fadeVolume) : 1.0F;
        this.backend.setVolume(volume);

        if (!enabled) {
            this.clearActivation();
            if (this.settings.loop() || this.settings.clipToFade()) {
                this.backend.stop();
            }
            return;
        }

        if (!this.enabled) {
            this.enabled = true;
            this.triggered = false;
            this.delayRemaining = Math.max(0, startDelayTicks);
        }
        if (this.retryRemaining > 0) {
            this.retryRemaining--;
        }
        boolean waitingForDelay = this.delayRemaining > 0;
        if (waitingForDelay) {
            this.delayRemaining--;
        }

        if (this.backend.isActive()) {
            // An unclipped sound may still be playing. Let it finish before starting another instance.
            this.triggered = true;
            return;
        }
        if (this.triggered && !this.settings.loop()) {
            return;
        }
        if (waitingForDelay || this.retryRemaining > 0) {
            return;
        }

        // Try non-looping sounds once, even if playback fails. Looping sounds can retry later.
        this.triggered = true;
        this.retryRemaining = RETRY_INTERVAL_TICKS;
        this.backend.play(this.settings, volume);
    }

    /**
     * Stops the sound, including unclipped playback, and cancels any pending delay.
     */
    public void stop() {
        this.backend.stop();
        this.clearActivation();
    }

    /**
     * Also clears resource failure state, allowing playback to retry after a resource reload.
     */
    public void reset() {
        this.backend.reset();
        this.clearActivation();
    }

    public boolean isActive() {
        return this.backend.isActive();
    }

    private void clearActivation() {
        this.enabled = false;
        this.triggered = false;
        this.delayRemaining = 0;
        this.retryRemaining = 0;
    }

    private static float clampVolume(float fadeVolume) {
        return Float.isFinite(fadeVolume) ? Math.clamp(fadeVolume, 0.0F, 1.0F) : 0.0F;
    }

    /**
     * A backend owns at most one sound instance. Volume excludes Minecraft's category and master sliders.
     */
    public interface Backend {
        boolean play(SoundSettings settings, float volume);

        void setVolume(float volume);

        boolean isActive();

        void stop();

        default void reset() {
            this.stop();
        }
    }
}
