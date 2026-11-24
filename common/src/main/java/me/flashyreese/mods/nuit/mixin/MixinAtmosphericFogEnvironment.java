package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.NuitSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.components.RGB;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class MixinAtmosphericFogEnvironment {
    @WrapOperation(
            method = "getBaseColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;",
                    ordinal = 1
            )
    )
    @SuppressWarnings("unchecked")
    private <Value> Value nuit$modifySkyAngle(
            EnvironmentAttributeProbe instance,
            EnvironmentAttribute<Value> attribute,
            float tickDelta,
            Operation<Value> original,
            ClientLevel clientLevel,
            Camera camera,
            int distance,
            float partialTick
    ) {
        final Value sunAngle = original.call(instance, attribute, tickDelta);
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled()) {
            SkyboxManager.CelestialController controller = skyboxManager.getCelestialController().orElse(null);
            if (controller != null) {
                return (Value) (Object) (float) controller.getSkyAngleDegrees(clientLevel, tickDelta);
            }
        }
        return sunAngle;
    }

    @ModifyConstant(
            method = "getBaseColor",
            slice = @Slice(from = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;",
                    ordinal = 0
            )),
            constant = @Constant(intValue = 4, ordinal = 0)
    )
    private int nuit$renderSkyColor(int original) {
        final SkyboxManager skyboxManager = SkyboxManager.getInstance();
        final Skybox skybox = skyboxManager.getCurrentSkybox().orElse(null);
        if (skyboxManager.isEnabled() && skybox instanceof NuitSkybox nuitSkybox && !nuitSkybox.getProperties().renderSunSkyTint()) {
            return Integer.MAX_VALUE;
        } else {
            return original;
        }
    }

    /**
     * Checks if we should change the fog color to whatever the skybox set it to, and sets it.
     */
    @Inject(method = "getBaseColor", at = @At(value = "TAIL"), cancellable = true)
    private void nuit$modifyColors(ClientLevel clientLevel, Camera camera, int i, float f, CallbackInfoReturnable<Integer> cir) {
        int color = cir.getReturnValue();
        float red = ARGB.redFloat(color);
        float green = ARGB.greenFloat(color);
        float blue = ARGB.blueFloat(color);
        final RGB initialFogColor = new RGB(red, green, blue);
        final RGB fogColor = Utils.alphaBlendFogColors(SkyboxManager.getInstance().getActiveSkyboxes(), initialFogColor);
        if (SkyboxManager.getInstance().isEnabled() && !fogColor.equals(initialFogColor)) {
            cir.setReturnValue(ARGB.colorFromFloat(1.0F, fogColor.getRed(), fogColor.getGreen(), fogColor.getBlue()));
        }
    }
}
