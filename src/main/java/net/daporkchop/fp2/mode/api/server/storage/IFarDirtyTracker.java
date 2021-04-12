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

package net.daporkchop.fp2.mode.api.server.storage;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.lib.primitive.lambda.ObjLongConsumer;

import java.util.stream.Stream;

/**
 * Tracks the positions in a world that have been scheduled for updating.
 * <p>
 * All operations are both thread-safe and atomic.
 *
 * @author DaPorkchop_
 */
public interface IFarDirtyTracker<POS extends IFarPos> {
    /**
     * Marks the given tile as dirty.
     * <p>
     * The operation will be successful if the tile was not previously marked as dirty, or was already marked as dirty but with an older timestamp.
     *
     * @param pos       the tile's position
     * @param timestamp the timestamp that the tile was marked at
     * @return whether or not the operation was successful
     */
    boolean markDirty(@NonNull POS pos, long timestamp);

    /**
     * Marks all of the given tiles as dirty.
     * <p>
     * For an individual tile, the operation will be successful if the tile was not previously marked as dirty, or was already marked as dirty but with an older timestamp.
     *
     * @param positions the tiles' positions
     * @param timestamp the timestamp that the tiles were marked at
     * @return a {@link Stream} containing all the positions that were added successfully
     */
    default Stream<POS> markDirty(@NonNull Stream<POS> positions, long timestamp) {
        return positions.filter(pos -> this.markDirty(pos, timestamp));
    }

    /**
     * Gets the timestamp at which the given tile was marked as dirty.
     *
     * @param pos the tile's position
     * @return the timestamp at which the given tile was marked as dirty, or {@code -1L} if the tile isn't dirty
     */
    long dirtyTimestamp(@NonNull POS pos);

    /**
     * Unmarks the given position as dirty.
     * <p>
     * The operation will be successful if the tile was previously marked as dirty with exactly the same timestamp.
     *
     * @param pos       the tile's position
     * @param timestamp the expected timestamp
     * @return whether or not the operation was able to be applied successfully
     */
    boolean clearDirty(@NonNull POS pos, long timestamp);

    /**
     * Runs the given function for every position that has been marked as dirty.
     *
     * @param callback the function to run
     */
    void forEachDirtyPos(@NonNull ObjLongConsumer<POS> callback);
}
