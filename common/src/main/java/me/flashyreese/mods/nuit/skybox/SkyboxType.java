package me.flashyreese.mods.nuit.skybox;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Codec;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import net.minecraft.resources.Identifier;

import java.util.Map;

@Deprecated(forRemoval = true)
public class SkyboxType<T extends Skybox> extends me.flashyreese.mods.nuit.api.skyboxes.SkyboxType<T> {
    public SkyboxType(Identifier name, int schemaVersion, Codec<T> codec) {
        super(name, schemaVersion, codec);
    }

    public SkyboxType(BiMap<Integer, Codec<T>> codecBiMap, Identifier name) {
        super(name, codecBiMap);
    }

    public SkyboxType(Identifier name, Map<Integer, Codec<T>> codecs) {
        super(name, codecs);
    }
}
