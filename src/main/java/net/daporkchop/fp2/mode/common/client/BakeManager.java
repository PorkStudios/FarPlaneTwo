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

package net.daporkchop.fp2.mode.common.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.fp2.util.threading.keyed.DefaultKeyedTaskScheduler;
import net.daporkchop.fp2.util.threading.keyed.KeyedTaskScheduler;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class BakeManager<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarTileCache.Listener<POS, T> {
    protected static final long[] EMPTY_TIMESTAMPS = new long[0];

    protected static boolean compareTimestamps(@NonNull long[] a, @NonNull long[] b) {
        if (a == EMPTY_TIMESTAMPS) {
            return a != b;
        }
        checkArg(a.length == b.length, "%d != %d", a.length, b.length);

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] >> 63L != b[i] >> 63L) { //sign changed - this means the tile in question was added/removed
                return true;
            } else if (a[i] < b[i]) { //tile is newer
                return true;
            }
        }
        return false;
    }

    protected final AbstractFarRenderer<POS, T> renderer;
    protected final IFarRenderStrategy<POS, T> strategy;

    protected final IFarTileCache<POS, T> tileCache;

    protected final FarRenderTree<POS, T> tree;
    protected final Map<POS, long[]> bakeTimestamps = new ConcurrentHashMap<>();

    protected final KeyedTaskScheduler<POS> bakeExecutor;

    public BakeManager(@NonNull AbstractFarRenderer<POS, T> renderer, @NonNull IFarTileCache<POS, T> tileCache) {
        this.renderer = renderer;
        this.strategy = renderer.strategy();
        this.tileCache = tileCache;

        this.tree = new FarRenderTree<>(renderer.mode(), this.strategy, renderer.maxLevel());

        this.bakeExecutor = new DefaultKeyedTaskScheduler<>(
                FP2Config.client.renderThreads,
                PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Rendering Thread #%d").build());

        this.tileCache.addListener(this, true);
    }

    @Override
    protected void doRelease() {
        this.tileCache.removeListener(this, false);

        this.bakeExecutor.release();
    }

    @Override
    public void tileAdded(@NonNull Compressed<POS, T> tile) {
        checkState(this.bakeTimestamps.putIfAbsent(tile.pos(), EMPTY_TIMESTAMPS) == null, "tile at %s was already added?!?", tile.pos());

        this.notifyOutputs(tile.pos());
    }

    @Override
    public void tileModified(@NonNull Compressed<POS, T> tile) {
        checkState(this.bakeTimestamps.containsKey(tile.pos()), "tile at %s hasn't been added?!?", tile.pos());

        this.notifyOutputs(tile.pos());
    }

    @Override
    public void tileRemoved(@NonNull POS pos) {
        checkState(this.bakeTimestamps.remove(pos) != null, "tile at %s hasn't been added?!?", pos);

        //make sure that any in-progress bake tasks are finished before the tile is removed
        this.bakeExecutor.submitExclusive(pos, () -> ClientThreadExecutor.INSTANCE.execute(() -> this.tree.removeNode(pos)));
    }

    protected void notifyOutputs(@NonNull POS pos) {
        this.strategy.bakeOutputs(pos).forEach(outputPos -> {
            if (outputPos.level() < 0 || outputPos.level() > this.renderer.maxLevel) {
                return;
            }

            if (!this.bakeTimestamps.containsKey(outputPos)) { //if the output tile itself doesn't exist, there's obviously no reason to even consider it
                return;
            }

            //schedule tile for baking
            this.bakeExecutor.submitExclusive(outputPos, () -> this.bake(outputPos));
        });
    }

    protected void bake(@NonNull POS pos) { //this function is called from inside of RENDER_WORKERS, which holds a lock on the position
        long[] oldTimestamps = this.bakeTimestamps.get(pos);
        if (oldTimestamps == null) { //the tile was removed before this task began execution
            return;
        }

        Compressed<POS, T>[] compressedInputTiles = uncheckedCast(this.tileCache.getTilesCached(this.strategy.bakeInputs(pos)).toArray(Compressed[]::new));
        if (compressedInputTiles[0] == null) { //the tile isn't cached any more
            return;
        }

        long[] newTimestamps = Arrays.stream(compressedInputTiles).mapToLong(c -> c != null ? c.timestamp() : -1L).toArray();
        if (!compareTimestamps(oldTimestamps, newTimestamps)) { //nothing has changed, no need to re-render
            return;
        }

        SimpleRecycler<T> recycler = this.renderer.mode().tileRecycler();
        T[] srcs = this.renderer.mode().tileArray(compressedInputTiles.length);
        try {
            for (int i = 0; i < srcs.length; i++) { //inflate tiles
                if (compressedInputTiles[i] != null) {
                    srcs[i] = compressedInputTiles[i].inflate(recycler);
                }
            }

            BakeOutput output = new BakeOutput(this.strategy.renderDataSize());
            boolean empty = this.strategy.bake(pos, srcs, output);
            if (!this.bakeTimestamps.replace(pos, oldTimestamps, newTimestamps)) { //tile was removed while baking
                return;
            }

            if (empty) {
                ClientThreadExecutor.INSTANCE.execute(() -> {
                    if (this.bakeTimestamps.get(pos) != newTimestamps) { //tile was baked again since this task was submitted to the client thread
                        return;
                    }
                    this.strategy.executeBakeOutput(pos, output);
                    this.tree.putRenderData(pos, output);
                });
            } else { //remove tile from render tree
                ClientThreadExecutor.INSTANCE.execute(() -> {
                    if (this.bakeTimestamps.get(pos) != newTimestamps) { //tile was baked again since this task was submitted to the client thread
                        return;
                    }
                    this.tree.removeNode(pos);
                });
            }
        } finally { //release tiles again
            for (int i = 0; i < srcs.length; i++) {
                if (srcs[i] != null) {
                    recycler.release(srcs[i]);
                }
            }
        }
    }
}
