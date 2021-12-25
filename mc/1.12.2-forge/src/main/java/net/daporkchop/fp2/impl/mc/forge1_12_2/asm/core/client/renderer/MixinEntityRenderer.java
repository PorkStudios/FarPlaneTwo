/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.client.renderer;

import net.daporkchop.fp2.core.client.MatrixHelper;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.BufferUtils;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * @author DaPorkchop_
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private float farPlaneDistance;

    @Unique
    private final FloatBuffer fp2_tempMatrix = BufferUtils.createFloatBuffer(MatrixHelper.MAT4_ELEMENTS);

    @Inject(method = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorldPass(IFJ)V",
            at = @At("HEAD"))
    private void fp2_renderWorldPass_pre(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        fp2().client().enableReverseZ();
    }

    //use reversed-z projection with infinite zFar everywhere

    @Redirect(method = "*",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void fp2_$everything$_dontUseGluPerspective(float fov, float aspect, float zNear, float zFar) {
        MatrixHelper.reversedZ(this.fp2_tempMatrix, fov, aspect, zNear);
        glMultMatrix(this.fp2_tempMatrix);
    }

    //set farPlaneDistance to the value in config

    @Redirect(method = "Lnet/minecraft/client/renderer/EntityRenderer;setupCameraTransform(FI)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;farPlaneDistance:F",
                    opcode = Opcodes.PUTFIELD))
    private void fp2_setupCameraTransform_increaseFarPlaneDistance(EntityRenderer renderer, float farPlaneDistance) {
        IFarClientContext<?, ?> context = ((IFarPlayerClient) this.mc.getConnection()).fp2_IFarPlayerClient_activeContext();

        if (context != null) {
            FP2Config config = context.config();
            farPlaneDistance = config.effectiveRenderDistanceBlocks();
            //TODO: i need a better system for computing this
        }
        this.farPlaneDistance = farPlaneDistance;
    }

    //optifine changes this value for us, but we need to manually change it if optifine isn't around

    @ModifyConstant(method = "Lnet/minecraft/client/renderer/EntityRenderer;enableLightmap()V",
            constant = @Constant(intValue = GL_CLAMP),
            allow = 2)
    private int fp2_enableLightmap_setLightmapEdgeClampModeToEmulateOptifine(int value) {
        return GL_CLAMP_TO_EDGE;
    }
}
