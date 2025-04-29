package io.github.amerebagatelle.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.amerebagatelle.mods.nuit.util.OverrideUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(GlCommandEncoder.class)
public abstract class MixinGlCommandEncoder {
    @WrapOperation(method = "applyPipelineState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getBlendFunction()Ljava/util/Optional;"))
    private Optional<BlendFunction> nuit$overrideBlending(RenderPipeline instance, Operation<Optional<BlendFunction>> original) {
        if (OverrideUtils.isOverridingBlending()) {
            return OverrideUtils.getOverridenBlendFunction() != null ? Optional.of(OverrideUtils.getOverridenBlendFunction()) : Optional.empty();
        } else {
            return original.call(instance);
        }
    }
}
