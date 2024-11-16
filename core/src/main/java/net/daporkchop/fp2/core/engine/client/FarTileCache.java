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

package net.daporkchop.fp2.core.engine.client;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.tile.CompressedTileSnapshot;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.listener.ListenerList;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Client-side, in-memory cache for loaded tiles.
 *
 * @author DaPorkchop_
 */
//TODO: this still has some race conditions - it's possible that addListener/removeListener might cause the listener to be notified twice for tiles that are
// received/unloaded during the initial notification pass
//TODO: handling in case an exception is thrown by a listener
public final class FarTileCache extends AbstractReleasable {
    private final Map<TilePos, ITileSnapshot> tiles = new ConcurrentHashMap<>();
    private final ListenerList<Listener> listeners = ListenerList.create(Listener.class);

    private final AtomicReference<Stats> debugStats = new AtomicReference<>(new Stats(0, 0, 0L, 0L));

    /**
     * Adds the given tile into the cache.
     *
     * @param tile the tile to add
     */
    public void receiveTile(@NonNull ITileSnapshot tile) {
        this.assertNotReleased();
        this.tiles.compute(tile.pos(), (pos, old) -> {
            this.debug_updateStats(old, tile);

            this.listeners.dispatcher().tileChanged(pos);
            return tile;
        });
    }

    /**
     * If a tile at the given position exists, tries to compress it.
     *
     * @param pos the tile's position
     */
    public void tryCompressExistingTile(@NonNull TilePos pos) {
        this.assertNotReleased();

        ITileSnapshot tile = this.getTileCached(pos);
        if (tile == null || tile instanceof CompressedTileSnapshot) { //the tile isn't cached, or is cached but is also already compressed
            return;
        }

        ITileSnapshot compressedTile = tile.compressed();
        if (this.tiles.replace(tile.pos(), tile, compressedTile)) { //the tile was successfully replaced with the compressed one
            // (if this fails, the tile was probably changed while we were compressing it)
            this.debug_updateStats(tile, compressedTile);
        }
    }

    public void unloadTile(@NonNull TilePos _pos) {
        this.assertNotReleased();
        this.tiles.computeIfPresent(_pos, (pos, old) -> {
            this.debug_updateStats(old, null);

            this.listeners.dispatcher().tileChanged(pos);
            return null;
        });
    }

    /**
     * Adds a new {@link Listener} that will be notified when tiles change.
     *
     * @param listener the {@link Listener}
     * @return a {@link ListenerList.Handle} which should be closed in order to remove the listener again
     */
    public ListenerList<Listener>.Handle addListener(@NonNull Listener listener) {
        this.assertNotReleased();
        return this.listeners.add(listener);
    }

    /**
     * Gets the given tile at the given position from the cache.
     *
     * @param position the position
     * @return the tile at the given position, or {@code null} if the tile wasn't present in the cache
     */
    public ITileSnapshot getTileCached(@NonNull TilePos position) {
        this.assertNotReleased();

        //retain the tile inside the computeIfPresent() block to ensure that it can't be released between getting it from the map and trying to retain it
        return this.tiles.get(position);
    }

    /**
     * Gets the given tiles at the given positions from the cache.
     *
     * @param positions the positions
     * @return the tiles at the given positions. Tiles that were not present in the cache will be {@code null}
     */
    public List<ITileSnapshot> getTilesCached(@NonNull List<TilePos> positions) {
        this.assertNotReleased();

        List<ITileSnapshot> res = new ArrayList<>(positions.size());
        for (TilePos pos : positions) {
            res.add(this.getTileCached(pos));
        }
        return res;
    }

    /**
     * Gets every tile snapshot in this cache.
     * <p>
     * No synchronization guarantees are made - tiles added or removed after calling this method may or may not be visible in the returned set.
     *
     * @return a {@link Stream} over the position of every tile in this cache
     */
    public Set<TilePos> getAllPositions() {
        this.assertNotReleased();
        return this.tiles.keySet();
    }

    private void debug_updateStats(ITileSnapshot prev, ITileSnapshot next) {
        int d_tileCount = 0;
        int d_tileCountWithData = 0;
        long d_totalSize = 0L;
        long d_uncompressedSize = 0L;

        if (prev != null) {
            d_tileCount--;
            if (!prev.isEmpty()) {
                d_tileCountWithData--;
            }
            d_totalSize -= prev.dataSize();
            d_uncompressedSize -= prev.uncompressedDataSize();
        }
        if (next != null) {
            d_tileCount++;
            if (!next.isEmpty()) {
                d_tileCountWithData++;
            }
            d_totalSize += next.dataSize();
            d_uncompressedSize += next.uncompressedDataSize();
        }

        //damn you java
        int d_tileCount_final = d_tileCount;
        int d_tileCountWithData_final = d_tileCountWithData;
        long d_totalSize_final = d_totalSize;
        long d_uncompressedSize_final = d_uncompressedSize;

        this.debugStats.updateAndGet(currStats -> new Stats(
                currStats.tileCount() + d_tileCount_final,
                currStats.tileCountWithData() + d_tileCountWithData_final,
                currStats.totalSize() + d_totalSize_final,
                currStats.uncompressedSize() + d_uncompressedSize_final));
    }

    /**
     * @return debug statistics describing this tile cache and its current state
     */
    public Stats stats() {
        return this.debugStats.get();
    }

    /**
     * Debug statistics for the tile cache.
     *
     * @author DaPorkchop_
     */
    @Data
    public static final class Stats {
        /**
         * The number of tiles which are stored in the cache.
         */
        private final @NotNegative int tileCount;

        /**
         * The number of tiles which are stored in the cache and whose data is non-empty.
         */
        private final @NotNegative int tileCountWithData;

        /**
         * The total amount of memory allocated for all tile data in this cache.
         */
        private final @NotNegative long totalSize;

        /**
         * The total uncompressed size of all tile data in this cache.
         */
        private final @NotNegative long uncompressedSize;
    }

    @Override
    protected void doRelease() {
        this.tiles.clear();
    }

    /**
     * Receives notifications when a tile is updated in a {@link FarTileCache}.
     *
     * @author DaPorkchop_
     */
    public interface Listener {
        /**
         * Fired when a tile's cached data is added/modified/removed.
         *
         * @param pos the position of the tile
         */
        void tileChanged(@NonNull TilePos pos);

        /**
         * Fired when multiple tiles' cached data is added/modified/removed.
         *
         * @param positions the positions of the tiles
         */
        default void tilesChanged(@NonNull Set<TilePos> positions) {
            positions.forEach(this::tileChanged);
        }
    }
}
