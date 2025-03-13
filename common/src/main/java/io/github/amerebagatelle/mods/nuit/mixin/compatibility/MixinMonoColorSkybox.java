package io.github.amerebagatelle.mods.nuit.mixin.compatibility;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.RGBA;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.AbstractSkybox;
import io.github.amerebagatelle.mods.nuit.skybox.MonoColorSkybox;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.CoreShaders;
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
@Mixin(MonoColorSkybox.class)
public abstract class MixinMonoColorSkybox extends AbstractSkybox {

    @Shadow
    public Blend blend;

    @Shadow
    public RGBA color;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters, CallbackInfo ci) {
        ci.cancel();

        RenderSystem.setShaderFog(fogParameters);
        if (this.alpha > 0) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            this.blend.apply(this.alpha);

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            VertexBufferWriter writer = VertexBufferWriter.of(builder);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long buffer = stack.nmalloc(6 * 4 * ColorVertex.STRIDE);
                long ptr = buffer;

                int color = ColorABGR.pack(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha);

                for (int face = 0; face < 6; ++face) {
                    poseStack.pushPose();
                    Utils.rotateSkyBoxByFace(poseStack, face);
                    Matrix4f matrix4f = poseStack.last().pose();


                    ColorVertex.put(ptr, matrix4f, -100F, -100F, -100F, color);
                    ptr += ColorVertex.STRIDE;

                    ColorVertex.put(ptr, matrix4f, -100F, -100F, 100F, color);
                    ptr += ColorVertex.STRIDE;

                    ColorVertex.put(ptr, matrix4f, 100F, -100F, 100F, color);
                    ptr += ColorVertex.STRIDE;

                    ColorVertex.put(ptr, matrix4f, 100F, -100F, -100F, color);
                    ptr += ColorVertex.STRIDE;


                    poseStack.popPose();
                }
                writer.push(stack, buffer, 6 * 4, ColorVertex.FORMAT);
            }

            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            BufferUploader.drawWithShader(builder.buildOrThrow());
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }
}
