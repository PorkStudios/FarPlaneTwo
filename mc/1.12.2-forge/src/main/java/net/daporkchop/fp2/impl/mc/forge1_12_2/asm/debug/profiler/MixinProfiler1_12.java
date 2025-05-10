/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.debug.profiler;

import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputUtil;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.profiler.IDebugMixinProfiler1_12;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Mixin(Profiler.class)
abstract class MixinProfiler1_12 implements IDebugMixinProfiler1_12 {
    @Shadow public boolean profilingEnabled;
    @Unique
    private OpenGL fp2_gl;

    @Override
    public final void fp2_debug_gl(OpenGL gl) {
        this.fp2_gl = gl;
    }

    @Override
    public final void fp2_debug_beginFrame() {
        if (this.fp2_gl != null) {
            GLDebugOutputUtil.tryPopAllDebugGroups(this.fp2_gl);
        }
    }

    @Inject(method = "startSection(Ljava/lang/String;)V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_debug_startSection_tryPushDebugGroup(String name, CallbackInfo ci) {
        if (this.fp2_gl != null) {
            GLDebugOutputUtil.tryPushDebugGroupManual(this.fp2_gl, GL_DEBUG_SOURCE_APPLICATION, 0, name);
        }
    }

    @Inject(method = "func_194340_a(Ljava/util/function/Supplier;)V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_debug_startSectionSupplier_tryPushDebugGroup(Supplier<String> nameSupplier, CallbackInfo ci) {
        if (!this.profilingEnabled && this.fp2_gl != null) {
            GLDebugOutputUtil.tryPushDebugGroupManual(this.fp2_gl, GL_DEBUG_SOURCE_APPLICATION, 0, nameSupplier.get());
        }
    }

    @Inject(method = "endSection()V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_debug_endSection_tryPushDebugGroup(CallbackInfo ci) {
        if (this.fp2_gl != null) {
            GLDebugOutputUtil.tryPopDebugGroupManual(this.fp2_gl);
        }
    }
}
