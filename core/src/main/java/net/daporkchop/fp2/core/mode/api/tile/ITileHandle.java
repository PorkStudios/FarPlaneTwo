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

package net.daporkchop.fp2.core.mode.api.tile;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;

/**
 * @author DaPorkchop_
 */
public interface ITileHandle<POS extends IFarPos, T extends IFarTile> extends ITileMetadata {
    /**
     * @return the tile's position
     */
    POS pos();

    /**
     * @return a snapshot of this tile's current data and metadata, or {@code null} if the tile hasn't been initialized
     */
    ITileSnapshot<POS, T> snapshot();

    /**
     * Atomically sets this tile's contents to the given data.
     * <p>
     * If the new timestamp is less than or equal to the current timestamp, nothing will be modified and the method will return {@code false}.
     * <p>
     * If the new timestamp is greater than or equal to the current dirty timestamp, the dirty timestamp will be cleared.
     *
     * @param metadata the tile's new metadata
     * @param tile     an instance of {@link T} containing the new tile data
     * @return whether or not the operation was able to be applied
     */
    boolean set(@NonNull ITileMetadata metadata, @NonNull T tile);

    /**
     * @return the timestamp at which this tile was last marked as dirty, or {@link #TIMESTAMP_BLANK} if it isn't
     */
    long dirtyTimestamp();

    /**
     * Atomically marks this tile as dirty as of the given timestamp.
     * <p>
     * If the new dirty timestamp is less than or equal to the current timestamp or the current dirty timestamp, or the tile hasn't been initialized yet, nothing will be
     * modified and the method will return {@code false}.
     *
     * @param dirtyTimestamp the new dirty timestamp
     * @return whether or not the operation was able to be applied
     */
    boolean markDirty(long dirtyTimestamp);

    /**
     * Un-marks this position as being dirty.
     * <p>
     * If the tile is not already dirty, this method does nothing.
     *
     * @return whether or not the operation was able to be applied
     */
    boolean clearDirty();

    /**
     * @return whether or not any vanilla terrain which could affect the contents of this tile exists
     */
    boolean anyVanillaExists();
}
