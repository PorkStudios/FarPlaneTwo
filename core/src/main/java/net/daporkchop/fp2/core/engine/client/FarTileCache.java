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

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.tile.CompressedTileSnapshot;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

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
    private final Collection<Listener> listeners = new CopyOnWriteArraySet<>();

    private final AtomicReference<DebugStats.TileSnapshot> debug_tileStats = new AtomicReference<>(DebugStats.TileSnapshot.ZERO);
    private final LongAdder debug_nonEmptyTileCount = new LongAdder();

    /**
     * Adds the given tile into the cache.
     *
     * @param tile the tile to add
     */
    public void receiveTile(@NonNull ITileSnapshot tile) {
        this.assertNotReleased();
        this.tiles.compute(tile.pos(), (pos, old) -> {
            this.debug_updateStats(old, tile);

            if (old == null) {
                this.listeners.forEach(listener -> listener.tileAdded(tile));
            } else {
                this.listeners.forEach(listener -> listener.tileModified(tile));
            }
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

            this.listeners.forEach(listener -> listener.tileRemoved(pos));
            return null;
        });
    }

    /**
     * Adds a new {@link Listener} that will be notified when tiles change.
     *
     * @param listener          the {@link Listener}
     * @param notifyForExisting whether or not to call {@link Listener#tileAdded(ITileSnapshot)} for tiles that were already cached before
     *                          the listener was added
     */
    public void addListener(@NonNull Listener listener, boolean notifyForExisting) {
        this.assertNotReleased();
        checkState(this.listeners.add(listener), "duplicate listener: %s", listener);
        if (notifyForExisting) {
            //iterate while delegating to compute() for each key to ensure we hold a lock
            this.tiles.forEach((_pos, _tile) -> this.tiles.computeIfPresent(_pos, (pos, tile) -> {
                listener.tileAdded(tile);
                return tile; //don't modify the saved tile
            }));
        }
    }

    /**
     * Removes a previously added {@link Listener}.
     *
     * @param listener      the {@link Listener}
     * @param notifyRemoval whether or not to call {@link Listener#tileRemoved(TilePos)} for all cached tiles
     */
    public void removeListener(@NonNull Listener listener, boolean notifyRemoval) {
        this.assertNotReleased();
        checkState(this.listeners.remove(listener), "unknown listener: %s", listener);
        if (notifyRemoval) {
            this.tiles.forEach((pos, tile) -> listener.tileRemoved(pos));
        }
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
     * No synchronization guarantees are made - tiles added or removed after calling this method may or may not be visible in the returned stream.
     *
     * @return a {@link Stream} over every tile snapshot in this cache
     */
    public Stream<ITileSnapshot> getAllTiles() {
        this.assertNotReleased();
        return this.tiles.values().stream();
    }

    private void debug_updateStats(ITileSnapshot prev, ITileSnapshot next) {
        DebugStats.TileSnapshot prevStats = prev != null ? prev.stats() : DebugStats.TileSnapshot.ZERO;
        DebugStats.TileSnapshot nextStats = next != null ? next.stats() : DebugStats.TileSnapshot.ZERO;

        this.debug_tileStats.updateAndGet(currStats -> currStats.sub(prevStats).add(nextStats));
        this.debug_nonEmptyTileCount.add(prev != null
                ? next != null ? 0L : -1L
                : next != null ? 1L : 0L);
    }

    public DebugStats.TileCache stats() {
        DebugStats.TileSnapshot snapshotStats = this.debug_tileStats.get();

        return DebugStats.TileCache.builder()
                .tileCount(this.tiles.size())
                .tileCountWithData(this.debug_nonEmptyTileCount.sum())
                .allocatedSpace(snapshotStats.allocatedSpace())
                .totalSpace(snapshotStats.allocatedSpace())
                .uncompressedSize(snapshotStats.uncompressedSize())
                .build();
    }

    @Override
    protected void doRelease() {
        this.tiles.forEach((pos, tile) -> this.listeners.forEach(listener -> listener.tileRemoved(pos)));
        this.tiles.clear();
        this.listeners.clear();
    }

    /**
     * Receives notifications when a tile is updated in a {@link FarTileCache}.
     *
     * @author DaPorkchop_
     */
    public interface Listener {
        /**
         * Fired when a new tile is added to the cache.
         *
         * @param tile the tile
         */
        void tileAdded(@NonNull ITileSnapshot tile);

        /**
         * Fired when a tile's contents are changed.
         *
         * @param tile the tile
         */
        void tileModified(@NonNull ITileSnapshot tile);

        /**
         * Fired when a tile is removed from the cache.
         *
         * @param pos the position of the tile
         */
        void tileRemoved(@NonNull TilePos pos);
    }
}
