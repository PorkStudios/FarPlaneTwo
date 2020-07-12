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

package net.daporkchop.fp2.strategy.common;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.RenderStrategy;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
public interface IFarContext {
    /**
     * Initializes this context.
     *
     * @param strategy the new {@link RenderStrategy} to use
     */
    void fp2_init(@NonNull RenderStrategy strategy);

    /**
     * @return the current {@link RenderStrategy}
     * @throws IllegalStateException if this context has not yet been initialized
     */
    RenderStrategy fp2_strategy();

    /**
     * @return the current {@link IFarWorld}
     * @throws IllegalStateException if this context has not yet been initialized
     */
    IFarWorld fp2_world();

    /**
     * @return the current {@link IFarPlayerTracker}
     * @throws IllegalStateException if this context has not yet been initialized
     */
    IFarPlayerTracker fp2_tracker();

    /**
     * @return the current {@link TerrainRenderer}, or {@code null} if this context has not yet been initialized
     */
    @SideOnly(Side.CLIENT)
    TerrainRenderer fp2_renderer();
}
