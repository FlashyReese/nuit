package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

@ApiStatus.Internal
public interface SkyboxRenderAccess {
    void renderSkyDisc(Vector4fc color);

    void renderSkyDisc(Vector3fc color);

    void renderDarkDisc();

    void renderStars(float brightness, PoseStack poseStack);

    void renderEndFlash(float intensity, float xAngle, float yAngle);

    Identifier endSkyTexture();
}
