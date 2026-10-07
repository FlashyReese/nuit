package me.flashyreese.mods.nuit.mixin;

import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.render.NuitSkyRenderStateHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {
    @Shadow
    private ClientLevel level;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    /**
     * Extracts Nuit's sky state once vanilla has extracted the frame, and attaches it to vanilla's sky render state.
     * <p>
     * Runs at the end of extraction rather than around the sky renderer's own extraction, so it doesn't depend on the
     * sky renderer: it still runs on the first frame, before vanilla has created one, and when another mod replaces it.
     */
    @Inject(method = "extract", at = @At("RETURN"))
    private void nuit$extractSkyboxes(DeltaTracker deltaTracker, Camera camera, float worldPartialTicks, CallbackInfo ci) {
        ((NuitSkyRenderStateHolder) this.levelRenderState.skyRenderState)
                .nuit$setRenderState(SkyboxManager.getInstance().extract(this.level, worldPartialTicks, camera));
    }
}
