package me.flashyreese.mods.nuit.mixin;

import me.flashyreese.mods.nuit.render.NuitSkyRenderState;
import me.flashyreese.mods.nuit.render.NuitSkyRenderStateHolder;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderState.class)
public abstract class MixinSkyRenderState implements NuitSkyRenderStateHolder {
    @Unique
    private @Nullable NuitSkyRenderState nuit$renderState;

    @Override
    public @Nullable NuitSkyRenderState nuit$getRenderState() {
        return this.nuit$renderState;
    }

    @Override
    public void nuit$setRenderState(@Nullable NuitSkyRenderState renderState) {
        this.nuit$renderState = renderState;
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void nuit$resetRenderState(CallbackInfo ci) {
        this.nuit$renderState = null;
    }
}
