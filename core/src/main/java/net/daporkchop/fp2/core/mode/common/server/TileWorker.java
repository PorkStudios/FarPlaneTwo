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

package net.daporkchop.fp2.core.mode.common.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.core.server.world.ExactFBlockWorldHolder;
import net.daporkchop.fp2.core.util.SimpleRecycler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.SharedFutureScheduler;
import net.daporkchop.lib.common.util.PArrays;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class TileWorker<POS extends IFarPos, T extends IFarTile> implements SharedFutureScheduler.WorkFunction<PriorityTask<POS>, ITileHandle<POS, T>> {
    @NonNull
    protected final AbstractFarTileProvider<POS, T> provider;
    @NonNull
    protected final Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler;

    protected long minimumTimestamp(@NonNull TaskStage stage, @NonNull ITileHandle<POS, T> handle) {
        switch (stage) {
            case LOAD:
                return ITileMetadata.TIMESTAMP_GENERATED;
            case UPDATE:
                long minimumTimestamp = handle.dirtyTimestamp();
                return minimumTimestamp == ITileMetadata.TIMESTAMP_BLANK ? ITileMetadata.TIMESTAMP_GENERATED : minimumTimestamp;
            default:
                throw new IllegalArgumentException("unknown stage: " + stage);
        }
    }

    protected boolean allowNewGeneration(@NonNull TaskStage stage, @NonNull ITileHandle<POS, T> handle) {
        switch (stage) {
            case LOAD:
                return true;
            case UPDATE:
                return false;
            default:
                throw new IllegalArgumentException("unknown stage: " + stage);
        }
    }

    protected PriorityTask<POS> taskFor(@NonNull TaskStage stage, @NonNull POS pos) {
        return this.provider.taskFor(stage, pos);
    }

    @Override
    public void work(@NonNull PriorityTask<POS> task, @NonNull SharedFutureScheduler.Callback<PriorityTask<POS>, ITileHandle<POS, T>> callback) {
        TaskStage stage = task.stage();
        POS pos = task.pos();

        ITileHandle<POS, T> handle = this.provider.storage().handleFor(pos);

        long minimumTimestamp = this.minimumTimestamp(stage, handle);
        long worldTimestamp = this.provider.lastCompletedTick;
        checkState(worldTimestamp >= minimumTimestamp, "worldTimestamp (%d) is less than minimumTimestamp (%d)?!?", worldTimestamp, minimumTimestamp);

        if (handle.timestamp() >= minimumTimestamp) { //break out early if already new enough
            callback.complete(task, handle);
            return;
        }

        if (!(FP2_DEBUG && !fp2().globalConfig().debug().exactGeneration()) && this.provider.anyVanillaTerrainExistsAt(pos)) {
            //there's some terrain at the given position, let's try to generate something with it
            if (pos.level() == 0) {
                //the position is at detail level 0, do exact generation
                try {
                    this.generateExact(task, pos, handle, worldTimestamp, false, callback);
                    return;
                } catch (GenerationNotAllowedException e) {
                    //the terrain existed, but wasn't populated so we don't want to use it
                }
            } else {
                //force the tile to be scaled, which will cause this to be executed recursively
                this.generateScale(task, pos, handle, worldTimestamp, callback);
                return;
            }
        }

        if (this.provider.canGenerateRough(pos)) { //the tile can be generated using the rough generator
            this.generateRough(task, pos, handle, worldTimestamp, callback);
            return;
        }

        if (!this.allowNewGeneration(stage, handle)) { //we aren't allowed to generate any new tiles
            //make sure the tile isn't marked as dirty
            //TODO: this should be impossible, but we can't do proper vanilla terrain population tests yet. delete this in The Future:tm:!
            handle.clearDirty();

            callback.complete(task, handle);
            return;
        }

        //rough generation isn't available...
        if (pos.level() == 0) {
            //do exact generation, allowing it to generate vanilla terrain if needed
            try {
                this.generateExact(task, pos, handle, worldTimestamp, true, callback);
            } catch (GenerationNotAllowedException e) { //impossible
                throw new IllegalArgumentException("generation blocked while processing tile at " + pos, e);
            }
        } else { //this will generate the tile and all tiles below it down to level 0 until the tile can be "generated" from scaled data
            this.generateScale(task, pos, handle, worldTimestamp, callback);
        }
    }

    protected void generateRough(@NonNull PriorityTask<POS> task, @NonNull POS pos, @NonNull ITileHandle<POS, T> handle, long minimumTimestamp, @NonNull SharedFutureScheduler.Callback<PriorityTask<POS>, ITileHandle<POS, T>> callback) {
        checkArg(pos.level() == 0 || this.provider.canGenerateRough(pos), "cannot do rough generation at %s!", pos);

        SimpleRecycler<T> tileRecycler = this.provider.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            //generate tile
            this.provider.generatorRough().generate(pos, tile);

            //update handle contents
            handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), tile);
        } finally {
            tileRecycler.release(tile);
        }

        //notify callback
        callback.complete(task, handle);
    }

    protected void generateExact(@NonNull PriorityTask<POS> task, @NonNull POS pos, @NonNull ITileHandle<POS, T> handle, long minimumTimestamp, boolean allowGeneration, @NonNull SharedFutureScheduler.Callback<PriorityTask<POS>, ITileHandle<POS, T>> callback) throws GenerationNotAllowedException {
        try (FBlockWorld exactWorld = this.provider.world().fp2_IFarWorldServer_exactBlockWorldHolder().worldFor(allowGeneration ? ExactFBlockWorldHolder.AllowGenerationRequirement.ALLOWED : ExactFBlockWorldHolder.AllowGenerationRequirement.NOT_ALLOWED)) {
            //consider promotion to a batch generation task
            Optional<List<POS>> optionalBatchPositions = this.provider.generatorExact().batchGenerationGroup(exactWorld, pos);
            if (optionalBatchPositions.isPresent()) {
                List<POS> initialBatchPositions = optionalBatchPositions.get();

                //ensure the original position is contained in the list
                checkState(initialBatchPositions.remove(pos), "batch group for %s doesn't contain itself! %s", pos, initialBatchPositions);

                //acquire as many positions as possible for this batch
                List<PriorityTask<POS>> batchTasks = callback.acquire(
                        initialBatchPositions.stream().map(batchPos -> this.taskFor(task.stage(), batchPos)).collect(Collectors.toList()),
                        SharedFutureScheduler.AcquisitionStrategy.TRY_STEAL_EXISTING_OR_CREATE);

                //manually add the initial task to the list, since it isn't present in the list now (because we can't acquire the same task twice)
                batchTasks.add(task);

                //actually generate the tiles
                this.generateExactBatch(batchTasks, batchTasks.stream().map(PriorityTask::pos).collect(Collectors.toList()), minimumTimestamp, callback, exactWorld);
            } else {
                //do regular, non-batched exact generation
                this.generateExactSingle(task, pos, handle, minimumTimestamp, callback, exactWorld);
            }
        }
    }

    protected void generateExactSingle(@NonNull PriorityTask<POS> task, @NonNull POS pos, @NonNull ITileHandle<POS, T> handle, long minimumTimestamp, @NonNull SharedFutureScheduler.Callback<PriorityTask<POS>, ITileHandle<POS, T>> callback, @NonNull FBlockWorld exactWorld) throws GenerationNotAllowedException {
        SimpleRecycler<T> tileRecycler = this.provider.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            //generate tile
            this.provider.generatorExact().generate(exactWorld, pos, tile);

            //update handle contents
            handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), tile);
        } finally {
            tileRecycler.release(tile);
        }

        //notify callback
        callback.complete(task, handle);
    }

    protected void generateExactBatch(@NonNull List<PriorityTask<POS>> tasks, @NonNull List<POS> positions, long minimumTimestamp, @NonNull SharedFutureScheduler.Callback<PriorityTask<POS>, ITileHandle<POS, T>> callback, @NonNull FBlockWorld exactWorld) throws GenerationNotAllowedException {
        SimpleRecycler<T> tileRecycler = this.provider.mode().tileRecycler();
        T[] tiles = tileRecycler.allocate(positions.size(), this.provider.mode()::tileArray);
        try {
            //generate tile
            this.provider.generatorExact().generate(exactWorld, positions.toArray(this.provider.mode().posArray(positions.size())), tiles);

            //update tile contents
            this.provider.storage().multiSet(positions,
                    Arrays.asList(PArrays.filled(positions.size(), ITileMetadata[]::new, ITileMetadata.ofTimestamp(minimumTimestamp))),
                    Arrays.asList(tiles));
        } finally {
            tileRecycler.release(tiles);
        }

        //notify callback
        tasks.forEach(task -> callback.complete(task, this.provider.storage().handleFor(task.pos())));
    }

    protected void generateScale(@NonNull PriorityTask<POS> task, @NonNull POS pos, @NonNull ITileHandle<POS, T> handle, long minimumTimestamp, @NonNull SharedFutureScheduler.Callback<PriorityTask<POS>, ITileHandle<POS, T>> callback) {
        //generate scale inputs
        List<ITileHandle<POS, T>> srcHandles = this.scheduler.scatterGather(this.provider.scaler().inputs(pos).map(srcPos -> this.taskFor(task.stage(), srcPos)).collect(Collectors.toList()));

        if (handle.timestamp() >= minimumTimestamp) { //break out early if tile is already done
            callback.complete(task, handle);
            return;
        }

        //inflate sources
        SimpleRecycler<T> tileRecycler = this.provider.mode().tileRecycler();
        T[] srcs = this.provider.mode().tileArray(srcHandles.size());
        for (int i = 0; i < srcHandles.size(); i++) {
            srcs[i] = srcHandles.get(i).snapshot().loadTile(tileRecycler);
        }

        if (handle.timestamp() >= minimumTimestamp) { //break out early if tile is already done
            callback.complete(task, handle);
            return;
        }

        T dst = tileRecycler.allocate();
        try {
            //actually do scaling
            this.provider.scaler().scale(srcs, dst);

            //update handle contents
            handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), dst);
        } finally {
            tileRecycler.release(dst);
            for (T src : srcs) {
                if (src != null) {
                    tileRecycler.release(src);
                }
            }
        }

        //notify callback
        callback.complete(task, handle);
    }
}
