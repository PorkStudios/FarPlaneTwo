/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
import net.daporkchop.fp2.core.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles reading and writing of far terrain data.
 *
 * @author DaPorkchop_
 */
public interface IFarTileStorage<POS extends IFarPos, T extends IFarTile> extends Closeable {
    /**
     * Gets an {@link ITileHandle} for accessing the tile data at the given position.
     *
     * @param pos the position
     * @return an {@link ITileHandle}
     */
    ITileHandle<POS, T> handleFor(@NonNull POS pos);

    /**
     * Takes snapshots of multiple tiles' data and metadata.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * List<ITileSnapshot<POS, T>> out = new ArrayList<>(positions.size());
     * positions.forEach(pos -> out.add(this.handleFor(pos).snapshot()));
     * return out;
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions the positions of the tiles to take snapshots of
     * @return snapshots of the tiles'
     * @see ITileHandle#snapshot()
     */
    default List<ITileSnapshot<POS, T>> multiSnapshot(@NonNull List<POS> positions) {
        List<ITileSnapshot<POS, T>> out = new ArrayList<>(positions.size());
        positions.forEach(pos -> out.add(this.handleFor(pos).snapshot()));
        return out;
    }

    /**
     * Gets multiple tiles' timestamps.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * LongList out = new LongArrayList(positions.size());
     * positions.forEach(pos -> out.add(this.handleFor(pos).timestamp()));
     * return out;
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions the positions of the tiles to get the timestamps of
     * @return the tiles' timestamps
     * @see ITileHandle#timestamp()
     */
    default LongList multiTimestamp(@NonNull List<POS> positions) {
        LongList out = new LongArrayList(positions.size());
        positions.forEach(pos -> out.add(this.handleFor(pos).timestamp()));
        return out;
    }

    /**
     * Sets multiple tiles' contents to the given data.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * BitSet out = new BitSet(positions.size());
     * for (int i = 0; i < positions.size(); i++) {
     *     out.set(i, this.handleFor(positions.get(i)).set(metadatas.get(i), tiles.get(i)));
     * }
     * return out;
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions the positions of the tiles to set the contents of
     * @param metadatas the new {@link ITileMetadata}s to use
     * @param tiles     the new tile datas to use
     * @return a {@link BitSet} indicating the tiles for which the operation was able to be applied
     * @see ITileHandle#set(ITileMetadata, IFarTile)
     */
    default BitSet multiSet(@NonNull List<POS> positions, @NonNull List<ITileMetadata> metadatas, @NonNull List<T> tiles) {
        BitSet out = new BitSet(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            out.set(i, this.handleFor(positions.get(i)).set(metadatas.get(i), tiles.get(i)));
        }
        return out;
    }

    /**
     * Gets the timestamps at which multiple tiles were last marked as dirty.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * LongList out = new LongArrayList(positions.size());
     * positions.forEach(pos -> out.add(this.handleFor(pos).dirtyTimestamp()));
     * return out;
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions the positions of the tiles to get the dirty timestamps of
     * @return the timestamps at which multiple tiles were last marked as dirty
     * @see ITileHandle#dirtyTimestamp()
     */
    default LongList multiDirtyTimestamp(@NonNull List<POS> positions) {
        LongList out = new LongArrayList(positions.size());
        positions.forEach(pos -> out.add(this.handleFor(pos).dirtyTimestamp()));
        return out;
    }

    /**
     * Marks multiple tiles as dirty as of the given timestamp.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * BitSet out = new BitSet(positions.size());
     * for (int i = 0; i < positions.size(); i++) {
     *     out.set(i, this.handleFor(positions.get(i)).markDirty(dirtyTimestamp));
     * }
     * return out;
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions      the positions of the tiles to mark as dirty
     * @param dirtyTimestamp the timestamp as of which the tiles are dirty
     * @return a {@link BitSet} indicating the tiles for which the operation was able to be applied
     * @see ITileHandle#markDirty(long)
     */
    default BitSet multiMarkDirty(@NonNull List<POS> positions, long dirtyTimestamp) {
        BitSet out = new BitSet(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            out.set(i, this.handleFor(positions.get(i)).markDirty(dirtyTimestamp));
        }
        return out;
    }

    /**
     * Un-marks multiple tiles as dirty.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * BitSet out = new BitSet(positions.size());
     * for (int i = 0; i < positions.size(); i++) {
     *     out.set(i, this.handleFor(positions.get(i)).clearDirty());
     * }
     * return out;
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param positions the positions of the tiles to un-mark as dirty
     * @return a {@link BitSet} indicating the tiles for which the operation was able to be applied
     * @see ITileHandle#clearDirty()
     */
    default BitSet multiClearDirty(@NonNull List<POS> positions) {
        BitSet out = new BitSet(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            out.set(i, this.handleFor(positions.get(i)).clearDirty());
        }
        return out;
    }

    /**
     * Closes this storage.
     * <p>
     * If write operations are queued, this method will block until they are completed.
     * <p>
     * After this method has completed, attempting to invoke any methods on this {@link IFarTileStorage} instance or any {@link ITileHandle}s returned by {@link #handleFor(IFarPos)}
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
     * Listens for changes made to any data stored in a {@link IFarTileStorage}.
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
