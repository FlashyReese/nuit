package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.render.NuitSkyRenderStateHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/SkyRenderState;)V"
            )
    )
    private void nuit$extractSkyboxes(SkyRenderer skyRenderer, ClientLevel level, float partialTick, Camera camera,
                                     SkyRenderState skyRenderState, Operation<Void> original) {
        original.call(skyRenderer, level, partialTick, camera, skyRenderState);
        ((NuitSkyRenderStateHolder) skyRenderState)
                .nuit$setRenderState(SkyboxManager.getInstance().extract(level, camera.getPartialTickTime(), camera));
    }
}
