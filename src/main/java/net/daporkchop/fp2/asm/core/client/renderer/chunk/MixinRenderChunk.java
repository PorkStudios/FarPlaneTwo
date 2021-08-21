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

package net.daporkchop.fp2.asm.core.client.renderer.chunk;

import net.daporkchop.fp2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk {
    @Shadow
    @Final
    public BlockPos.MutableBlockPos position;

    @Shadow
    @Final
    public RenderGlobal renderGlobal;

    @Inject(
            method = { //compiledChunk is modified in both of these methods
                    "Lnet/minecraft/client/renderer/chunk/RenderChunk;setCompiledChunk(Lnet/minecraft/client/renderer/chunk/CompiledChunk;)V",
                    "Lnet/minecraft/client/renderer/chunk/RenderChunk;stopCompileTask()V"
            },
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;compiledChunk:Lnet/minecraft/client/renderer/chunk/CompiledChunk;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 2)
    private void fp2_setCompiledChunk$$and$$stopCompileTask_notifyVanillaRenderabilityTracker(CallbackInfo ci) {
        //this RenderChunk's compiledChunk instance has been modified, so we should notify the vanilla renderability tracker
        ((IMixinRenderGlobal) this.renderGlobal).fp2_vanillaRenderabilityTracker().update(uncheckedCast(this));
    }
}
