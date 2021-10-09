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

package net.daporkchop.fp2.mode.common.server.tracking;

import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.debug.util.DebugStats;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.api.server.tracking.IFarTracker;
import net.daporkchop.fp2.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.util.annotation.CalledFromServerThread;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.fp2.util.annotation.RemovalPolicy;
import net.daporkchop.fp2.util.datastructure.RecyclingArrayDeque;
import net.daporkchop.fp2.util.datastructure.SimpleSet;
import net.daporkchop.fp2.util.math.IntAxisAlignedBB;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractTracker<POS extends IFarPos, T extends IFarTile, STATE> implements IFarTracker<POS, T> {
    /**
     * The squared distance a player must move from their previous position in order to trigger a tracking update.
     * <p>
     * The default value of {@code (T_VOXELS / 2)²} is based on the equivalent value of {@code 64} (which is {@code (CHUNK_SIZE / 2)²}) used by vanilla.
     */
    protected static final double UPDATE_TRIGGER_DISTANCE_SQUARED = sq(T_VOXELS >> 1);

    protected final AbstractTrackerManager<POS, T> manager;
    protected final IFarRenderMode<POS, T> mode;

    protected final IFarServerContext<POS, T> context;
    protected final IntAxisAlignedBB[] coordLimits;

    protected final SimpleSet<POS> loadedPositions;
    protected final Set<POS> waitingPositions = ConcurrentHashMap.newKeySet();
    protected final RecyclingArrayDeque<POS> queuedPositions = new RecyclingArrayDeque<>();

    //these are using a single object reference instead of flattened fields to allow the value to be replaced atomically. to ensure coherent access to the values,
    // readers must take care never to dereference the fields more than once.
    protected volatile STATE lastState;
    protected volatile STATE nextState;

    @DebugOnly(RemovalPolicy.DROP)
    protected long lastUpdateTime;

    public AbstractTracker(@NonNull AbstractTrackerManager<POS, T> manager, @NonNull IFarServerContext<POS, T> context) {
        this.manager = manager;
        this.mode = manager.tileProvider().mode();

        this.context = context;
        this.coordLimits = ((IFarWorldServer) manager.tileProvider().world()).fp2_IFarWorld_coordLimits();

        this.loadedPositions = this.mode.directPosAccess().newPositionSet();
    }

    @CalledFromServerThread
    @Override
    public void update() {
        STATE lastState = this.lastState;
        STATE nextState = this.currentState();
        if (lastState == null || this.shouldTriggerUpdate(lastState, nextState)) {
            //set nextPos to be used while updating
            this.nextState = nextState;

            //add update task to execution queue (this will call doUpdate)
            this.manager.scheduler().schedule(this);
        }
    }

    /**
     * Actually runs a tracker update. Called from the {@link #manager}'s update scheduler.
     */
    protected void doUpdate() {
        { //check if we need to update tracking state
            STATE lastState = this.lastState;
            STATE nextState = this.nextState;
            if (nextState != null && (lastState == null || this.shouldTriggerUpdate(lastState, nextState))) {
                //inform the server thread that this update has started, by updating the current state and clearing the next one
                this.lastState = nextState;
                this.nextState = null;

                try (SimpleSet<POS> unloadingPositions = this.mode.directPosAccess().newPositionSet()) {
                    //actually update the tracking state (this is synchronized)
                    this.updateState(lastState, nextState, unloadingPositions);

                    //handle unloading tiles now that we no longer hold a lock
                    unloadingPositions.forEach(pos -> this.manager.stopTracking(this, pos));

                    //we've stopped tracking all of the positions we wanted to unload, so now we can assume that notifyChanged() won't be called any more
                    //  for any of those tiles and can safely tell the client to unload them
                    this.context.sendMultiTileUnload(unloadingPositions);
                }
            }
        }
    }

    protected synchronized void updateState(STATE lastState, @NonNull STATE nextState, @NonNull SimpleSet<POS> unloadingPositions) {
        long startTime = System.nanoTime();

        if (lastState != null) { //if lastState exists, we can diff the positions (which is faster than iterating over all of them)
            //unqueue all positions which are no longer visible.
            //  this is O(n), whereas removing them during the deltaPositions 'removed' callback would be O(n*m) (where n=queue size, m=number of positions in render
            //  distance). the savings from this are significant - when flying around with 1level@400cutoff, tracker updates are reduced from 5000-10000ms to about 40-50ms.
            this.queuedPositions.removeIf(pos -> !this.isVisible(nextState, pos));

            //figure out which positions were added/removed
            this.deltaPositions(lastState, nextState,
                    this.queuedPositions::add,
                    pos -> {
                        //stop loading the tile if needed
                        if (this.loadedPositions.remove(pos) || this.waitingPositions.remove(pos)) {
                            unloadingPositions.add(pos);
                        }
                    });
        } else { //no positions have been added so far, so we need to iterate over them all
            this.allPositions(nextState, this.queuedPositions::add);
        }

        //sort queue
        this.queuedPositions.sort(this.comparatorFor(nextState));

        this.lastUpdateTime = System.nanoTime() - startTime;
    }

    protected void updateFillLoadQueue() {
        int targetLoadQueueSize = FP2Config.global().performance().terrainThreads();
        List<POS> addedPositions = new ArrayList<>();

        do {
            if (!PUnsafe.tryMonitorEnter(this)) { //this tracker's monitor is already held!
                //there are three ways this can happen, in all of which it is safe for us to exit without potentially missing positions:
                //  - a tracker thread is currently running updateState(), in which case it'll eventually release the monitor and call updateFillLoadQueue()
                //  - some other thread (tracker or terrain) is currently running updateFillLoadQueue(), in which case it'll loop around again (so we can be sure
                //    that any queue slots that may have been freed up by this thread prior to it calling updateFillLoadQueue() will be filled eventually)
                //  - the tracker is being closed, in which case we shouldn't be adding more tiles to the load queue anyway
                return;
            }

            try {
                if (this.queuedPositions.isEmpty()) { //the queue is empty, so there's nothing left to do
                    return;
                }

                //keep adding positions from the queue until waitingPositions has targetLoadQueueSize elements or the queue is drained
                for (int count = targetLoadQueueSize - this.waitingPositions.size(); count > 0; count--) {
                    POS pos = this.queuedPositions.poll();
                    if (pos == null) { //nothing left in the queue, therefore nothing left to do!
                        return;
                    }

                    this.waitingPositions.add(pos);

                    //buffer the positions we want to add in a list (we don't want to being tracking them while holding the monitor since that could deadlock)
                    addedPositions.add(pos);
                }
            } finally {
                PUnsafe.monitorExit(this);
            }

            //begin tracking all of the added positions
            addedPositions.forEach(pos -> this.manager.beginTracking(this, pos));
            addedPositions.clear();
        } while (this.waitingPositions.size() < targetLoadQueueSize);
    }

    /**
     * Notifies the tracker that the tile data at the given position has been modified.
     * <p>
     * This is also called when initially loading a tile.
     *
     * @param snapshot a snapshot of the tile data
     */
    @CalledFromAnyThread
    protected void notifyChanged(@NonNull ITileSnapshot<POS, T> snapshot) {
        this.context.sendTile(uncheckedCast(snapshot));

        if (this.waitingPositions.remove(snapshot.pos())) { //we were waiting for this position to be loaded!
            this.updateFillLoadQueue();
        }
    }

    @CalledFromServerThread
    @Override
    public synchronized void close() {
        //tell the client to unload all tiles
        this.context.sendMultiTileUnload(this.loadedPositions);

        //remove player from all tracking positions
        // (using temporary set to avoid CME)
        try (SimpleSet<POS> tmp = this.mode.directPosAccess().newPositionSet()) {
            this.waitingPositions.forEach(tmp::add);
            this.loadedPositions.forEach(tmp::add);

            tmp.forEach(pos -> this.manager.stopTracking(this, pos));
        }

        //release everything
        this.queuedPositions.close();
        this.loadedPositions.close();
        this.waitingPositions.clear();
    }

    @DebugOnly
    @Override
    public DebugStats.Tracking debugStats() {
        return DebugStats.Tracking.builder()
                .tilesLoaded(this.loadedPositions.count())
                .tilesLoading(this.waitingPositions.size())
                .tilesQueued(this.queuedPositions.size())
                .lastUpdateDuration(this.lastUpdateTime)
                .avgUpdateDuration(this.lastUpdateTime)
                .build();
    }

    protected abstract STATE currentState();

    protected abstract void allPositions(@NonNull STATE state, @NonNull Consumer<POS> callback);

    protected abstract void deltaPositions(@NonNull STATE oldState, @NonNull STATE newState, @NonNull Consumer<POS> added, @NonNull Consumer<POS> removed);

    protected abstract boolean isVisible(@NonNull STATE state, @NonNull POS pos);

    protected abstract Comparator<POS> comparatorFor(@NonNull STATE state);

    protected abstract boolean shouldTriggerUpdate(@NonNull STATE oldState, @NonNull STATE newState);
}
