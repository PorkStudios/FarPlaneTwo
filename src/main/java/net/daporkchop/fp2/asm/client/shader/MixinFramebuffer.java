/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.asm.client.shader;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {
    @Shadow
    public int framebufferTextureWidth;
    @Shadow
    public int framebufferTextureHeight;
    @Shadow
    public int depthBuffer;
    @Shadow(remap = false)
    private boolean stencilEnabled;

    private int stencilBuffer = -1;

    @Inject(method = "Lnet/minecraft/client/shader/Framebuffer;createFramebuffer(II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OpenGlHelper;glGenRenderbuffers()I",
                    shift = At.Shift.AFTER))
    private void allocateStencilBuffer(int width, int height, CallbackInfo ci) {
        if (this.stencilEnabled) {
            this.stencilBuffer = glGenRenderbuffers();
        }
    }

    @Redirect(method = "Lnet/minecraft/client/shader/Framebuffer;createFramebuffer(II)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/shader/Framebuffer;stencilEnabled:Z",
                    remap = false))
    private boolean dontDoForgeStencilInit(Framebuffer framebuffer) {
        return false;
    }

    @Inject(method = "Lnet/minecraft/client/shader/Framebuffer;createFramebuffer(II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OpenGlHelper;glFramebufferRenderbuffer(IIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void initializeStencilBuffer(int width, int height, CallbackInfo ci) {
        if (this.stencilEnabled) {
            glBindRenderbuffer(GL_RENDERBUFFER, this.stencilBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, this.framebufferTextureWidth, this.framebufferTextureHeight);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, this.stencilBuffer);

            //re-bind depth buffer to avoid changing behavior
            glBindRenderbuffer(GL_RENDERBUFFER, this.depthBuffer);
        }
    }

    @Inject(method = "Lnet/minecraft/client/shader/Framebuffer;deleteFramebuffer()V",
            at = @At("TAIL"))
    private void deleteStencilBuffer(CallbackInfo ci) {
        if (OpenGlHelper.isFramebufferEnabled() && this.stencilBuffer > -1) {
            glDeleteRenderbuffers(this.stencilBuffer);
            this.stencilBuffer = -1;
        }
    }

    @ModifyConstant(method = "Lnet/minecraft/client/shader/Framebuffer;createFramebuffer(II)V",
            constant = @Constant(intValue = GL_DEPTH_COMPONENT24))
    private int useF32DepthBuffer(int prev) {
        return GL_DEPTH_COMPONENT32F;
    }
}
