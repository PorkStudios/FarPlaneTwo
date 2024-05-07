/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render;

import lombok.Getter;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * @author DaPorkchop_
 */
public abstract class TerrainRenderingBlockedTracker implements AutoCloseable {
    protected static final long HEADERS_OFFSET = 0L;
    protected static final long FLAGS_OFFSET = HEADERS_OFFSET + 2L * (4 * Integer.BYTES);

    protected final DirectMemoryAllocator alloc = new DirectMemoryAllocator();

    protected final OpenGL gl;
    @Getter
    protected final GLMutableBuffer glBuffer;

    protected int offsetX;
    protected int offsetY;
    protected int offsetZ;
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;

    protected long sizeBytes;
    protected long addr;

    public TerrainRenderingBlockedTracker(FP2Client client) {
        this.gl = client.gl();
        this.glBuffer = GLMutableBuffer.create(this.gl);
    }

    @Override
    public void close() {
        this.glBuffer.close();

        this.alloc.free(this.addr);
        this.alloc.close();
    }

    /**
     * Checks whether or not the chunk section at the given coordinates.
     *
     * @param chunkX the chunk section's X coordinate
     * @param chunkY the chunk section's Y coordinate
     * @param chunkZ the chunk section's Z coordinate
     * @return whether or not vanilla terrain at the given chunk section would prevent us from rendering level-0 FP2 terrain
     */
    public final boolean renderingBlocked(int chunkX, int chunkY, int chunkZ) {
        int x = chunkX + this.offsetX;
        int y = chunkY + this.offsetY;
        int z = chunkZ + this.offsetZ;

        if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY || z < 0 || z >= this.sizeZ) {
            return false;
        }

        int idx = (x * this.sizeY + y) * this.sizeZ + z;
        //this is carefully crafted so that HotSpot C2 on OpenJDK 8 on x86_64 can optimize this into just three instructions
        // by taking advantage of complex addressing
        return (PUnsafe.getInt(this.addr + FLAGS_OFFSET + ((long) idx >> 5 << 2)) & (1 << idx)) != 0;
    }
}
