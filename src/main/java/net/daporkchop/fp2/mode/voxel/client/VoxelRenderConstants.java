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

package net.daporkchop.fp2.mode.voxel.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.mode.voxel.VoxelDirectPosAccess;

/**
 * Constant values used throughout the voxel render code.
 *
 * @author DaPorkchop_
 */
@UtilityClass
class VoxelRenderConstants {
    //
    // off-heap structs layouts
    //TODO: figure out how to keep intellij from rearranging this area when reformatting
    //

    /*
     * struct Tile {
     *   Pos pos;
     *   RenderData renderData;
     * };
     */

    public final long _TILE_POS_OFFSET = 0L;
    public final long _TILE_RENDERDATA_OFFSET = _TILE_POS_OFFSET + VoxelDirectPosAccess._SIZE;

    public long _tile_pos(long tile) {
        return tile + _TILE_POS_OFFSET;
    }

    public long _tile_renderData(long tile) {
        return tile + _TILE_RENDERDATA_OFFSET;
    }
}
