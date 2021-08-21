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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import net.daporkchop.fp2.util.datastructure.Datastructures;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;

/**
 * Keeps track of which chunk sections are able to be rendered by vanilla, and therefore should not be rendered by fp2 at detail level 0.
 *
 * @author DaPorkchop_
 */
public class VanillaRenderabilityTracker extends AbstractRefCounted {
    protected static NDimensionalIntSet newSet(int dimensions) {
        return Datastructures.INSTANCE.nDimensionalIntSet().threadSafe(false).dimensions(dimensions).build();
    }

    protected final NDimensionalIntSet rendered = newSet(3);
    protected final NDimensionalIntSet blockedPositions = newSet(3);

    @Override
    public VanillaRenderabilityTracker retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.rendered.release();
        this.blockedPositions.release();
    }

    public synchronized void update(@NonNull RenderChunk chunk) {
        BlockPos pos = chunk.getPosition();
        int chunkX = pos.getX() >> 4;
        int chunkY = pos.getY() >> 4;
        int chunkZ = pos.getZ() >> 4;

        CompiledChunk compiledChunk = chunk.getCompiledChunk();
        if (compiledChunk != CompiledChunk.DUMMY) {
            if (this.rendered.add(chunkX, chunkY, chunkZ)) {
                this.recheckBase(chunkX, chunkY, chunkZ);
            }
        } else {
            if (this.rendered.remove(chunkX, chunkY, chunkZ)) {
                this.recheckBase(chunkX, chunkY, chunkZ);
            }
        }
    }

    protected void recheckBase(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    this.recheck(x + dx, y + dy, z + dz);
                }
            }
        }
    }

    protected void recheck(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!this.rendered.contains(x + dx, y + dy, z + dz)) {
                        this.blockedPositions.remove(x, y, z);
                        return;
                    }
                }
            }
        }

        this.blockedPositions.add(x, y, z);
    }

    public boolean blocksFP2Render(int x, int y, int z) {
        return this.blockedPositions.contains(x, y, z);
    }
}
