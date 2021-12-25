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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.region;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import lombok.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * {@link OutputStream} which writes data to a specific position in a {@link ThreadSafeRegionFileCache}.
 *
 * @author DaPorkchop_
 */
public class RegionByteBufOutput extends ByteBufOutputStream {
    protected final ThreadSafeRegionFileCache target;
    protected final Path regionDir;
    protected final int chunkX;
    protected final int chunkZ;

    public RegionByteBufOutput(@NonNull ThreadSafeRegionFileCache target, @NonNull Path regionDir, int chunkX, int chunkZ) {
        super(ByteBufAllocator.DEFAULT.buffer());

        this.target = target;
        this.regionDir = regionDir;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public void close() throws IOException {
        try {
            this.target.write(this.regionDir, this.chunkX, this.chunkZ, this.buffer());
        } finally {
            this.buffer().release();
        }
    }
}
