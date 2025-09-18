package me.flashyreese.mods.nuit.components;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.flashyreese.mods.nuit.util.CodecUtils;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Tuple;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.Optional;

public record Rotation(boolean skyboxRotation, Map<Long, Quaternionf> mapping, Map<Long, Quaternionf> axis,
                       long duration, float speed) {
    private static final Codec<Quaternionf> QUATERNIONF_FROM_VEC_3_F = Codec.FLOAT.listOf().comapFlatMap((list) -> {
        if (list.size() != 3) {
            return DataResult.error(() -> "Invalid number of elements in vector");
        } else {
            return DataResult.success(new Quaternionf()
                    .rotateLocalX((float) Math.toRadians(list.get(0)))
                    .rotateLocalY((float) Math.toRadians(list.get(1)))
                    .rotateLocalZ((float) Math.toRadians(list.get(2))));
        }
    }, (vec) -> ImmutableList.of(vec.x(), vec.y(), vec.z()));

    public static final Codec<Rotation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("skyboxRotation", true).forGetter(Rotation::skyboxRotation),
            CodecUtils.unboundedMapFixed(Long.class, QUATERNIONF_FROM_VEC_3_F, Long2ObjectOpenHashMap::new)
                    .optionalFieldOf("mapping", CodecUtils.fastUtilLong2ObjectOpenHashMap())
                    .forGetter(Rotation::mapping),
            CodecUtils.unboundedMapFixed(Long.class, QUATERNIONF_FROM_VEC_3_F, Long2ObjectOpenHashMap::new)
                    .optionalFieldOf("axis", CodecUtils.fastUtilLong2ObjectOpenHashMap())
                    .forGetter(Rotation::axis),
            Codec.LONG.optionalFieldOf("duration", 24000L).forGetter(Rotation::duration),
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Rotation::speed)
    ).apply(instance, Rotation::new));

    public Matrix4f apply(Matrix4f matrix4f, ClientLevel level) {
        final long currentTime = level.getDayTime() % this.duration;
        Quaternionf resultRot = new Quaternionf();

        Optional<Tuple<Long, Long>> possibleMappingKeyframes = Utils.findClosestKeyframes(this.mapping, currentTime);
        Quaternionf mappingRot = new Quaternionf();

        Optional<Tuple<Long, Long>> possibleAxisKeyframes = Utils.findClosestKeyframes(this.axis, currentTime);
        Quaternionf axisRot = new Quaternionf();

        possibleAxisKeyframes.ifPresent(axisKeyframe -> {
            // Set the axis rotation to the multiplication of the mapping rot and the axis rot
            mappingRot.mul(Utils.interpolateQuatKeyframes(this.axis, axisKeyframe, currentTime, this.duration), axisRot);
            resultRot.mul(axisRot);

            double timeRotation = Utils.calculateRotation(this.speed, this.skyboxRotation, level);
            resultRot.mul(Axis.YP.rotationDegrees((float) timeRotation).mul(mappingRot));

            resultRot.mul(axisRot.conjugate());
        });

        possibleMappingKeyframes.ifPresent(mappingKeyframe -> {
            mappingRot.set(Utils.interpolateQuatKeyframes(this.mapping, mappingKeyframe, currentTime, this.duration));
            resultRot.mul(mappingRot);
        });

        return matrix4f.rotate(resultRot);
    }

    public void apply(PoseStack poseStack, ClientLevel level) {
        poseStack.mulPose(apply(new Matrix4f(), level));
    }

    public static Rotation of() {
        return new Rotation(true, Map.of(), Map.of(), 24000L, 1.0F);
    }

    public static Rotation decorations() {
        return new Rotation(false, Map.of(), Map.of(), 24000L, 1.0F);
    }
}