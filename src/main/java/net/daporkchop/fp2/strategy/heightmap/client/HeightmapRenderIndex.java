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

package net.daporkchop.fp2.strategy.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderIndex;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;

import static net.daporkchop.fp2.strategy.heightmap.client.HeightmapRenderHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
class HeightmapRenderIndex extends AbstractFarRenderIndex<HeightmapPos, HeightmapRenderTile, HeightmapRenderIndex> {
    @Override
    public boolean add(@NonNull HeightmapRenderTile tile) {
        if (tile.hasAddress()) {
            this.ensureWritable(4 * 8);

            for (HeightmapRenderTile t : tile.neighbors()) {
                this.writeTile(t);
            }

            HeightmapRenderTile parent = tile.parent();
            if (parent != null) {
                HeightmapPos pos = tile.pos();
                int xLSB = pos.x() & 1;
                int zLSB = pos.z() & 1;
                HeightmapRenderTile[] neighbors = parent.neighbors();

                this.writeTile(parent);
                this.writeTile(neighbors[zLSB]);
                this.writeTile(neighbors[xLSB << 1]);
                this.writeTile(neighbors[(xLSB << 1) | zLSB]);
            } else {
                for (int i = 0; i < 4; i++) {
                    this.writeTile(null);
                }
            }

            this.size++;
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void writeTile(HeightmapRenderTile tile) {
        if (tile != null && tile.hasAddress()) {
            HeightmapPos pos = tile.pos();
            this.buffer.put(pos.x()).put(pos.z()).put(pos.level()).put(toInt(tile.address() / HEIGHTMAP_RENDER_SIZE));
        } else {
            this.buffer.put(0).put(0).put(0).put(0);
        }
    }
}
