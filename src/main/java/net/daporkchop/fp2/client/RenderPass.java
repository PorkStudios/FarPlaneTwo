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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import net.minecraft.util.BlockRenderLayer;

/**
 * {@link BlockRenderLayer} with some additional values.
 *
 * @author DaPorkchop_
 */
public enum RenderPass {
    /**
     * Called immediately before the {@code "prepareterrain"} profiler section begins.
     */
    PRE,
    /**
     * Called immediately before {@link BlockRenderLayer#SOLID} is rendered.
     */
    SOLID,
    /**
     * Called immediately before {@link BlockRenderLayer#CUTOUT_MIPPED} is rendered.
     */
    CUTOUT_MIPPED,
    /**
     * Called immediately before {@link BlockRenderLayer#CUTOUT} is rendered.
     */
    CUTOUT,
    /**
     * Called immediately before {@link BlockRenderLayer#TRANSLUCENT} is rendered.
     */
    TRANSLUCENT,
    /**
     * Called immediately before the check for whether or not the {@code "aboveClouds"} profiler section should run.
     */
    POST;

    private static final RenderPass[] FROM_VANILLA = {
            SOLID,
            CUTOUT_MIPPED,
            CUTOUT,
            TRANSLUCENT
    };

    public static RenderPass fromVanilla(@NonNull BlockRenderLayer vanilla)   {
        return FROM_VANILLA[vanilla.ordinal()];
    }

    public final String profilerSectionName = ("fp2_render_" + this.name().toLowerCase()).intern();
}
