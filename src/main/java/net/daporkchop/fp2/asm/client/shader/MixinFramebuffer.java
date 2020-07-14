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

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {
    @ModifyConstant(method = "Lnet/minecraft/client/shader/Framebuffer;createFramebuffer(II)V",
            constant = @Constant(intValue = GL_DEPTH_COMPONENT24))
    private int useF32DepthBuffer(int prev) {
        return GL_DEPTH_COMPONENT32F;
    }

    /*@Shadow
    public int framebufferWidth;

    @Shadow
    public int framebufferHeight;

    @Shadow
    public int framebufferTextureWidth;

    @Shadow
    public int framebufferTextureHeight;

    @Shadow
    public abstract void framebufferClear();

    @Shadow
    public int framebufferObject;

    @Shadow
    public int framebufferTexture;

    @Shadow
    public boolean useDepth;

    @Shadow
    public int depthBuffer;

    @Shadow
    public abstract void setFramebufferFilter(int framebufferFilterIn);

    @Shadow
    private boolean stencilEnabled;

    @Shadow
    public abstract void unbindFramebufferTexture();*/

    /**
     * @author DaPorkchop_
     */
    /*@Overwrite
    public void createFramebuffer(int width, int height) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.framebufferTextureWidth = width;
        this.framebufferTextureHeight = height;

        if (!OpenGlHelper.isFramebufferEnabled()) {
            this.framebufferClear();
        } else {
            this.framebufferObject = OpenGlHelper.glGenFramebuffers();
            this.framebufferTexture = TextureUtil.glGenTextures();

            if (this.useDepth) {
                this.depthBuffer = OpenGlHelper.glGenRenderbuffers();
            }

            this.setFramebufferFilter(GL_NEAREST);
            GlStateManager.bindTexture(this.framebufferTexture);
            GlStateManager.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
                    this.framebufferTextureWidth, this.framebufferTextureHeight,
                    0, GL_RGBA, GL_UNSIGNED_BYTE, null);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, this.framebufferTexture, 0);

            if (this.useDepth) {
                OpenGlHelper.glBindRenderbuffer(OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
                if (!this.stencilEnabled) {
                    OpenGlHelper.glRenderbufferStorage(OpenGlHelper.GL_RENDERBUFFER,
                            //GL_DEPTH_COMPONENT24,
                            GL_DEPTH_COMPONENT32F,
                            this.framebufferTextureWidth, this.framebufferTextureHeight);
                    OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT,
                            OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
                } else {
                    new RuntimeException().printStackTrace(System.err);*/
                    /*OpenGlHelper.glRenderbufferStorage(OpenGlHelper.GL_RENDERBUFFER, org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
                            this.framebufferTextureWidth, this.framebufferTextureHeight);
                    OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                            OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
                    OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
                            OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);*//*
                }
            }

            this.framebufferClear();
            this.unbindFramebufferTexture();
        }
    }*/
}
