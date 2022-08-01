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
 */

package net.daporkchop.fp2.core.mode.api.client;

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.lib.unsafe.capability.Releasable;

import java.util.stream.Stream;

/**
 * Client-side, in-memory cache for loaded tiles.
 *
 * @author DaPorkchop_
 */
public interface IFarTileCache<POS extends IFarPos, T extends IFarTile> extends Releasable {
    /**
     * Adds the given tile into the cache.
     * <p>
     * Ownership of the tile is transferred to the cache.
     *
     * @param tile the tile to add
     */
    void receiveTile(@NonNull ITileSnapshot<POS, T> tile);

    void unloadTile(@NonNull POS pos);

    /**
     * Adds a new {@link Listener} that will be notified when tiles change.
     *
     * @param listener          the {@link Listener}
     * @param notifyForExisting whether or not to call {@link Listener#tileAdded(ITileSnapshot)} for tiles that were already cached before
     *                          the listener was added
     */
    void addListener(@NonNull Listener<POS, T> listener, boolean notifyForExisting);

    /**
     * Removes a previously added {@link Listener}.
     *
     * @param listener      the {@link Listener}
     * @param notifyRemoval whether or not to call {@link Listener#tileRemoved(IFarPos)} for all cached tiles
     */
    void removeListener(@NonNull Listener<POS, T> listener, boolean notifyRemoval);

    /**
     * Gets the given tile at the given position from the cache.
     * <p>
     * The tile is retained before being returned, i.e. ownership is transferred to the caller.
     *
     * @param position the position
     * @return the tile at the given position, or {@code null} if the tile wasn't present in the cache
     */
    ITileSnapshot<POS, T> getTileCached(@NonNull POS position);

    /**
     * Gets the given tiles at the given positions from the cache.
     * <p>
     * Each of the tiles is retained before being returned, i.e. ownership is transferred to the caller.
     *
     * @param positions the positions
     * @return the tiles at the given positions. Tiles that were not present in the cache will be {@code null}
     */
    Stream<ITileSnapshot<POS, T>> getTilesCached(@NonNull Stream<POS> positions);

    DebugStats.TileCache stats();

    /**
     * Receives notifications when a tile is updated in a {@link IFarTileCache}.
     *
     * @author DaPorkchop_
     */
    interface Listener<POS extends IFarPos, T extends IFarTile> {
        /**
         * Fired when a new tile is added to the cache.
         * <p>
         * Ownership of the tile is <strong>not</strong> transferred to the listener. The listener must explicitly retain the tile if it wishes to preserve its reference
         * beyond the scope of this method.
         *
         * @param tile the tile
         */
        void tileAdded(@NonNull ITileSnapshot<POS, T> tile);

        /**
         * Fired when a tile's contents are changed.
         * <p>
         * Ownership of the tile is <strong>not</strong> transferred to the listener. The listener must explicitly retain the tile if it wishes to preserve its reference
         * beyond the scope of this method.
         *
         * @param tile the tile
         */
        void tileModified(@NonNull ITileSnapshot<POS, T> tile);

        /**
         * Fired when a tile is removed from the cache.
         *
         * @param pos the position of the tile
         */
        void tileRemoved(@NonNull POS pos);
    }
}
