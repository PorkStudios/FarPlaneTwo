/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.asm.debug.client.renderer;

import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputCallback;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlDebugTextUtils;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author DaPorkchop_
 */
@Mixin(GlDebugTextUtils.class)
abstract class MixinGlDebugTextUtils1_16 {
    /*@Shadow
    @Final
    private static List<Integer> DEBUG_LEVELS;

    @Shadow
    @Final
    private static List<Integer> DEBUG_LEVELS_ARB;

    @ModifyVariable(method = "enableDebugCallback(IZ)V",
            at = @At("HEAD"),
            ordinal = 0, argsOnly = true,
            allow = 1, require = 1)
    private static int fp2_debug_enableDebugCallback_enableAllTracing(int glDebugVerbosity) {
        if (OpenGL.DEBUG_OUTPUT) {
            glDebugVerbosity = Math.max(1, Math.max(DEBUG_LEVELS.size(), DEBUG_LEVELS_ARB.size()));
        }
        return glDebugVerbosity;
    }

    @ModifyVariable(method = "enableDebugCallback(IZ)V",
            at = @At("HEAD"),
            ordinal = 0, argsOnly = true,
            allow = 1, require = 1)
    private static boolean fp2_debug_enableDebugCallback_makeTracingSynchronous(boolean synchronous) {
        if (OpenGL.DEBUG_OUTPUT) {
            synchronous = true;
        }
        return synchronous;
    }

    @Inject(method = "enableDebugCallback(IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/KHRDebug;glDebugMessageCallback(Lorg/lwjgl/opengl/GLDebugMessageCallbackI;J)V"),
            allow = 1, require = 1)
    private static void fp2_debug_enableDebugCallback_enableAllDebugMessagesKHR(CallbackInfo ci) {
        if (OpenGL.DEBUG_OUTPUT) {
            KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (IntBuffer) null, true);
        }
    }

    @Inject(method = "enableDebugCallback(IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/ARBDebugOutput;glDebugMessageCallbackARB(Lorg/lwjgl/opengl/GLDebugMessageARBCallbackI;J)V"),
            allow = 1, require = 1)
    private static void fp2_debug_enableDebugCallback_enableAllDebugMessagesARB(CallbackInfo ci) {
        if (OpenGL.DEBUG_OUTPUT) {
            ARBDebugOutput.glDebugMessageControlARB(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (IntBuffer) null, true);
        }
    }*/

    @Inject(method = "enableDebugCallback(IZ)V",
            at = @At("HEAD"),
            cancellable = true,
            allow = 1, require = 1)
    private static void fp2_debug_enableDebugCallback_useFp2Tracing(CallbackInfo ci) {
        if (OpenGL.DEBUG_OUTPUT) {
            GLDebugOutputUtil.configureDebugOutput(OpenGL.forCurrent(), GLDebugOutputCallback.log(LogManager.getLogger("OpenGL Debug Output")::info));
            ci.cancel();
        }
    }

    @Dynamic
    @Inject(method = "printDebugLog(IIIIIJJ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/optifine/Config;isShowGlErrors()Z", remap = false),
            cancellable = true,
            allow = 1, require = 0)
    private static void fp2_debug_printDebugLog_fixOptiFineCrash(int source, int type, int id, int severity, int length, long message, long userParam, CallbackInfo ci) {
        //OptiFine will try to access this when debug output is generated, which will cause a crash if debug output is generated during game startup
        if (Minecraft.getInstance().gui == null) {
            ci.cancel();
        }
    }
}
