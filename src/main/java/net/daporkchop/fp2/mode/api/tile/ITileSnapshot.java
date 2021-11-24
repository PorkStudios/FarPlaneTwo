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

package net.daporkchop.fp2.mode.api.tile;

import lombok.NonNull;
import net.daporkchop.fp2.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.util.SimpleRecycler;
import net.daporkchop.fp2.util.annotation.DebugOnly;

/**
 * A snapshot of the data stored at a given tile position.
 *
 * @author DaPorkchop_
 */
public interface ITileSnapshot<POS extends IFarPos, T extends IFarTile> extends ITileMetadata {
    /**
     * @return the tile's position
     */
    POS pos();

    /**
     * Allocates a {@link T} using the given {@link SimpleRecycler} and initializes it using the data stored in this snapshot.
     *
     * @param recycler a {@link SimpleRecycler} to use for allocating instances of {@link T}
     * @return the loaded {@link T}, or {@code null} if this snapshot is empty
     */
    T loadTile(@NonNull SimpleRecycler<T> recycler);

    /**
     * @return whether or not this snapshot's tile data is empty
     */
    boolean isEmpty();

    /**
     * @return this snapshot, with its tile data stored compressed in-memory
     */
    ITileSnapshot<POS, T> compressed();

    /**
     * @return this snapshot, with its tile data stored in-memory without compression
     */
    ITileSnapshot<POS, T> uncompressed();

    @DebugOnly
    DebugStats.TileSnapshot stats();
}
