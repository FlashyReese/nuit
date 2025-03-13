package io.github.amerebagatelle.mods.nuit.mixin.compatibility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import io.github.amerebagatelle.mods.nuit.compatibility.sodium.formats.PositionTexture;
import io.github.amerebagatelle.mods.nuit.components.*;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.textured.SquareTexturedSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.textured.TexturedSkybox;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@IfModLoaded(value = "sodium")
@Mixin(SquareTexturedSkybox.class)
public abstract class MixinSquareTexturedSkybox extends TexturedSkybox {
    @Shadow
    protected Texture texture;

    protected MixinSquareTexturedSkybox(Properties properties, Conditions conditions, Blend blend) {
        super(properties, conditions, blend);
    }

    @Inject(method = "renderSkybox", at = @At("HEAD"), cancellable = true)
    public void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters, CallbackInfo ci) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        VertexBufferWriter writer = VertexBufferWriter.of(builder);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(6 * 4 * PositionTexture.STRIDE);
            long ptr = buffer;
            for (int face = 0; face < 6; face++) {
                // 0 = bottom | 1 = north | 2 = south | 3 = top | 4 = east | 5 = west
                UVRange tex = Utils.TEXTURE_FACES[face];
                poseStack.pushPose();
                Utils.rotateSkyBoxByFace(poseStack, face);
                Matrix4f matrix4f = poseStack.last().pose();


                PositionTexture.put(ptr, matrix4f, -100F, -100F, -100F, tex.minU(), tex.minV());
                ptr += PositionTexture.STRIDE;

                PositionTexture.put(ptr, matrix4f, -100F, -100F, 100F, tex.minU(), tex.maxV());
                ptr += PositionTexture.STRIDE;

                PositionTexture.put(ptr, matrix4f, 100F, -100F, 100F, tex.maxU(), tex.maxV());
                ptr += PositionTexture.STRIDE;

                PositionTexture.put(ptr, matrix4f, 100F, -100F, -100F, tex.maxU(), tex.minV());
                ptr += PositionTexture.STRIDE;


                poseStack.popPose();
            }

            writer.push(stack, buffer, 6 * 4, PositionTexture.FORMAT);
        }

        RenderSystem.setShaderTexture(0, this.texture.getTextureId());
        BufferUploader.drawWithShader(builder.buildOrThrow());


        ci.cancel();
    }
}
