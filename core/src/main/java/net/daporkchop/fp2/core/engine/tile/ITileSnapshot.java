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

package net.daporkchop.fp2.core.engine.tile;

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;

/**
 * A snapshot of the data stored at a given tile position.
 *
 * @author DaPorkchop_
 */
public interface ITileSnapshot extends ITileMetadata, RefCounted {
    @Override
    int refCnt();

    @Override
    ITileSnapshot retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    /**
     * @return the tile's position
     */
    TilePos pos();

    /**
     * Allocates a {@link Tile} using the given {@link Recycler} and initializes it using the data stored in this snapshot.
     *
     * @param recycler a {@link Recycler} to use for allocating instances of {@link Tile}
     * @return the loaded {@link Tile}, or {@code null} if this snapshot is empty
     */
    Tile loadTile(@NonNull Recycler<Tile> recycler, @NonNull IVariableSizeRecyclingCodec<Tile> codec);

    /**
     * @return whether or not this snapshot's tile data is empty
     */
    boolean isEmpty();

    /**
     * @return this snapshot, with its tile data stored compressed in-memory
     */
    ITileSnapshot compressed();

    /**
     * @return this snapshot, with its tile data stored in-memory without compression
     */
    ITileSnapshot uncompressed();

    /**
     * @return the size of the data stored in this snapshot
     */
    long dataSize();

    DebugStats.TileSnapshot stats();
}
