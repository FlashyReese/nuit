package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.minecraft.resources.Identifier;

public record Metadata(int schemaVersion, Identifier type) {
    public static final Codec<Metadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schemaVersion").forGetter(Metadata::schemaVersion),
            SkyboxType.SKYBOX_ID_CODEC.fieldOf("type").forGetter(Metadata::type)
    ).apply(instance, Metadata::new));
}

