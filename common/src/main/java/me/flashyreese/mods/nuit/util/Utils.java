package me.flashyreese.mods.nuit.util;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.api.skyboxes.NuitSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.components.RGB;
import me.flashyreese.mods.nuit.components.RangeEntry;
import me.flashyreese.mods.nuit.components.UVRange;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {
    private static final float SKYBOX_MIN = -100.0F;
    private static final float SKYBOX_MAX = 100.0F;
    private static final Map<Integer, SourceFactor> SOURCE_FACTORS_BY_GL_ID = Arrays.stream(SourceFactor.values())
            .collect(Collectors.toUnmodifiableMap(GlConst::toGl, Function.identity()));
    private static final Map<Integer, DestFactor> DEST_FACTORS_BY_GL_ID = Arrays.stream(DestFactor.values())
            .collect(Collectors.toUnmodifiableMap(GlConst::toGl, Function.identity()));
    public static final UVRange[] TEXTURE_FACES = new UVRange[]{
            new UVRange(0, 0, 1.0F / 3.0F, 1.0F / 2.0F), // bottom
            new UVRange(1.0F / 3.0F, 1.0F / 2.0F, 2.0F / 3.0F, 1), // north
            new UVRange(2.0F / 3.0F, 0, 1, 1.0F / 2.0F), // south
            new UVRange(1.0F / 3.0F, 0, 2.0F / 3.0F, 1.0F / 2.0F), // top
            new UVRange(2.0F / 3.0F, 1.0F / 2.0F, 1, 1), // east
            new UVRange(0, 1.0F / 2.0F, 1.0F / 3.0F, 1) // west
    };
    public static final SkyboxFace[] SKYBOX_FACES = new SkyboxFace[]{
            new SkyboxFace(TEXTURE_FACES[0], new Vector3f(SKYBOX_MIN, SKYBOX_MIN, SKYBOX_MIN), new Vector3f(SKYBOX_MAX - SKYBOX_MIN, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, SKYBOX_MAX - SKYBOX_MIN)),
            new SkyboxFace(TEXTURE_FACES[1], new Vector3f(SKYBOX_MIN, SKYBOX_MAX, SKYBOX_MIN), new Vector3f(SKYBOX_MAX - SKYBOX_MIN, 0.0F, 0.0F), new Vector3f(0.0F, SKYBOX_MIN - SKYBOX_MAX, 0.0F)),
            new SkyboxFace(TEXTURE_FACES[2], new Vector3f(SKYBOX_MAX, SKYBOX_MAX, SKYBOX_MAX), new Vector3f(SKYBOX_MIN - SKYBOX_MAX, 0.0F, 0.0F), new Vector3f(0.0F, SKYBOX_MIN - SKYBOX_MAX, 0.0F)),
            new SkyboxFace(TEXTURE_FACES[3], new Vector3f(SKYBOX_MIN, SKYBOX_MAX, SKYBOX_MAX), new Vector3f(SKYBOX_MAX - SKYBOX_MIN, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, SKYBOX_MIN - SKYBOX_MAX)),
            new SkyboxFace(TEXTURE_FACES[4], new Vector3f(SKYBOX_MAX, SKYBOX_MAX, SKYBOX_MIN), new Vector3f(0.0F, 0.0F, SKYBOX_MAX - SKYBOX_MIN), new Vector3f(0.0F, SKYBOX_MIN - SKYBOX_MAX, 0.0F)),
            new SkyboxFace(TEXTURE_FACES[5], new Vector3f(SKYBOX_MIN, SKYBOX_MAX, SKYBOX_MAX), new Vector3f(0.0F, 0.0F, SKYBOX_MIN - SKYBOX_MAX), new Vector3f(0.0F, SKYBOX_MIN - SKYBOX_MAX, 0.0F))
    };

    // 0 = bottom | 1 = north | 2 = south | 3 = top | 4 = east | 5 = west
    private static final Matrix4f[] MATRIX4F_ROTATED_FACE = new Matrix4f[]{
            new Matrix4f(), // 0 (Bottom)
            new Matrix4f().rotateX(90.0F * Mth.DEG_TO_RAD), // 1 (North)
            new Matrix4f().rotateX(-90.0F * Mth.DEG_TO_RAD).rotateY(180.0F * Mth.DEG_TO_RAD), // 2 (South)
            new Matrix4f().rotateX(180.0F * Mth.DEG_TO_RAD), // 3 (Top)
            new Matrix4f().rotateZ(90.0F * Mth.DEG_TO_RAD).rotateY(-90.0F * Mth.DEG_TO_RAD), // 4 (East)
            new Matrix4f().rotateZ(-90.0F * Mth.DEG_TO_RAD).rotateY(90.0F * Mth.DEG_TO_RAD) // 5 (West)
    };

    /**
     * Maps input intersection to output intersection, does so by taking in input and output UV ranges and then mapping the input intersection to the output intersection.
     *
     * @param input             The input UV range
     * @param output            The output UV range
     * @param inputIntersection The input intersection
     * @return The output intersection
     */
    public static UVRange mapUVRanges(UVRange input, UVRange output, UVRange inputIntersection) {
        float inputUWidth = input.maxU() - input.minU();
        float outputUWidth = output.maxU() - output.minU();
        float inputVHeight = input.maxV() - input.minV();
        float outputVHeight = output.maxV() - output.minV();

        float u1 = (inputIntersection.minU() - input.minU()) / inputUWidth * outputUWidth + output.minU();
        float u2 = (inputIntersection.maxU() - input.minU()) / inputUWidth * outputUWidth + output.minU();
        float v1 = (inputIntersection.minV() - input.minV()) / inputVHeight * outputVHeight + output.minV();
        float v2 = (inputIntersection.maxV() - input.minV()) / inputVHeight * outputVHeight + output.minV();

        return new UVRange(u1, v1, u2, v2);
    }

    /**
     * Finds the intersection between two UV ranges
     *
     * @param first  First UV range
     * @param second Second UV range
     * @return The intersection between the two UV ranges, if none is found, null is returned
     */
    public static UVRange findUVIntersection(UVRange first, UVRange second) {
        float intersectionMinU = Math.max(first.minU(), second.minU());
        float intersectionMaxU = Math.min(first.maxU(), second.maxU());
        float intersectionMinV = Math.max(first.minV(), second.minV());
        float intersectionMaxV = Math.min(first.maxV(), second.maxV());
        if (intersectionMaxU >= intersectionMinU && intersectionMaxV >= intersectionMinV) {
            return new UVRange(intersectionMinU, intersectionMinV, intersectionMaxU, intersectionMaxV);
        } else {
            // No intersection
            return null;
        }
    }

    /**
     * @return Whether the value is within any of the rangeEntries.
     */
    public static boolean checkRanges(double value, List<RangeEntry> rangeEntries, boolean inverse) {
        return rangeEntries.isEmpty() || (inverse ^ rangeEntries.stream()
                .anyMatch(entry -> value >= entry.min() && value < entry.max()));
    }

    /**
     * Calculates the rotation in degrees for skybox rotations
     *
     * @param rotationSpeed    Rotation speed
     * @param isSkyboxRotation Whether it is a skybox rotation or decoration rotation
     * @param world            Client world
     * @return Rotation in degrees
     */
    public static double calculateRotation(double rotationSpeed, boolean isSkyboxRotation, ClientLevel world) {
        if (rotationSpeed != 0F) {
            long timeOfDay = world.getDayTime();
            double rotationFraction = timeOfDay / (24000.0D / rotationSpeed);
            double skyAngle = Mth.positiveModulo(rotationFraction, 1);
            if (isSkyboxRotation) {
                return 360D * skyAngle;
            } else {
                return 360D * skyAngle;
            }
        } else {
            return 0D;
        }
    }

    /**
     * Get the Matrix4f rotation for the requested cube face of the skybox
     *
     * @param face
     * @return The Matrix4f rotation for the requested cube face of the skybox
     */
    public static Matrix4f getMatrixForRotatedFace(int face) {
        if (face >= MATRIX4F_ROTATED_FACE.length) {
            throw new RuntimeException("Face is out of bounds");
        }

        return MATRIX4F_ROTATED_FACE[face];
    }

    public static Identifier getVanillaSkyboxId(DimensionType.Skybox skybox) {
        return switch (skybox) {
            case NONE -> Identifier.withDefaultNamespace("none");
            case OVERWORLD -> Identifier.withDefaultNamespace("overworld");
            case END -> Identifier.withDefaultNamespace("end");
        };
    }

    @Deprecated(forRemoval = true)
    public static Identifier getVanillaWorldId(DimensionType.Skybox skybox) {
        return getVanillaSkyboxId(skybox);
    }

    public static void addTexturedSkyboxFace(BufferBuilder builder, Matrix4f matrix4f, SkyboxFace face, UVRange positionRange, UVRange textureRange) {
        Vector3f minMin = face.getPosition(positionRange.minU(), positionRange.minV());
        Vector3f minMax = face.getPosition(positionRange.minU(), positionRange.maxV());
        Vector3f maxMax = face.getPosition(positionRange.maxU(), positionRange.maxV());
        Vector3f maxMin = face.getPosition(positionRange.maxU(), positionRange.minV());
        builder.addVertex(matrix4f, minMin.x(), minMin.y(), minMin.z()).setUv(textureRange.minU(), textureRange.minV());
        builder.addVertex(matrix4f, minMax.x(), minMax.y(), minMax.z()).setUv(textureRange.minU(), textureRange.maxV());
        builder.addVertex(matrix4f, maxMax.x(), maxMax.y(), maxMax.z()).setUv(textureRange.maxU(), textureRange.maxV());
        builder.addVertex(matrix4f, maxMin.x(), maxMin.y(), maxMin.z()).setUv(textureRange.maxU(), textureRange.minV());
    }

    /**
     * Rotates the faces of the vertexes for the skybox
     *
     * @param poseStack
     * @param face
     */
    public static void rotateSkyBoxByFace(PoseStack poseStack, int face) {
        if (face == 1) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else if (face == 2) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        } else if (face == 3) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        } else if (face == 4) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (face == 5) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    public record SkyboxFace(UVRange textureRange, Vector3f origin, Vector3f uAxis, Vector3f vAxis) {
        public Vector3f getPosition(float u, float v) {
            float normalizedU = (u - this.textureRange.minU()) / (this.textureRange.maxU() - this.textureRange.minU());
            float normalizedV = (v - this.textureRange.minV()) / (this.textureRange.maxV() - this.textureRange.minV());
            return new Vector3f(this.origin).fma(normalizedU, this.uAxis).fma(normalizedV, this.vAxis);
        }
    }

    /**
     * Calculates the interpolated alpha value based on the current time within an fade cycle.
     * This method supports cyclical fade where keyframes can loop back to the start.
     *
     * @param currentTime          The current time in the fade cycle.
     * @param duration             The total duration of the fade cycle.
     * @param currentKeyFrame      The timestamp of the current keyframe.
     * @param nextKeyFrame         The timestamp of the next keyframe.
     * @param currentKeyFrameValue The alpha value at the current keyframe.
     * @param nextKeyFrameValue    The alpha value at the next keyframe.
     * @return The interpolated alpha value based on the current time.
     */
    public static float calculateInterpolatedAlpha(long currentTime, long duration, long currentKeyFrame, long nextKeyFrame, float currentKeyFrameValue, float nextKeyFrameValue) {
        // If both keyframes have the same value or the same timestamp, no interpolation is needed.
        if (currentKeyFrameValue == nextKeyFrameValue || currentKeyFrame == nextKeyFrame) {
            return nextKeyFrameValue;
        }

        long cycleDuration;
        long timePassedInCycle;

        // Handle cyclical keyframes where the next keyframe is before the current keyframe in time.
        if (currentKeyFrame > nextKeyFrame) {
            // The cycle wraps around, so we calculate the duration from currentKeyFrame to nextKeyFrame.
            cycleDuration = duration - currentKeyFrame + nextKeyFrame;

            // Determine how much time has passed in the cycle based on the current time.
            if (currentTime < nextKeyFrame) {
                // If currentTime is in the second half of the cycle (after wrapping around),
                // calculate time passed from the previous cycle's end.
                timePassedInCycle = duration - currentKeyFrame + currentTime;
            } else {
                // Otherwise, calculate time passed normally within the first half.
                timePassedInCycle = currentTime - currentKeyFrame;
            }
        } else {
            // Standard case where keyframes are in order without wrapping.
            cycleDuration = nextKeyFrame - currentKeyFrame;
            timePassedInCycle = currentTime - currentKeyFrame;
        }

        // Perform linear interpolation between the two keyframe values.
        return currentKeyFrameValue + ((float) timePassedInCycle / cycleDuration) * (nextKeyFrameValue - currentKeyFrameValue);
    }

    /**
     * Finds the closest keyframes to the given current time from a map of keyframes.
     *
     * @param keyFrames   A map of keyframes (timestamps) to alpha values.
     * @param currentTime The current time for which to find the closest keyframes.
     * @return A pair of timestamps representing the closest keyframes before and after the current time.
     */
    public static <T> Optional<Tuple<Long, Long>> findClosestKeyframes(Map<Long, T> keyFrames, long currentTime) {
        if (keyFrames.isEmpty())
            return Optional.empty();

        long smallestValue = Long.MAX_VALUE;
        long largestValue = Long.MIN_VALUE;
        long closestLowerKeyFrame = Long.MIN_VALUE;
        long closestHigherKeyFrame = Long.MAX_VALUE;

        for (long keyFrame : keyFrames.keySet()) {
            if (keyFrame < smallestValue)
                smallestValue = keyFrame;

            if (keyFrame > largestValue)
                largestValue = keyFrame;

            if (keyFrame <= currentTime && keyFrame > closestLowerKeyFrame) {
                closestLowerKeyFrame = keyFrame;
            }
            if (keyFrame > currentTime && keyFrame < closestHigherKeyFrame) {
                closestHigherKeyFrame = keyFrame;
            }
        }

        // Handle cases where the current time is before the first keyframe or after the last keyframe and single keyframe cases
        if (closestLowerKeyFrame == Long.MIN_VALUE || closestHigherKeyFrame == Long.MAX_VALUE) {
            closestLowerKeyFrame = largestValue;
            closestHigherKeyFrame = smallestValue;
        }

        return Optional.of(new Tuple<>(closestLowerKeyFrame, closestHigherKeyFrame));
    }

    /**
     * Interpolates between two quaternion keyframes.
     *
     * @param keyFrames    The keyframe map being used.
     * @param chosenFrames The two frames to interpolate between.
     * @param currentTime  The current time in game ticks.
     * @return The interpolated quaternion.
     */
    public static Quaternionf interpolateQuatKeyframes(Map<Long, Quaternionf> keyFrames, Tuple<Long, Long> chosenFrames, long currentTime, long duration) {
        if (keyFrames.size() == 1) {
            return keyFrames.values().iterator().next();
        }

        long currentKey = chosenFrames.getA();
        long nextKey = chosenFrames.getB();

        long cycleDuration;
        long timePassedInCycle;

        if (currentKey > nextKey) {
            cycleDuration = duration - currentKey + nextKey;

            if (currentTime < nextKey) {
                timePassedInCycle = duration - currentKey + currentTime;
            } else {
                timePassedInCycle = currentTime - currentKey;
            }
        } else {
            cycleDuration = nextKey - currentKey;
            timePassedInCycle = currentTime - currentKey;
        }

        float alpha = (float) timePassedInCycle / cycleDuration;

        var result = new Quaternionf();
        keyFrames.get(currentKey).nlerp(keyFrames.get(nextKey), alpha, result);
        return result;
    }

    /**
     * Blends all fog colors using the alpha blending formula: (source * source_alpha) + (destination * (1 - source_alpha)).
     *
     * @param skyboxList      List of skyboxes to blend the fog colors from.
     * @param initialFogColor The initial fog color to be blended with the skybox fog colors.
     * @return The final blended fog color.
     */
    public static RGB alphaBlendFogColors(List<Skybox> skyboxList, RGB initialFogColor) {
        RGB destination = initialFogColor;
        for (Skybox skybox : skyboxList) {
            if (skybox.isActive() && skybox instanceof NuitSkybox nuitSkybox && nuitSkybox.getProperties().fog().isModifyColors()) {
                final RGB fogColor = nuitSkybox.getProperties().fog();
                destination = fogColor.blend(destination, nuitSkybox.getAlpha());
            }
        }

        return destination;
    }

    public static float alphaBlendFogDensity(List<Skybox> skyboxList, float initialFogDensity) {
        float destination = initialFogDensity;
        for (Skybox skybox : skyboxList) {
            if (skybox.isActive() && skybox instanceof NuitSkybox nuitSkybox && nuitSkybox.getProperties().fog().isModifyDensity()) {
                float sourceAlphaInv = 1.0F - nuitSkybox.getAlpha();
                destination = (nuitSkybox.getProperties().fog().getDensity() * nuitSkybox.getAlpha()) + (destination * sourceAlphaInv);
            }
        }

        return destination;
    }

    /**
     * Calculates the condition alpha
     *
     * @param maxAlpha  The maximum alpha value
     * @param minAlpha  The minimum alpha value
     * @param lastAlpha The last condition alpha value
     * @param duration  The duration
     * @param in        Whether it will transition in or out
     * @return condition alpha
     */
    public static float calculateConditionAlphaValue(float maxAlpha, float minAlpha, float lastAlpha, int duration, boolean in) {
        if (duration == 0) {
            return lastAlpha;
        } else if (in && maxAlpha == lastAlpha) {
            return maxAlpha;
        } else if (!in && lastAlpha == minAlpha) {
            return minAlpha;
        } else {
            float alphaChange = (maxAlpha - minAlpha) / duration;
            float result = in ? lastAlpha + alphaChange : lastAlpha - alphaChange;
            return Mth.clamp(result, minAlpha, maxAlpha);
        }
    }

    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform.
    public static <T> T loadService(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        NuitClient.getLogger().debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static SourceFactor toSourceFactor(int glId) {
        SourceFactor factor = SOURCE_FACTORS_BY_GL_ID.get(glId);
        if (factor == null) {
            throw new IllegalArgumentException("Unknown SourceFactor with GL id of " + glId);
        }
        return factor;
    }

    public static DestFactor toDestFactor(int glId) {
        DestFactor factor = DEST_FACTORS_BY_GL_ID.get(glId);
        if (factor == null) {
            throw new IllegalArgumentException("Unknown DestFactor with GL id of " + glId);
        }
        return factor;
    }

    public static boolean isSourceFactor(int glId) {
        return SOURCE_FACTORS_BY_GL_ID.containsKey(glId);
    }

    public static boolean isDestFactor(int glId) {
        return DEST_FACTORS_BY_GL_ID.containsKey(glId);
    }
}
