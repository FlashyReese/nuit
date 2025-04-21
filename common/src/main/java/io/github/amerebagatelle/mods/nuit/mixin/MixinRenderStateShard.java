package io.github.amerebagatelle.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderStateShard.class)
public abstract class MixinRenderStateShard {
    @WrapWithCondition(method = "setupRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private boolean nuit$disableRenderTypeBlending(Runnable runnable) {
        RenderStateShard renderStateShard = (RenderStateShard) (Object) this;
        return !Utils.isOverridingBlending() || !(renderStateShard instanceof RenderStateShard.TransparencyStateShard);
    }

    @WrapWithCondition(method = "clearRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private boolean nuit$enableRenderTypeBlending(Runnable runnable) {
        RenderStateShard renderStateShard = (RenderStateShard) (Object) this;
        return !Utils.isOverridingBlending() || !(renderStateShard instanceof RenderStateShard.TransparencyStateShard);
    }
}
