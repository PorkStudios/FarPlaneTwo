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

package net.daporkchop.fp2.core.engine.server;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.tile.ITileHandle;
import net.daporkchop.fp2.core.engine.tile.ITileMetadata;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.SharedFutureScheduler;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.primitive.lambda.ObjObjLongConsumer;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class TileWorker implements SharedFutureScheduler.WorkFunction<PriorityTask, ITileHandle> {
    @NonNull
    protected final TileProvider provider;
    @NonNull
    protected final Scheduler<PriorityTask, ITileHandle> scheduler;

    protected long minimumTimestamp(@NonNull TaskStage stage, @NonNull ITileHandle handle) {
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

    protected LongList minimumTimestamps(@NonNull TaskStage stage, @NonNull List<TilePos> positions) {
        LongList out = new LongArrayList(positions.size());
        switch (stage) {
            case LOAD:
                for (int i = 0; i < positions.size(); i++) {
                    out.add(ITileMetadata.TIMESTAMP_GENERATED);
                }
                break;
            case UPDATE:
                this.provider.storage().multiDirtyTimestamp(positions)
                        .forEach((LongConsumer) minimumTimestamp ->
                                out.add(minimumTimestamp == ITileMetadata.TIMESTAMP_BLANK ? ITileMetadata.TIMESTAMP_GENERATED : minimumTimestamp));
                break;
            default:
                throw new IllegalArgumentException("unknown stage: " + stage);
        }
        return out;
    }

    protected boolean allowNewGeneration(@NonNull TaskStage stage) {
        switch (stage) {
            case LOAD:
                return true;
            case UPDATE:
                return false;
            default:
                throw new IllegalArgumentException("unknown stage: " + stage);
        }
    }

    @Override
    public void work(@NonNull PriorityTask task, @NonNull SharedFutureScheduler.Callback<PriorityTask, ITileHandle> callback) {
        State state = new State(task, callback);

        if (state.considerExit()) {
            return;
        }

        if (!(FP2_DEBUG && !fp2().globalConfig().debug().exactGeneration()) && this.provider.anyVanillaTerrainExistsAt(state.positions())) {
            //there's some terrain at the given position, let's try to generate something with it
            if (state.level() == 0) {
                //the position is at detail level 0, do exact generation
                try {
                    this.generateExact(state, false);
                    return;
                } catch (GenerationNotAllowedException e) {
                    //the terrain existed, but wasn't populated so we don't want to use it
                }
            } else {
                //force the tile to be scaled, which will cause this to be executed recursively
                this.generateScale(state);
                return;
            }
        }

        if (state.positions().stream().anyMatch(this.provider::canGenerateRough)) { //the tile can be generated using the rough generator
            this.generateRough(state);
            return;
        }

        if (!this.allowNewGeneration(state.stage())) { //we aren't allowed to generate any new tiles
            //make sure the tile isn't marked as dirty
            //TODO: this should be impossible, but we can't do proper vanilla terrain population tests yet. delete this in The Future:tm:!
            this.provider.storage().multiClearDirty(state.positions());

            //mark all acquired tasks as complete before exiting
            state.completeAll();
            return;
        }

        //rough generation isn't available...
        if (state.level() == 0) {
            //do exact generation, allowing it to generate vanilla terrain if needed
            try {
                this.generateExact(state, true);
            } catch (GenerationNotAllowedException e) { //impossible
                throw new IllegalArgumentException("encountered ungenerated terrain while processing tile(s) at " + state.positions(), e);
            }
        } else { //this will generate the tile and all tiles below it down to level 0 until the tile can be "generated" from scaled data
            this.generateScale(state);
        }
    }

    protected void generateRough(@NonNull State state) {
        state.positions().forEach(pos -> checkArg(this.provider.canGenerateRough(pos), "cannot do rough generation at %s!", pos));

        //try to steal tasks required to make this a batch
        this.provider.generatorRough().batchGenerationGroup(state.positions()).ifPresent(batchPositions -> {
            //ensure the original position is contained in the list
            checkState(batchPositions.containsAll(state.positions()), "batch group for %s doesn't contain all of its inputs! given: %s", state.positions(), batchPositions);

            //try to acquire all the positions
            state.tryAcquire(batchPositions, SharedFutureScheduler.AcquisitionStrategy.TRY_STEAL_EXISTING_OR_CREATE);
        });

        Recycler<Tile> tileRecycler = Tile.recycler();
        //TODO: T[] tiles = tileRecycler.allocate(state.positions().size(), this.provider.mode()::tileArray);
        Tile[] tiles = PArrays.filledFrom(state.positions().size(), Tile[]::new, tileRecycler::allocate);
        try {
            //generate tile
            this.provider.generatorRough().generate(state.positions().toArray(new TilePos[0]), tiles);

            //update tile contents
            this.provider.storage().multiSet(state.positions(),
                    StreamSupport.stream(state.minimumTimestamps().spliterator(), false).map(ITileMetadata::ofTimestamp).collect(Collectors.toList()),
                    Arrays.asList(tiles));
        } finally {
            //TODO: tileRecycler.release(tiles);
            for (Tile tile : tiles) {
                tileRecycler.release(tile);
            }
            Arrays.fill(tiles, null);
        }

        //notify callback
        state.completeAll();
    }

    protected void generateExact(@NonNull State state, boolean allowGeneration) throws GenerationNotAllowedException {
        try (FBlockLevel exactWorld = this.provider.world().exactBlockLevelHolder().worldFor(allowGeneration
                ? ExactFBlockLevelHolder.AllowGenerationRequirement.ALLOWED
                : ExactFBlockLevelHolder.AllowGenerationRequirement.NOT_ALLOWED)) {
            //try to steal tasks required to make this a batch
            this.provider.generatorExact().batchGenerationGroup(exactWorld, state.positions()).ifPresent(batchPositions -> {
                //ensure the original position is contained in the list
                checkState(batchPositions.containsAll(state.positions()), "batch group for %s doesn't contain all of its inputs! given: %s", state.positions(), batchPositions);

                //try to acquire all the positions
                state.tryAcquire(batchPositions, SharedFutureScheduler.AcquisitionStrategy.TRY_STEAL_EXISTING_OR_CREATE);
            });

            //actually do exact generation
            Recycler<Tile> tileRecycler = Tile.recycler();
            //TODO: T[] tiles = tileRecycler.allocate(state.positions().size(), this.provider.mode()::tileArray);
            Tile[] tiles = PArrays.filledFrom(state.positions().size(), Tile[]::new, tileRecycler::allocate);
            try {
                //generate tile
                this.provider.generatorExact().generate(exactWorld, state.positions().toArray(new TilePos[0]), tiles);

                //update tile contents
                this.provider.storage().multiSet(state.positions(),
                        StreamSupport.stream(state.minimumTimestamps().spliterator(), false).map(ITileMetadata::ofTimestamp).collect(Collectors.toList()),
                        Arrays.asList(tiles));
            } finally {
                //TODO: tileRecycler.release(tiles);
                for (Tile tile : tiles) {
                    tileRecycler.release(tile);
                }
                Arrays.fill(tiles, null);
            }

            //notify callback
            state.completeAll();
        }
    }

    protected void generateScale(@NonNull State state) {
        //find input positions
        List<TilePos> uniqueValidSourcePositions = this.provider.scaler().uniqueInputs(state.positions()).stream()
                .filter(this.provider.coordLimits()::contains)
                .collect(Collectors.toList());

        //wait for all source tiles to be generated
        this.scheduler.scatterGather(uniqueValidSourcePositions.stream()
                .map(state.stage()::taskForPosition)
                .collect(Collectors.toList()));

        if (state.considerExit()) {
            return;
        }

        //snapshot all tiles
        Map<TilePos, ITileSnapshot> snapshotsByPosition = this.provider.storage().multiSnapshot(uniqueValidSourcePositions).stream()
                //no null checks are necessary, because we're only snapshotting the input positions which are valid, and therefore all snapshots will be non-null
                .collect(Collectors.toMap(ITileSnapshot::pos, Function.identity()));

        if (state.considerExit()) {
            return;
        }

        //scale each position individually
        Recycler<Tile> tileRecycler = Tile.recycler();
        state.forEachPositionHandleTimestamp((pos, handle, minimumTimestamp) -> {
            List<TilePos> srcPositions = this.provider.scaler().inputs(pos);

            Tile[] srcs = new Tile[srcPositions.size()];
            Tile dst = tileRecycler.allocate();
            try {
                //inflate tile snapshots where necessary
                for (int i = 0; i < srcPositions.size(); i++) {
                    //tile is only guaranteed to have been generated if it's at a valid position (we filter out invalid
                    // positions above in the scatterGather call)
                    if (this.provider.coordLimits().contains(srcPositions.get(i))) {
                        srcs[i] = snapshotsByPosition.get(srcPositions.get(i)).loadTile(tileRecycler, Tile.CODEC);
                    }
                }

                //actually do scaling
                this.provider.scaler().scale(srcs, dst);

                //update handle contents
                handle.set(ITileMetadata.ofTimestamp(minimumTimestamp), dst);
            } finally {
                //release all allocated tile instances
                tileRecycler.release(dst);
                for (Tile src : srcs) {
                    if (src != null) {
                        tileRecycler.release(src);
                    }
                }
            }
        });

        //all tiles have been scaled, mark the tasks as complete
        state.completeAll();
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    protected class State {
        private final TaskStage stage;
        private final SharedFutureScheduler.Callback<PriorityTask, ITileHandle> callback;

        private final int level;

        private final Set<TilePos> allPositions = new ObjectOpenHashSet<>();
        private final List<TilePos> positions = DirectTilePosAccess.newPositionArrayList();
        private final List<ITileHandle> handles = new ArrayList<>();
        private final LongList minimumTimestamps = new LongArrayList();

        private final long worldTimestamp = TileWorker.this.provider.currentTimestamp();

        public State(@NonNull PriorityTask initialTask, @NonNull SharedFutureScheduler.Callback<PriorityTask, ITileHandle> callback) {
            this.stage = initialTask.stage();
            this.callback = callback;

            this.level = initialTask.pos().level();
            this.add(initialTask.pos());
        }

        private void add(@NonNull TilePos pos) {
            checkArg(TileWorker.this.provider.coordLimits().contains(pos), "tile position is outside coordinate limits: %s", pos);

            ITileHandle handle = TileWorker.this.provider.storage().handleFor(pos);

            long minimumTimestamp = TileWorker.this.minimumTimestamp(this.stage, handle);
            checkState(this.worldTimestamp >= minimumTimestamp, "worldTimestamp (%d) is less than minimumTimestamp (%d)?!?", this.worldTimestamp, minimumTimestamp);

            checkState(this.allPositions.add(pos), "position %s has already been acquired!", pos);
            this.positions.add(pos);
            this.handles.add(handle);
            this.minimumTimestamps.add(minimumTimestamp);
        }

        private void add(@NonNull List<TilePos> positions) {
            positions.forEach(pos -> checkArg(TileWorker.this.provider.coordLimits().contains(pos), "tile position is outside coordinate limits: %s", pos));

            LongList minimumTimestamps = TileWorker.this.minimumTimestamps(this.stage, positions);
            minimumTimestamps.forEach((LongConsumer) minimumTimestamp -> checkState(this.worldTimestamp >= minimumTimestamp,
                    "worldTimestamp (%d) is less than minimumTimestamp (%d)?!?", this.worldTimestamp, minimumTimestamp));

            positions.forEach(pos -> {
                checkState(this.allPositions.add(pos), "position %s has already been acquired!", pos);
                this.handles.add(TileWorker.this.provider.storage().handleFor(pos));
            });
            this.positions.addAll(positions);
            this.minimumTimestamps.addAll(minimumTimestamps);
        }

        protected void tryAcquire(@NonNull Collection<TilePos> positions, @NonNull SharedFutureScheduler.AcquisitionStrategy strategy) {
            //convert to PriorityTasks
            List<PriorityTask> initialTasks = positions.stream()
                    .filter(pos -> !this.allPositions.contains(pos)) //skip positions which have already been acquired to avoid adding duplicates
                    .map(this.stage::taskForPosition).collect(Collectors.toList());

            //try to acquire the tasks
            List<PriorityTask> acquiredTasks = this.callback.acquire(initialTasks, strategy);

            //add the acquired tasks to this state
            this.add(acquiredTasks.stream().map(PriorityTask::pos).collect(Collectors.toList()));
        }

        public void completeAll() {
            //complete all positions
            for (int i = 0; i < this.positions.size(); i++) {
                this.callback.complete(this.stage.taskForPosition(this.positions.get(i)), this.handles.get(i));
            }

            //clear all state
            this.positions.clear();
            this.handles.clear();
            this.minimumTimestamps.clear();
        }

        public boolean considerExit() {
            //check if any positions are already new enough
            LongList timestamps = TileWorker.this.provider.storage().multiTimestamp(this.positions);
            for (int i = 0; i < timestamps.size(); i++) {
                if (timestamps.get(i) >= this.minimumTimestamps.get(i)) { //tile is new enough
                    //complete the corresponding task
                    this.callback.complete(this.stage.taskForPosition(this.positions.get(i)), this.handles.get(i));

                    //remove the position from state
                    this.positions.remove(i);
                    this.handles.remove(i);
                    this.minimumTimestamps.removeAt(i);
                    timestamps.removeAt(i);
                    i--;
                }
            }

            //if no positions are left, this task has nothing left to do and may safely exit
            return this.positions.isEmpty();
        }

        public void forEachPositionHandleTimestamp(@NonNull ObjObjLongConsumer<TilePos, ITileHandle> action) {
            for (int i = 0; i < this.positions.size(); i++) {
                action.accept(this.positions.get(i), this.handles.get(i), this.minimumTimestamps.get(i));
            }
        }
    }
}
