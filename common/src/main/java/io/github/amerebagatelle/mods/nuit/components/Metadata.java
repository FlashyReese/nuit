package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.skybox.SkyboxType;
import net.minecraft.resources.ResourceLocation;

public record Metadata(int schemaVersion, ResourceLocation type) {
    public static final Codec<Metadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schemaVersion").forGetter(Metadata::schemaVersion),
            SkyboxType.SKYBOX_ID_CODEC.fieldOf("type").forGetter(Metadata::type)
    ).apply(instance, Metadata::new));
}

