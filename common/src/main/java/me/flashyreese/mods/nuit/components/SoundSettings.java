package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record SoundSettings(Identifier file, boolean clipToFade, boolean loop, int delay) {
    public static final Codec<SoundSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.validate(SoundSettings::validateFile).fieldOf("file").forGetter(SoundSettings::file),
            Codec.BOOL.optionalFieldOf("clipToFade", true).forGetter(SoundSettings::clipToFade),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(SoundSettings::loop),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("delay", 0).forGetter(SoundSettings::delay)
    ).apply(instance, SoundSettings::new));

    public SoundSettings {
        validateFile(file).getOrThrow();
        if (delay < 0) {
            throw new IllegalArgumentException("Sound delay must be nonnegative");
        }
    }

    public SoundSettings(Identifier file) {
        this(file, true, false, 0);
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
