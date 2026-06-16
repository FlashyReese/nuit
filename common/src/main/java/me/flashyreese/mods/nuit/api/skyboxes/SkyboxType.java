package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.serialization.Codec;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.skybox.MonoColorSkybox;
import me.flashyreese.mods.nuit.skybox.decorations.DecorationBox;
import me.flashyreese.mods.nuit.skybox.textured.MultiTexturedSkybox;
import me.flashyreese.mods.nuit.skybox.textured.SquareTexturedSkybox;
import me.flashyreese.mods.nuit.skybox.vanilla.EndSkybox;
import me.flashyreese.mods.nuit.skybox.vanilla.OverworldSkybox;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SkyboxType<T extends Skybox> {
    public static final Codec<Identifier> SKYBOX_ID_CODEC;
    public static final ResourceKey<Registry<SkyboxType<? extends Skybox>>> SKYBOX_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.tryBuild(NuitClient.MOD_ID, "skybox_type"));
    public static final SkyboxType<OverworldSkybox> OVERWORLD;
    public static final SkyboxType<EndSkybox> END;
    public static final SkyboxType<MonoColorSkybox> MONO_COLOR_SKYBOX;
    public static final SkyboxType<SquareTexturedSkybox> SQUARE_TEXTURED_SKYBOX;
    public static final SkyboxType<MultiTexturedSkybox> MULTI_TEXTURED_SKYBOX;
    public static final SkyboxType<DecorationBox> DECORATION_BOX;
    private static final Map<Identifier, SkyboxType<?>> SKYBOX_TYPES = new ConcurrentHashMap<>();

    static {
        SKYBOX_ID_CODEC = Codec.STRING.xmap((s) -> {
            if (!s.contains(":")) {
                return Identifier.tryBuild(NuitClient.MOD_ID, s);
            }
            return Identifier.tryParse(s);
        }, (id) -> {
            if (id.getNamespace().equals(NuitClient.MOD_ID)) {
                return id.getPath();
            }
            return id.toString();
        });

        OVERWORLD = register(new SkyboxType<>("overworld", 1, OverworldSkybox.CODEC));
        END = register(new SkyboxType<>("end", 1, EndSkybox.CODEC));

        MONO_COLOR_SKYBOX = register(new SkyboxType<>("monocolor", 1, MonoColorSkybox.CODEC));
        SQUARE_TEXTURED_SKYBOX = register(new SkyboxType<>("square-textured", 1, SquareTexturedSkybox.CODEC));
        MULTI_TEXTURED_SKYBOX = register(new SkyboxType<>("multi-textured", 1, MultiTexturedSkybox.CODEC));

        DECORATION_BOX = register(new SkyboxType<>("decorations", 1, DecorationBox.CODEC));
    }

    private final Map<Integer, Codec<T>> codecs;
    private final Identifier name;

    private SkyboxType(String name, int schemaVersion, Codec<T> codec) {
        this(Identifier.tryBuild(NuitClient.MOD_ID, name), schemaVersion, codec);
    }

    public SkyboxType(Identifier name, int schemaVersion, Codec<T> codec) {
        this(name, Map.of(schemaVersion, codec));
    }

    public SkyboxType(Identifier name, Map<Integer, Codec<T>> codecs) {
        this.codecs = Map.copyOf(codecs);
        this.name = name;
    }

    public static <T extends Skybox> SkyboxType<T> register(Identifier name, int schemaVersion, Codec<T> codec) {
        return register(new SkyboxType<>(name, schemaVersion, codec));
    }

    public static <T extends Skybox> SkyboxType<T> register(SkyboxType<T> type) {
        if (SKYBOX_TYPES.putIfAbsent(type.name, type) != null) {
            throw new IllegalStateException("SkyboxType with name '" + type.name + "' already registered!");
        }
        return type;
    }

    public static Optional<SkyboxType<?>> get(Identifier name) {
        return Optional.ofNullable(SKYBOX_TYPES.get(name));
    }

    public static Collection<SkyboxType<?>> values() {
        return Collections.unmodifiableCollection(SKYBOX_TYPES.values());
    }

    public static void registerAll(Consumer<SkyboxType<?>> function) {
        SKYBOX_TYPES.values().forEach(function);
    }

    public Identifier getName() {
        return this.name;
    }

    public Codec<T> getCodec(int schemaVersion) {
        return Objects.requireNonNull(this.codecs.get(schemaVersion), String.format("Unsupported schema version '%d' for skybox type %s", schemaVersion, this.name));
    }
}
