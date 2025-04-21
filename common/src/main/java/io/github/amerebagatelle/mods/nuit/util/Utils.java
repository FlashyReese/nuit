package io.github.amerebagatelle.mods.nuit.util;

import com.google.common.collect.Range;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.NuitSkybox;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import io.github.amerebagatelle.mods.nuit.components.RGB;
import io.github.amerebagatelle.mods.nuit.components.RangeEntry;
import io.github.amerebagatelle.mods.nuit.components.UVRange;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class Utils {
    private static boolean overrideRenderTypeBlending = false;

    public static final UVRange[] TEXTURE_FACES = new UVRange[]{
            new UVRange(0, 0, 1.0F / 3.0F, 1.0F / 2.0F), // bottom
            new UVRange(1.0F / 3.0F, 1.0F / 2.0F, 2.0F / 3.0F, 1), // north
            new UVRange(2.0F / 3.0F, 0, 1, 1.0F / 2.0F), // south
            new UVRange(1.0F / 3.0F, 0, 2.0F / 3.0F, 1.0F / 2.0F), // top
            new UVRange(2.0F / 3.0F, 1.0F / 2.0F, 1, 1), // east
            new UVRange(0, 1.0F / 2.0F, 1.0F / 3.0F, 1) // west
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
                .map(entry -> Range.closed(entry.min(), entry.max()))
                .anyMatch(range -> range.contains((float) value)));
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
                return 360D * world.dimensionType().timeOfDay((long) (24000 * skyAngle));
            }
        } else {
            return 0D;
        }
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
    public static Quaternionf interpolateQuatKeyframes(Map<Long, Quaternionf> keyFrames, Tuple<Long, Long> chosenFrames, long currentTime) {
        if (keyFrames.size() == 1) {
            return keyFrames.values().iterator().next();
        }

        var alpha = Math.abs((float) (currentTime - chosenFrames.getA()) / (chosenFrames.getB() - chosenFrames.getA()));
        var result = new Quaternionf();
        keyFrames.get(chosenFrames.getA()).nlerp(keyFrames.get(chosenFrames.getB()), alpha, result);
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
                RGB source = nuitSkybox.getProperties().fog();

                float sourceAlphaInv = 1f - nuitSkybox.getAlpha();
                destination = new RGB(
                        (source.getRed() * nuitSkybox.getAlpha()) + (destination.getRed() * sourceAlphaInv),
                        (source.getGreen() * nuitSkybox.getAlpha()) + (destination.getGreen() * sourceAlphaInv),
                        (source.getBlue() * nuitSkybox.getAlpha()) + (destination.getBlue() * sourceAlphaInv)
                );
            }
        }

        return destination;
    }

    public static float alphaBlendFogDensity(List<Skybox> skyboxList, float initialFogDensity) {
        float destination = initialFogDensity;
        for (Skybox skybox : skyboxList) {
            if (skybox.isActive() && skybox instanceof NuitSkybox nuitSkybox && nuitSkybox.getProperties().fog().isModifyDensity()) {
                float sourceAlphaInv = 1f - nuitSkybox.getAlpha();
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

    public static void enableBlendingOverride() {
        overrideRenderTypeBlending = true;
    }

    public static boolean isOverridingBlending() {
        return overrideRenderTypeBlending;
    }

    public static void disableBlendingOverride() {
        overrideRenderTypeBlending = false;
    }
}
