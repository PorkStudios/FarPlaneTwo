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

package net.daporkchop.fp2.strategy.voxel.client;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderCache;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;

import static net.daporkchop.fp2.strategy.voxel.client.VoxelRenderHelper.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderCache extends AbstractFarRenderCache<VoxelPos, VoxelPiece, VoxelRenderTile, VoxelRenderIndex> {
    public VoxelRenderCache(@NonNull VoxelRenderer renderer) {
        super(renderer, new VoxelRenderIndex(VOXEL_RENDER_SIZE), VOXEL_RENDER_SIZE);
    }

    @Override
    public VoxelRenderTile createTile(VoxelRenderTile parent, @NonNull VoxelPos pos) {
        return new VoxelRenderTile(this, parent, pos);
    }

    @Override
    public void tileAdded(@NonNull VoxelRenderTile tile) {
        int x = tile.pos().x();
        int y = tile.pos().y();
        int z = tile.pos().z();
        int level = tile.pos().level();

        tile.neighbors()[0] = tile;
        tile.neighbors()[1] = this.getTile(new VoxelPos(x, y, z + 1, level));
        tile.neighbors()[2] = this.getTile(new VoxelPos(x, y + 1, z, level));
        tile.neighbors()[3] = this.getTile(new VoxelPos(x, y + 1, z + 1, level));
        tile.neighbors()[4] = this.getTile(new VoxelPos(x + 1, y, z, level));
        tile.neighbors()[5] = this.getTile(new VoxelPos(x + 1, y, z + 1, level));
        tile.neighbors()[6] = this.getTile(new VoxelPos(x + 1, y + 1, z, level));
        tile.neighbors()[7] = this.getTile(new VoxelPos(x + 1, y + 1, z + 1, level));

        VoxelRenderTile t;
        if ((t = this.getTile(new VoxelPos(x, y, z - 1, level))) != null) {
            t.neighbors()[1] = tile;
        }
        if ((t = this.getTile(new VoxelPos(x, y - 1, z, level))) != null) {
            t.neighbors()[2] = tile;
        }
        if ((t = this.getTile(new VoxelPos(x, y - 1, z - 1, level))) != null) {
            t.neighbors()[3] = tile;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y, z, level))) != null) {
            t.neighbors()[4] = tile;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y, z - 1, level))) != null) {
            t.neighbors()[5] = tile;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y - 1, z, level))) != null) {
            t.neighbors()[6] = tile;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y - 1, z - 1, level))) != null) {
            t.neighbors()[7] = tile;
        }
    }

    @Override
    public void tileRemoved(@NonNull VoxelRenderTile tile) {
        int x = tile.pos().x();
        int y = tile.pos().y();
        int z = tile.pos().z();
        int level = tile.pos().level();

        VoxelRenderTile t;
        if ((t = this.getTile(new VoxelPos(x, y, z - 1, level))) != null) {
            t.neighbors()[1] = null;
        }
        if ((t = this.getTile(new VoxelPos(x, y - 1, z, level))) != null) {
            t.neighbors()[2] = null;
        }
        if ((t = this.getTile(new VoxelPos(x, y - 1, z - 1, level))) != null) {
            t.neighbors()[3] = null;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y, z, level))) != null) {
            t.neighbors()[4] = null;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y, z - 1, level))) != null) {
            t.neighbors()[5] = null;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y - 1, z, level))) != null) {
            t.neighbors()[6] = null;
        }
        if ((t = this.getTile(new VoxelPos(x - 1, y - 1, z - 1, level))) != null) {
            t.neighbors()[7] = null;
        }
    }
}