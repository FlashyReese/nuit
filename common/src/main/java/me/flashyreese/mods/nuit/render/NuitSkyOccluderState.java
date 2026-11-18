package me.flashyreese.mods.nuit.render;

import me.flashyreese.mods.nuit.components.Occluder;
import me.flashyreese.mods.nuit.components.RGB;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Combines occluder requests into one draw after all skyboxes and decorations.
 */
@ApiStatus.Internal
public final class NuitSkyOccluderState {
    private final Vector3f weightedColor = new Vector3f();
    private float totalWeight;
    private float opacity;

    public void request(Occluder occluder, float skyboxAlpha, Vector4fc fogColor) {
        if (!occluder.enabled()) {
            return;
        }

        float weight = Math.clamp(occluder.opacity(), 0.0F, 1.0F) * Math.clamp(skyboxAlpha, 0.0F, 1.0F);
        if (!(weight > 0.0F)) {
            return;
        }

        RGB color = occluder.color().orElse(null);
        this.weightedColor.add(
                (color != null ? color.getRed() : fogColor.x()) * weight,
                (color != null ? color.getGreen() : fogColor.y()) * weight,
                (color != null ? color.getBlue() : fogColor.z()) * weight
        );
        this.totalWeight += weight;
        this.opacity = Math.max(this.opacity, weight);
    }

    /**
     * Returns the opacity-weighted color with the strongest requested opacity, or {@code null} without any requests.
     */
    public @Nullable Vector4f resolveColor() {
        if (this.totalWeight == 0.0F) {
            return null;
        }

        return new Vector4f(
                this.weightedColor.x / this.totalWeight,
                this.weightedColor.y / this.totalWeight,
                this.weightedColor.z / this.totalWeight,
                this.opacity
        );
    }
}
