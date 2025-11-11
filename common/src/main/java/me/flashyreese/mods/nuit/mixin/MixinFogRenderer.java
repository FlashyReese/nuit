package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {

    @ModifyReturnValue(method = "computeFogColor", at = @At(value = "RETURN"))
    private static Vector4f nuit$redirectSetShaderFogColor(Vector4f original) {
        if (SkyboxManager.getInstance().isEnabled()) {
            final float fogDensity = Utils.alphaBlendFogDensity(SkyboxManager.getInstance().getActiveSkyboxes(), original.w());
            return new Vector4f(original.x(), original.y(), original.z(), fogDensity);
        }
        return original;
    }
}
