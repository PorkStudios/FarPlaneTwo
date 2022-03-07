/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.client.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.MatrixHelper;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.client.render.GlobalUniformAttributes;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATFogRenderer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.client.renderer.IMixinWorldRenderer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.TerrainRenderingBlockedTracker1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.compat.of.OFHelper1_16;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static net.minecraft.util.math.MathHelper.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer1_16 implements IMixinWorldRenderer1_16 {
    @Shadow
    private ClientWorld level;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    protected TerrainRenderingBlockedTracker1_16 fp2_vanillaRenderabilityTracker;

    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;setLevel(Lnet/minecraft/client/world/ClientWorld;)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/WorldRenderer;viewArea:Lnet/minecraft/client/renderer/ViewFrustum;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 1)
    private void fp2_setLevel_onDestroyViewFrustum(CallbackInfo ci) {
        this.fp2_vanillaRenderabilityTracker.release();
        this.fp2_vanillaRenderabilityTracker = null;
    }

    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;allChanged()V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/WorldRenderer;viewArea:Lnet/minecraft/client/renderer/ViewFrustum;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE),
            require = 1)
    private void fp2_allChanged_onCreateViewFrustum(CallbackInfo ci) {
        //only create a new renderability tracker if none existed before.
        //  we don't want multiple instances lying around!
        if (this.fp2_vanillaRenderabilityTracker == null) {
            this.fp2_vanillaRenderabilityTracker = new TerrainRenderingBlockedTracker1_16();
        }
    }

    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;setupRender(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelper;ZIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/profiler/IProfiler;pop()V"),
            slice = @Slice(
                    from = @At(value = "INVOKE_STRING",
                            target = "Lnet/minecraft/profiler/IProfiler;push(Ljava/lang/String;)V",
                            args = "ldc=iteration"),
                    to = @At(value = "INVOKE_STRING",
                            target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V",
                            args = "ldc=rebuildNear")),
            require = 1, allow = 1)
    private void fp2_setupRender_updateVanillaRenderability(ActiveRenderInfo info, ClippingHelper clippingHelper, boolean useCapturedFrustum, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        this.minecraft.getProfiler().push("fp2_vanillaRenderabilityTracker_update");
        this.fp2_vanillaRenderabilityTracker.update(uncheckedCast(this), frameCount);
        this.minecraft.getProfiler().pop();
    }

    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;setupRender(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelper;ZIZ)V",
            at = @At(value = "INVOKE_STRING",
                    target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V",
                    args = "ldc=rebuildNear"),
            require = 1, allow = 1)
    private void fp2_setupRender_prepare(ActiveRenderInfo info, ClippingHelper clippingHelper, boolean useCapturedFrustum, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.level.getProfiler().push("fp2_prepare");
                renderer.prepare((IFrustum) clippingHelper);
                this.level.getProfiler().pop();
            }
        });
    }

    @Unique
    private int fp2_toLayerIndex(RenderType type) {
        if (type == RenderType.solid()) {
            return IFarRenderer.LAYER_SOLID;
        } else if (type == RenderType.cutout()) {
            return IFarRenderer.LAYER_CUTOUT;
        } else if (type == RenderType.translucent()) {
            return IFarRenderer.LAYER_TRANSPARENT;
        } else {
            return -1;
        }
    }

    @Redirect(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderLevel(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getRenderDistance()F"),
            require = 1, allow = 1)
    private float fp2_renderLevel_increaseFogDistance(GameRenderer renderer) {
        IFarClientContext<?, ?> context = fp2().client().currentPlayer().map(IFarPlayerClient::fp2_IFarPlayerClient_activeContext).orElse(null);
        if (context != null) {
            FP2Config config = context.config();
            return config.effectiveRenderDistanceBlocks();
            //TODO: i need a better system for computing this
        }

        return renderer.getRenderDistance();
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderLevel(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
                    shift = At.Shift.AFTER,
                    ordinal = 2),
            require = 1, allow = 1)
    private void fp2_renderLevel_doFP2Render(MatrixStack matrixStack, float partialTicks, long finishTimeNanos, boolean shouldRenderBlockOutline, ActiveRenderInfo activeRenderInfo, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        //immediately after vanilla cutout() layer

        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.level.getProfiler().push("fp2_render_post");

                this.minecraft.getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels > 0);
                renderer.render(this.globalUniformAttributes(activeRenderInfo, matrixStack, projectionMatrix));
                this.minecraft.getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).restoreLastBlurMipmap();

                this.level.getProfiler().pop();
            }
        });
    }

    @Unique
    private GlobalUniformAttributes globalUniformAttributes(ActiveRenderInfo activeRenderInfo, MatrixStack matrixStack, Matrix4f projectionMatrix) {
        GlobalUniformAttributes attributes = new GlobalUniformAttributes();

        { //camera
            this.initModelViewProjectionMatrix(attributes, matrixStack, projectionMatrix);

            double x = activeRenderInfo.getPosition().x();
            double y = activeRenderInfo.getPosition().y();
            double z = activeRenderInfo.getPosition().z();

            attributes.positionFloorX = floorI(x);
            attributes.positionFloorY = floorI(y);
            attributes.positionFloorZ = floorI(z);

            attributes.positionFracX = (float) frac(x);
            attributes.positionFracY = (float) frac(y);
            attributes.positionFracZ = (float) frac(z);
        }

        { //fog
            attributes.fogColorR = ATFogRenderer1_16.getFogRed();
            attributes.fogColorG = ATFogRenderer1_16.getFogGreen();
            attributes.fogColorB = ATFogRenderer1_16.getFogBlue();
            attributes.fogColorA = 1.0f;

            attributes.fogMode = glGetInteger(GL_FOG_MODE);

            attributes.fogDensity = glGetFloat(GL_FOG_DENSITY);
            attributes.fogStart = glGetFloat(GL_FOG_START);
            attributes.fogEnd = glGetFloat(GL_FOG_END);
            attributes.fogScale = 1.0f / (attributes.fogEnd - attributes.fogStart);

            //i can't use glGetBoolean(GL_FOG) to check if fog is enabled because 1.16 turns it on and off again for every chunk section.
            //  instead, i check if fog mode is EXP2 and density is 0, because that's what is configured by FogRenderer.setupNoFog().
            //TODO: figure out if this will still work with OptiFine's option to disable fog
            if (attributes.fogMode == GL_EXP2 && attributes.fogDensity == 0.0f) {
                attributes.fogMode = 0;
            }
        }

        { //misc. GL state
            //in vanilla, both RenderType.CUTOUT and RenderType.CUTOUT_MIPPED are created with setAlphaState(MIDWAY_ALPHA), which corresponds
            //  to an alpha test reference value of 0.5.
            //in OptiFine, both RenderType.CUTOUT and RenderType.CUTOUT_MIPPED are created with setAlphaState(CUTOUT_MIPPED_ALPHA), which
            //  corresponds to an alpha test reference value of 0.1.
            attributes.alphaRefCutout = OFHelper1_16.OF ? 0.1f : 0.5f;
        }

        return attributes;
    }

    @Unique
    private void initModelViewProjectionMatrix(GlobalUniformAttributes attributes, MatrixStack matrixStack, Matrix4f projectionMatrix) {
        ArrayAllocator<float[]> alloc = GlobalAllocators.ALLOC_FLOAT.get();

        float[] modelView = alloc.atLeast(MatrixHelper.MAT4_ELEMENTS);
        float[] projection = alloc.atLeast(MatrixHelper.MAT4_ELEMENTS);
        try {
            //load both matrices into arrays
            matrixStack.last().pose().store(FloatBuffer.wrap(modelView));
            projectionMatrix.store(FloatBuffer.wrap(projection));

            //pre-multiply matrices on CPU to avoid having to do it per-vertex on GPU
            MatrixHelper.multiply4x4(projection, modelView, attributes.modelViewProjectionMatrix);

            //offset the projected points' depth values to avoid z-fighting with vanilla terrain
            MatrixHelper.offsetDepth(attributes.modelViewProjectionMatrix, fp2().client().isReverseZ() ? -0.00001f : 0.00001f);
        } finally {
            alloc.release(projection);
            alloc.release(modelView);
        }
    }

    @Override
    public TerrainRenderingBlockedTracker1_16 fp2_vanillaRenderabilityTracker() {
        return this.fp2_vanillaRenderabilityTracker;
    }
}
