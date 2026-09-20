package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record SoundSettings(Identifier file, SoundVolumeMode volumeMode, boolean loop, int delay) {
    public static final Codec<SoundSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.validate(SoundSettings::validateFile).fieldOf("file").forGetter(SoundSettings::file),
            SoundVolumeMode.CODEC.optionalFieldOf("volumeMode", SoundVolumeMode.FADE_AND_CONDITION)
                    .forGetter(SoundSettings::volumeMode),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(SoundSettings::loop),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("delay", 0).forGetter(SoundSettings::delay)
    ).apply(instance, SoundSettings::new));

    public SoundSettings {
        validateFile(file).getOrThrow();
        Objects.requireNonNull(volumeMode);
        if (delay < 0) {
            throw new IllegalArgumentException("Sound delay must be nonnegative");
        }
    }

    public SoundSettings(Identifier file) {
        this(file, SoundVolumeMode.FADE_AND_CONDITION, false, 0);
    }

    private static DataResult<Identifier> validateFile(Identifier file) {
        if (file != null && file.getPath().startsWith("sounds/") && file.getPath().endsWith(".ogg") &&
                file.getPath().length() > "sounds/.ogg".length()) {
            return DataResult.success(file);
        } else {
            return DataResult.error(() -> "Sound file must be a namespaced sounds/*.ogg resource: " + file);
        }
    }
}
