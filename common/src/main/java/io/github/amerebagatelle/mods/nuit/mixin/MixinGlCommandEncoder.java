package io.github.amerebagatelle.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(GlCommandEncoder.class)
public abstract class MixinGlCommandEncoder {
    @WrapOperation(method = "applyPipelineState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getBlendFunction()Ljava/util/Optional;"))
    private Optional<BlendFunction> nuit$overrideBlending(RenderPipeline instance, Operation<Optional<BlendFunction>> original) {
        if (Utils.isOverridingBlending()) {
            return Utils.getOverridenBlendFunction() != null ? Optional.of(Utils.getOverridenBlendFunction()) : Optional.empty();
        } else {
            return original.call(instance);
        }
    }
}
