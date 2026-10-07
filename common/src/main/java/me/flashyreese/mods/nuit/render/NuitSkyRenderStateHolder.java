package me.flashyreese.mods.nuit.render;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface NuitSkyRenderStateHolder {
    @Nullable NuitSkyRenderState nuit$getRenderState();

    void nuit$setRenderState(@Nullable NuitSkyRenderState renderState);
}
