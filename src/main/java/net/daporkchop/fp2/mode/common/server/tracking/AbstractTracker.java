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
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;
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

    protected final RecyclingArrayDeque<POS> queuedPositions = new RecyclingArrayDeque<>();
    protected final SimpleSet<POS> loadedPositions;
    protected final Set<POS> waitingPositions = ConcurrentHashMap.newKeySet();
    protected final Queue<POS> doneWaitingPositions = new ConcurrentLinkedQueue<>();

    //these are using a single object reference instead of flattened fields to allow the value to be replaced atomically. to ensure coherent access to the values,
    // readers must take care never to dereference the fields more than once.
    protected volatile STATE lastState;
    protected volatile STATE nextState;

    protected volatile boolean queuePaused = false;

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
        STATE nextState = this.currentState(this.context);
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

                { //untrack all the currently waiting tiles
                    //  this makes tile loading more responsive by forcing high-priority tiles to the front of the execution queue, and simplifies
                    //  synchronization logic in the rest of this class' code

                    //pause queue updates in order to make waitingPositions immutable
                    synchronized (this) { //synchronizing here is, in fact, critical to making this work (i think)
                        this.queuePaused = true;
                    }

                    List<POS> waitingPositions = new ArrayList<>(this.waitingPositions);
                    this.waitingPositions.clear();

                    //stop tracking all positions in the set
                    waitingPositions.forEach(pos -> this.manager.stopTracking(this, pos));

                    synchronized (this) {
                        //re-add all of the now cancelled tasks to the queue
                        //  (this probably doesn't need to be synchronized, but i'm doing it anyway to be safe)
                        this.queuedPositions.addAll(waitingPositions);
                    }
                }

                try (SimpleSet<POS> untrackingPositions = this.mode.directPosAccess().newPositionSet()) {
                    //actually update the tracking state (this is synchronized)
                    this.updateState(lastState, nextState, untrackingPositions);

                    //handle unloading tiles now that we no longer hold a lock
                    untrackingPositions.forEach(pos -> this.manager.stopTracking(this, pos));
                }

                checkState(this.waitingPositions.isEmpty(), "load queue isn't empty?!? %s", this.waitingPositions);

                //unpause the queue so that we can fill it up again
                this.queuePaused = false;
            }
        }

        this.updateFillLoadQueue();
    }

    protected synchronized void updateState(STATE lastState, @NonNull STATE nextState, @NonNull SimpleSet<POS> untrackingPositions) {
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
                        //untrack the tile if needed
                        if (this.loadedPositions.remove(pos)) {
                            untrackingPositions.add(pos);
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
        int targetLoadQueueSize = FP2Config.global().performance().terrainThreads() << 4;
        List<POS> positions = new ArrayList<>();

        do {
            if (this.queuePaused) { //the tracker update thread has specifically requested to pause queue polling, so we shouldn't do anything here
                return;
            }

            if (!PUnsafe.tryMonitorEnter(this)) { //this tracker's monitor is already held!
                //there are three ways this can happen, in all of which it is safe for us to exit without potentially missing positions:
                //  - a tracker thread is currently running updateState(), in which case it'll eventually release the monitor and call updateFillLoadQueue()
                //  - some other thread (tracker or terrain) is currently running updateFillLoadQueue(), in which case it'll loop around again (so we can be sure
                //    that any queue slots that may have been freed up by this thread prior to it calling updateFillLoadQueue() will be filled eventually)
                //  - the tracker is being closed, in which case we shouldn't be adding more tiles to the load queue anyway
                return;
            }

            try {
                //move completed positions from waitingPositions to loadedPositions
                for (POS pos; (pos = this.doneWaitingPositions.poll()) != null; ) {
                    this.waitingPositions.remove(pos);
                    this.loadedPositions.add(pos);
                }

                if (this.queuedPositions.isEmpty()) { //the queue is empty, so there's nothing left to do
                    return;
                }

                //keep adding positions from the queue until waitingPositions has targetLoadQueueSize elements or the queue is drained
                for (int count = targetLoadQueueSize - this.waitingPositions.size(); count > 0; count--) {
                    POS pos = this.queuedPositions.poll();
                    if (pos == null) { //nothing left in the queue, therefore nothing left to do!
                        break;
                    }

                    //buffer the positions we want to add in a list (we don't want to being tracking them while holding the monitor since that could deadlock)
                    positions.add(pos);
                }
            } finally {
                PUnsafe.monitorExit(this);
            }

            //begin tracking all of the added positions
            this.waitingPositions.addAll(positions);
            positions.forEach(pos -> this.manager.beginTracking(this, pos));
            positions.clear();
        } while (!this.doneWaitingPositions.isEmpty() || this.waitingPositions.size() < targetLoadQueueSize);
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
        try {
            this.context.sendTile(uncheckedCast(snapshot));

            POS pos = snapshot.pos();
            if (this.waitingPositions.contains(pos)) { //this tile has been initially loaded
                //mark the position as done waiting
                checkState(this.doneWaitingPositions.add(pos), "couldn't mark completed position as done waiting: ", pos);

                //try to fill up the load queue again to compensate for the removal of this position
                this.updateFillLoadQueue();
            }
        } catch (Throwable t) {
            ThreadingHelper.handle(this.context.tileProvider().world(), t);
            PUnsafe.throwException(t);
        }
    }

    /**
     * Notifies the tracker that the tile data at the given position has been unloaded.
     * <p>
     * This will only be called for a position which was already loaded.
     *
     * @param pos the position of the tile which was unloaded
     */
    @CalledFromAnyThread
    protected void notifyUnloaded(@NonNull POS pos) {
        this.context.sendTileUnload(pos);
    }

    @CalledFromServerThread
    @Override
    public synchronized void close() {
        //tell the client to unload all tiles
        this.context.sendMultiTileUnload(this.loadedPositions);

        //untrack all positions
        //  (using temporary set to avoid CME)
        try (SimpleSet<POS> tmp = this.mode.directPosAccess().newPositionSet()) {
            this.waitingPositions.forEach(tmp::add);
            this.loadedPositions.forEach(tmp::add);

            tmp.forEach(pos -> this.manager.stopTracking(this, pos));
        }

        //release everything
        this.queuedPositions.close();
        this.loadedPositions.close();
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

    /**
     * Computes the current {@link STATE} from the given context.
     *
     * @param context the {@link IFarServerContext} which we are tracking for
     * @return a new {@link STATE}
     */
    protected abstract STATE currentState(@NonNull IFarServerContext<POS, T> context);

    /**
     * Checks whether or not the difference between two given {@link STATE}s is sufficiently drastic to warrant triggering a tracking update.
     *
     * @param oldState the old {@link STATE}
     * @param newState the new {@link STATE}
     * @return whether or not a tracking update should be triggered
     */
    protected abstract boolean shouldTriggerUpdate(@NonNull STATE oldState, @NonNull STATE newState);

    /**
     * Enumerates every tile position visible in the given {@link STATE}.
     *
     * @param state    the {@link STATE}
     * @param callback a callback function which should be called once for every visible tile position
     */
    protected abstract void allPositions(@NonNull STATE state, @NonNull Consumer<POS> callback);

    /**
     * Computes the tile positions whose visibility changed between two given {@link STATE}s.
     *
     * @param oldState the old {@link STATE}
     * @param newState the new {@link STATE}
     * @param added    a callback function which should be called once for every tile position which was not visible in the old state but is now visible
     * @param removed  a callback function which should be called once for every tile position which was visible in the old state but is no longer visible
     */
    protected abstract void deltaPositions(@NonNull STATE oldState, @NonNull STATE newState, @NonNull Consumer<POS> added, @NonNull Consumer<POS> removed);

    /**
     * Checks whether or not the given tile position is visible in the given state.
     *
     * @param state the {@link STATE}
     * @param pos   the tile position to check
     * @return whether or not the tile position is visible
     */
    protected abstract boolean isVisible(@NonNull STATE state, @NonNull POS pos);

    /**
     * Gets a {@link Comparator} which can be used for sorting the tile positions visible in the given {@link STATE} by their load priority.
     *
     * @param state the {@link STATE}
     * @return a {@link Comparator} for sorting visible tile positions
     */
    protected abstract Comparator<POS> comparatorFor(@NonNull STATE state);
}
