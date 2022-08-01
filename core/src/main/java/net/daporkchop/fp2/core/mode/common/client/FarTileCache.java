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

package net.daporkchop.fp2.core.mode.common.client;

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Default implementation of {@link IFarTileCache}.
 *
 * @author DaPorkchop_
 */
//TODO: this still has some race conditions - it's possible that addListener/removeListener might cause the listener to be notified twice for tiles that are
// received/unloaded during the initial notification pass
//TODO: handling in case an exception is thrown by a listener
public class FarTileCache<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarTileCache<POS, T>, Function<POS, ITileSnapshot<POS, T>> {
    protected final Map<POS, ITileSnapshot<POS, T>> tiles = new ConcurrentHashMap<>();
    protected final Collection<Listener<POS, T>> listeners = new CopyOnWriteArraySet<>();

    protected final AtomicReference<DebugStats.TileSnapshot> debug_tileStats = new AtomicReference<>(DebugStats.TileSnapshot.ZERO);
    protected final LongAdder debug_nonEmptyTileCount = new LongAdder();

    @Override
    public void receiveTile(@NonNull ITileSnapshot<POS, T> tile) {
        this.assertNotReleased();
        this.tiles.compute(tile.pos(), (pos, old) -> {
            this.debug_updateStats(old, tile);

            if (old == null) {
                this.listeners.forEach(listener -> listener.tileAdded(tile));
            } else {
                this.listeners.forEach(listener -> listener.tileModified(tile));
                old.release();
            }
            return tile;
        });
    }

    @Override
    public void unloadTile(@NonNull POS _pos) {
        this.assertNotReleased();
        this.tiles.computeIfPresent(_pos, (pos, old) -> {
            this.debug_updateStats(old, null);

            this.listeners.forEach(listener -> listener.tileRemoved(pos));
            old.release();
            return null;
        });
    }

    @Override
    public void addListener(@NonNull Listener<POS, T> listener, boolean notifyForExisting) {
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

    @Override
    public void removeListener(@NonNull Listener<POS, T> listener, boolean notifyRemoval) {
        this.assertNotReleased();
        checkState(this.listeners.remove(listener), "unknown listener: %s", listener);
        if (notifyRemoval) {
            this.tiles.forEach((pos, tile) -> listener.tileRemoved(pos));
        }
    }

    @Override
    public ITileSnapshot<POS, T> getTileCached(@NonNull POS position) {
        this.assertNotReleased();
        ITileSnapshot<POS, T> snapshot = this.tiles.get(position);
        if (snapshot != null) {
            snapshot.retain();
        }
        return snapshot;
    }

    @Override
    public Stream<ITileSnapshot<POS, T>> getTilesCached(@NonNull Stream<POS> position) {
        this.assertNotReleased();
        return position.map(this);
    }

    protected void debug_updateStats(ITileSnapshot<POS, T> prev, ITileSnapshot<POS, T> next) {
        DebugStats.TileSnapshot prevStats = prev != null ? prev.stats() : DebugStats.TileSnapshot.ZERO;
        DebugStats.TileSnapshot nextStats = next != null ? next.stats() : DebugStats.TileSnapshot.ZERO;

        this.debug_tileStats.updateAndGet(currStats -> currStats.sub(prevStats).add(nextStats));
        this.debug_nonEmptyTileCount.add(prev != null
                ? next != null ? 0L : -1L
                : next != null ? 1L : 0L);
    }

    @Override
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

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public ITileSnapshot<POS, T> apply(@NonNull POS pos) {
        ITileSnapshot<POS, T> snapshot = this.tiles.get(pos);
        if (snapshot != null) {
            snapshot.retain();
        }
        return snapshot;
    }

    @Override
    protected void doRelease() {
        this.tiles.forEach((pos, tile) -> {
            this.listeners.forEach(listener -> listener.tileRemoved(pos));
            tile.release();
        });
        this.tiles.clear();
        this.listeners.clear();
    }
}
