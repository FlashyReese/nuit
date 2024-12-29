package io.github.amerebagatelle.mods.nuit.components;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.util.CodecUtils;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Tuple;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.Optional;

public record Rotation(boolean skyboxRotation, Map<Long, Quaternionf> mapping, Map<Long, Quaternionf> axis,
                       long duration, float speed) {
    private static final Codec<Quaternionf> QUAT_FROM_VEC_3_F = Codec.FLOAT.listOf().comapFlatMap((list) -> {
        if (list.size() != 3) {
            return DataResult.error(() -> "Invalid number of elements in vector");
        }

        Quaternionf result = new Quaternionf();
        result.rotateLocalX((float) Math.toRadians(list.get(0))); // X
        result.rotateLocalY((float) Math.toRadians(list.get(1))); // Y
        result.rotateLocalZ((float) Math.toRadians(list.get(2))); // Y

        return DataResult.success(result);
    }, (vec) -> ImmutableList.of(vec.x(), vec.y(), vec.z()));
    public static final Codec<Rotation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("skyboxRotation", true).forGetter(Rotation::skyboxRotation),
            CodecUtils.unboundedMapFixed(Long.class, QUAT_FROM_VEC_3_F, Long2ObjectOpenHashMap::new)
                    .optionalFieldOf("mapping", CodecUtils.fastUtilLong2ObjectOpenHashMap())
                    .forGetter(Rotation::mapping),
            CodecUtils.unboundedMapFixed(Long.class, QUAT_FROM_VEC_3_F, Long2ObjectOpenHashMap::new)
                    .optionalFieldOf("axis", CodecUtils.fastUtilLong2ObjectOpenHashMap())
                    .forGetter(Rotation::axis),
            Codec.LONG.optionalFieldOf("duration", 24000L).forGetter(Rotation::duration),
            Codec.FLOAT.optionalFieldOf("speed", 1f).forGetter(Rotation::speed)
    ).apply(instance, Rotation::new));

    public void rotateStack(PoseStack poseStack, ClientLevel world) {
        long currentTime = world.getDayTime() % this.duration;
//         static
        Quaternionf resultRot = new Quaternionf();

        Optional<Tuple<Long, Long>> possibleMappingKeyframes = Utils.findClosestKeyframes(this.mapping, currentTime);
        Quaternionf mappingRot = new Quaternionf();

        Optional<Tuple<Long, Long>> possibleAxisKeyframes = Utils.findClosestKeyframes(this.axis, currentTime);
        Quaternionf axisRot = new Quaternionf();

        possibleAxisKeyframes.ifPresent(axisKeyframe -> {
            // Set the axis rotation to the multiplication of the mapping rot and the axis rot
            mappingRot.mul(Utils.interpolateQuatKeyframes(this.axis, axisKeyframe, currentTime), axisRot);
            resultRot.mul(axisRot);

            double timeRotation = Utils.calculateRotation(this.speed, this.skyboxRotation, world);
            resultRot.mul(Axis.XP.rotationDegrees((float) timeRotation).mul(mappingRot));

            resultRot.mul(axisRot.conjugate());
        });

        possibleMappingKeyframes.ifPresent(mappingKeyframe -> {
            mappingRot.set(Utils.interpolateQuatKeyframes(this.mapping, mappingKeyframe, currentTime));
            resultRot.mul(mappingRot);
        });

        poseStack.mulPose(resultRot);
    }

    public static Rotation of() {
        return new Rotation(true, Map.of(), Map.of(), 24000L, 1f);
    }

    public static Rotation decorations() {
        return new Rotation(false, Map.of(), Map.of(), 24000L, 1f);
    }
}