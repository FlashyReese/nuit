package me.flashyreese.mods.nuit.mixin;

import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.NuitSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.components.RGB;
import me.flashyreese.mods.nuit.skybox.decorations.DecorationBox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class MixinAtmosphericFogEnvironment {
    // TODO 1.21.11
    /*@Redirect(method = "getBaseColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private float nuit$redirectSkyAngle(ClientLevel instance, float v) {
        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorBox && decorBox.getProperties().rotation().skyboxRotation())) {
            return Mth.positiveModulo(instance.getDayTime() / 24000F + 0.75F, 1);
        } else {
            return instance.getTimeOfDay(v);
        }
    }*/

    @Redirect(method = "getBaseColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 1))
    private <Value> Value nuit$redirectSkyAngleRadian(EnvironmentAttributeProbe instance, EnvironmentAttribute<Value> probe, float tickDelta) {
        final Value sunAngle = instance.getValue(probe, tickDelta);
        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorBox && decorBox.getProperties().rotation().skyboxRotation())) {
            return (Value) (Object) (Mth.positiveModulo(((float) sunAngle) / 24000F + 0.75F, 1) * Mth.DEG_TO_RAD);
        } else {
            return sunAngle;
        }
    }

    @ModifyConstant(method = "getBaseColor", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 0)), constant = @Constant(intValue = 4, ordinal = 0))
    private int nuit$renderSkyColor(int original) {
        final SkyboxManager skyboxManager = SkyboxManager.getInstance();
        final Skybox skybox = skyboxManager.getCurrentSkybox();
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
        float blue = ARGB.blueFloat(color);
        float green = ARGB.greenFloat(color);
        final RGB initialFogColor = new RGB(red, blue, green);
        final RGB fogColor = Utils.alphaBlendFogColors(SkyboxManager.getInstance().getActiveSkyboxes(), initialFogColor);
        if (SkyboxManager.getInstance().isEnabled() && !fogColor.equals(initialFogColor)) {
            cir.setReturnValue(ARGB.colorFromFloat(1.0F, fogColor.getRed(), fogColor.getGreen(), fogColor.getBlue()));
        }
    }
}
