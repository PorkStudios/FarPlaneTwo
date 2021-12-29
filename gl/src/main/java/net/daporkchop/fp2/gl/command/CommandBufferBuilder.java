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

package net.daporkchop.fp2.gl.command;

import lombok.NonNull;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;

/**
 * Builder for {@link CommandBuffer}s.
 *
 * @author DaPorkchop_
 */
public interface CommandBufferBuilder {
    //
    // STATE
    //

    CommandBufferBuilder framebufferClear(@NonNull FramebufferLayer... layers);

    CommandBufferBuilder blendEnable();

    CommandBufferBuilder blendDisable();

    CommandBufferBuilder blendFunctionSrc(@NonNull BlendFactor rgb, @NonNull BlendFactor a);

    CommandBufferBuilder blendFunctionDst(@NonNull BlendFactor rgb, @NonNull BlendFactor a);

    CommandBufferBuilder blendOp(@NonNull BlendOp rgb, @NonNull BlendOp a);

    CommandBufferBuilder blendColor(float r, float g, float b, float a);

    default CommandBufferBuilder blendColor(int argb) {
        final float F = 1.0f / 255.0f;
        return this.blendColor(((argb >>> 16) & 0xFF) * F, ((argb >>> 8) & 0xFF) * F, (argb & 0xFF) * F, (argb >>> 24) * F);
    }

    CommandBufferBuilder colorClear(float r, float g, float b, float a);

    default CommandBufferBuilder colorClear(int argb) {
        final float F = 1.0f / 255.0f;
        return this.colorClear(((argb >>> 16) & 0xFF) * F, ((argb >>> 8) & 0xFF) * F, (argb & 0xFF) * F, (argb >>> 24) * F);
    }

    CommandBufferBuilder colorWrite(boolean r, boolean g, boolean b, boolean a);

    CommandBufferBuilder cullEnable();

    CommandBufferBuilder cullDisable();

    CommandBufferBuilder depthEnable();

    CommandBufferBuilder depthDisable();

    CommandBufferBuilder depthClear(double value);

    CommandBufferBuilder depthWrite(boolean mask);

    CommandBufferBuilder depthCompare(@NonNull Compare compare);

    CommandBufferBuilder stencilEnable();

    CommandBufferBuilder stencilDisable();

    CommandBufferBuilder stencilClear(int value);

    CommandBufferBuilder stencilWriteMask(int writeMask);

    CommandBufferBuilder stencilCompareMask(int compareMask);

    CommandBufferBuilder stencilReference(int reference);

    CommandBufferBuilder stencilCompare(@NonNull Compare compare);

    /**
     * Sets the operations used to modify the value in the stencil buffer for a given fragment.
     *
     * @param fail      the action performed if the stencil test fails
     * @param pass      the action performed if both the stencil test and depth test pass
     * @param depthFail the action performed if the stencil test passes, but the depth test fails
     */
    CommandBufferBuilder stencilOperation(@NonNull StencilOperation fail, @NonNull StencilOperation pass, @NonNull StencilOperation depthFail);

    //
    // DRAWING
    //

    CommandBufferBuilder drawArrays(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DrawBinding binding, int first, int count);

    CommandBufferBuilder drawList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DrawList<?> list);

    CommandBufferBuilder drawList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DrawList<?> list, @NonNull GLBitSet selectionMask);

    //
    // MISC
    //

    /**
     * Executes the given {@link CommandBuffer}.
     */
    CommandBufferBuilder execute(@NonNull CommandBuffer buffer);

    /**
     * Places an ordering barrier at the current stage.
     * <p>
     * All commands issued before a barrier are guaranteed to have been executed before the barrier can be passed, and subsequent commands will not begin until after the barrier
     * is passed.
     */
    CommandBufferBuilder barrier();

    //
    // BUILDER
    //

    /**
     * @return the constructed {@link CommandBuffer}
     */
    CommandBuffer build();
}
