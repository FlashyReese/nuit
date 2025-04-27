package io.github.amerebagatelle.mods.nuit.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    // I was gonna put MATRICES_COLOR_SNIPPET in the accessWidener as we were previously doing so
    // with TransparencyShard. However, it just doesn't want to work..???
    @Accessor("MATRICES_COLOR_SNIPPET")
    static RenderPipeline.Snippet getMatricesColorSnippet() {
        return null;
    }
}
