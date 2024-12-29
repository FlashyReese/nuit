package io.github.amerebagatelle.mods.nuit.skybox;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import io.github.amerebagatelle.mods.nuit.skybox.decorations.DecorationBox;
import io.github.amerebagatelle.mods.nuit.skybox.textured.MultiTexturedSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.textured.SquareTexturedSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.vanilla.EndSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.vanilla.OverworldSkybox;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class SkyboxType<T extends Skybox> {
    public static final Codec<ResourceLocation> SKYBOX_ID_CODEC;
    public static final ResourceKey<Registry<SkyboxType<? extends Skybox>>> SKYBOX_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.tryBuild(NuitClient.MOD_ID, "skybox_type"));

    public static final SkyboxType<OverworldSkybox> OVERWORLD;
    public static final SkyboxType<EndSkybox> END;

    public static final SkyboxType<MonoColorSkybox> MONO_COLOR_SKYBOX;
    public static final SkyboxType<SquareTexturedSkybox> SQUARE_TEXTURED_SKYBOX;
    public static final SkyboxType<MultiTexturedSkybox> MULTI_TEXTURED_SKYBOX;

    public static final SkyboxType<DecorationBox> DECORATION_BOX;

    static {
        SKYBOX_ID_CODEC = Codec.STRING.xmap((s) -> {
            if (!s.contains(":")) {
                return ResourceLocation.tryBuild(NuitClient.MOD_ID, s.replace('-', '_'));
            }
            return ResourceLocation.tryParse(s.replace('-', '_'));
        }, (id) -> {
            if (id.getNamespace().equals(NuitClient.MOD_ID)) {
                return id.getPath().replace('_', '-');
            }
            return id.toString().replace('_', '-');
        });
        
        OVERWORLD = new SkyboxType<>("overworld", 1, OverworldSkybox.CODEC);
        END = new SkyboxType<>("end", 1, EndSkybox.CODEC);

        MONO_COLOR_SKYBOX = new SkyboxType<>("monocolor", 1, MonoColorSkybox.CODEC);
        SQUARE_TEXTURED_SKYBOX = new SkyboxType<>("square-textured", 1, SquareTexturedSkybox.CODEC);
        MULTI_TEXTURED_SKYBOX = new SkyboxType<>("multi-textured", 1, MultiTexturedSkybox.CODEC);

        DECORATION_BOX = new SkyboxType<>("decorations", 1, DecorationBox.CODEC);
    }

    private final BiMap<Integer, Codec<T>> codecBiMap;
    private final String name;

    public SkyboxType(String name, int schemaVersion, Codec<T> codec) {
        this(ImmutableBiMap.<Integer, Codec<T>>builder().put(schemaVersion, codec).build(), name);
    }

    private SkyboxType(BiMap<Integer, Codec<T>> codecBiMap, String name) {
        this.codecBiMap = codecBiMap;
        this.name = name;
    }

    public static void register(Consumer<SkyboxType<?>> function) {
        function.accept(OVERWORLD);
        function.accept(END);
        function.accept(MONO_COLOR_SKYBOX);
        function.accept(SQUARE_TEXTURED_SKYBOX);
        function.accept(MULTI_TEXTURED_SKYBOX);
        function.accept(DECORATION_BOX);
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation createId() {
        return this.createIdFactory().apply(NuitClient.MOD_ID);
    }

    public Function<String, ResourceLocation> createIdFactory() {
        return (ns) -> ResourceLocation.tryBuild(ns, this.getName().replace('-', '_'));
    }

    public Codec<T> getCodec(int schemaVersion) {
        return Objects.requireNonNull(this.codecBiMap.get(schemaVersion), String.format("Unsupported schema version '%d' for skybox type %s", schemaVersion, this.name));
    }
}
