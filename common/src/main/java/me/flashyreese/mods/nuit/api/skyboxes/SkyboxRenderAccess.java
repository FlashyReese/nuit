package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SkyboxRenderAccess {
    void renderSkyDisc(int color);

    void renderDarkDisc();

    void renderStars(float brightness, PoseStack poseStack);

    void renderEndFlash(float intensity, float xAngle, float yAngle);

    Identifier endSkyTexture();
}
