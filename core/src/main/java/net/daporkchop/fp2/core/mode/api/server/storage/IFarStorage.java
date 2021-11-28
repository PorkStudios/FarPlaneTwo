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

package net.daporkchop.fp2.core.mode.api.server.storage;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Handles reading and writing of far terrain data.
 *
 * @author DaPorkchop_
 */
public interface IFarStorage<POS extends IFarPos, T extends IFarTile> extends Closeable {
    /**
     * Gets an {@link ITileHandle} for accessing the tile data at the given position.
     *
     * @param pos the position
     * @return an {@link ITileHandle}
     */
    ITileHandle<POS, T> handleFor(@NonNull POS pos);

    void forEachDirtyPos(@NonNull Consumer<POS> callback);

    /**
     * Atomically marks multiple positions as dirty as of the given timestamp.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * return positions.distinct()
     *         .filter(pos -> this.handleFor(pos).markDirty(dirtyTimestamp));
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions      the positions to mark as dirty
     * @param dirtyTimestamp the new dirty timestamp
     * @return the positions for which the operation was able to be applied
     * @see ITileHandle#markDirty(long)
     */
    default Stream<POS> markAllDirty(@NonNull Stream<POS> positions, long dirtyTimestamp) {
        return positions.distinct()
                .filter(pos -> this.handleFor(pos).markDirty(dirtyTimestamp));
    }

    //void markVanillaRenderable(@NonNull Stream<POS> positions);

    /**
     * Closes this storage.
     * <p>
     * If write operations are queued, this method will block until they are completed.
     * <p>
     * After this method has completed, attempting to invoke any methods on this {@link IFarStorage} instance or any {@link ITileHandle}s returned by {@link #handleFor(IFarPos)}
     * will result in undefined behavior.
     */
    @Override
    void close() throws IOException;

    /**
     * Adds a new {@link Listener} which will receive all future notifications.
     *
     * @param listener the {@link Listener} to add
     */
    void addListener(@NonNull Listener<POS, T> listener);

    /**
     * Removes a previously added {@link Listener}, preventing it from receiving any future notifications.
     *
     * @param listener the {@link Listener} to remove
     */
    void removeListener(@NonNull Listener<POS, T> listener);

    /**
     * Listens for changes made to any data stored in a {@link IFarStorage}.
     *
     * @author DaPorkchop_
     */
    interface Listener<POS extends IFarPos, T extends IFarTile> {
        /**
         * Fired when the tiles at the given positions are modified.
         *
         * @param positions a distinct {@link Stream} of the modified positions
         */
        void tilesChanged(@NonNull Stream<POS> positions);

        /**
         * Fired when the tiles at the given positions are marked as dirty.
         * <p>
         * This includes when an already dirty tile's dirty timestamp is modified.
         *
         * @param positions a distinct {@link Stream} of the now dirty positions
         */
        void tilesDirty(@NonNull Stream<POS> positions);
    }
}
