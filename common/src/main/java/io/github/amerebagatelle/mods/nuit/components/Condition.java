package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public record Condition<T>(boolean excludes, List<T> entries) {
    public static <T> Condition<T> of() {
        return new Condition<>(false, ObjectArrayList.of());
    }

    public static <T> Codec<Condition<T>> create(Codec<T> codec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("excludes", false).forGetter(Condition::excludes),
                codec.listOf().optionalFieldOf("entries", ObjectArrayList.of()).forGetter(Condition::entries)
        ).apply(instance, Condition::new));
    }
}
