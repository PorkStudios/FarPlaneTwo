/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.client;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.Tile;

/**
 * @author DaPorkchop_
 */
public abstract class VoxelRenderer extends AbstractFarRenderer<TilePos, Tile> {
    public VoxelRenderer(@NonNull IFarClientContext<TilePos, Tile> context) {
        super(context);
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShaderMultidraw extends VoxelRenderer {
        public ShaderMultidraw(@NonNull IFarClientContext<TilePos, Tile> context) {
            super(context);
        }

        @Override
        protected IFarRenderStrategy<TilePos, Tile, ?, ?, ?> strategy0() {
            return new ShaderBasedVoxelRenderStrategy(this);
        }
    }
}
