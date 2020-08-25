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
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderIndex;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderIndex extends AbstractFarRenderIndex<VoxelPos, VoxelRenderTile, VoxelRenderIndex> {
    protected final int bakedSize;

    public VoxelRenderIndex(int bakedSize) {
        this.bakedSize = positive(bakedSize, "bakedSize");
    }

    @Override
    public boolean add(@NonNull VoxelRenderTile tile) {
        if (!tile.hasAddress()) {
            return false;
        }

        //this.ensureWritable(8 * (8 * 2));
        this.ensureWritable(8 * 8);

        for (VoxelRenderTile t : tile.neighbors()) {
            this.writeTile(t);
        }

        //TODO: LoD
        /*VoxelRenderTile parent = tile.parent();
        if (parent != null) {
            VoxelPos pos = tile.pos();
            int xLSB = (pos.x() & 1) << 2;
            int yLSB = (pos.y() & 1) << 1;
            int zLSB = pos.z() & 1;
            VoxelRenderTile[] neighbors = parent.neighbors();

            this.writeTile(parent);
            this.writeTile(neighbors[zLSB]);
            this.writeTile(neighbors[yLSB]);
            this.writeTile(neighbors[yLSB | zLSB]);
            this.writeTile(neighbors[xLSB]);
            this.writeTile(neighbors[xLSB | zLSB]);
            this.writeTile(neighbors[xLSB | yLSB]);
            this.writeTile(neighbors[xLSB | yLSB | zLSB]);
        } else {
            for (int i = 0; i < 8; i++) {
                this.writeTile(null);
            }
        }*/

        this.size++;
        return true;
    }

    @Override
    protected void writeTile(VoxelRenderTile tile) {
        if (tile != null && tile.hasAddress()) {
            VoxelPos pos = tile.pos();
            this.buffer.put(pos.x()).put(pos.y()).put(pos.z()).put(pos.level()).put(toInt(tile.address() / this.bakedSize))
                    .put(0).put(0).put(0);
        } else {
            this.buffer.put(0).put(0).put(0).put(0).put(0)
                    .put(0).put(0).put(0);
        }
    }
}
