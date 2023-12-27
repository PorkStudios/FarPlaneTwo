/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.common.server.tracking;

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TileCoordLimits;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;
import net.daporkchop.fp2.core.util.datastructure.RecyclingArrayDeque;
import net.daporkchop.lib.common.annotation.BorrowOwnership;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Per-context tracker instance.
 * <p>
 * This provides the generic functionality for scheduling tracker updates, manages the queue of tiles to be loaded and schedules loads for new tiles as necessary.
 *
 * @author DaPorkchop_
 * @see AbstractTrackerManager
 */
public abstract class AbstractTracker {
    protected final AbstractTrackerManager manager;

    protected final IFarServerContext context;
    protected final TileCoordLimits coordLimits;

    /**
     * The actual queue of positions to load.
     * <p>
     * May only be accessed while holding this tracker's lock. All operations other than polling additionally require that the queue be paused.
     */
    protected final RecyclingArrayDeque<TilePos> queuedPositions = new RecyclingArrayDeque<>();

    /**
     * The set of positions which are loaded.
     */
    protected final Set<TilePos> loadedPositions;
    protected final Set<TilePos> waitingPositions = ConcurrentHashMap.newKeySet();
    protected final Queue<TilePos> doneWaitingPositions = new ConcurrentLinkedQueue<>();

    //these are using a single object reference instead of flattened fields to allow the value to be replaced atomically. to ensure coherent access to the values,
    // readers must take care never to dereference the fields more than once.
    protected volatile TrackingState lastState;
    protected volatile TrackingState nextState;

    protected final ReentrantLock queuePausedLock = new ReentrantLock();
    protected volatile boolean closed = false;

    protected long lastUpdateTime;

    public AbstractTracker(@NonNull AbstractTrackerManager manager, @NonNull IFarServerContext context) {
        this.manager = manager;

        this.context = context;
        this.coordLimits = manager.tileProvider().coordLimits();

        this.loadedPositions = DirectTilePosAccess.newPositionSet();
    }

    /**
     * Updates this tracker.
     * <p>
     * Should be called periodically to check for tiles which should be loaded or unloaded.
     */
    @CalledFromServerThread
    public void update() {
        TrackingState lastState = this.lastState;
        TrackingState nextState = this.currentState(this.context);
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
        if (this.closed) { //the tracker has been closed, exit
            return;
        }

        { //check if we need to update tracking state
            TrackingState lastState = this.lastState;
            TrackingState nextState = this.nextState;
            if (nextState != null && (lastState == null || this.shouldTriggerUpdate(lastState, nextState))) {
                //inform the server thread that this update has started, by updating the current state and clearing the next one
                this.lastState = nextState;
                this.nextState = null;

                //pause
                this.pauseQueue();

                try {
                    if (this.closed) { //the tracker has been closed, exit
                        return;
                    }

                    //untrack all the currently waiting tiles
                    //  this makes tile loading more responsive by forcing high-priority tiles to the front of the execution queue, and simplifies
                    //  synchronization logic in the rest of this class' code
                    this.clearWaiting();

                    {
                        Set<TilePos> untrackingPositions = DirectTilePosAccess.newPositionSet();
                        //actually update the tracking state (this is synchronized)
                        this.updateState(lastState, nextState, untrackingPositions);

                        //handle unloading tiles now that we no longer hold a lock
                        untrackingPositions.forEach(pos -> this.manager.stopTracking(this, pos));
                    }

                    checkState(this.waitingPositions.isEmpty(), "load queue isn't empty?!? %s", this.waitingPositions);
                } finally {
                    //unpause the queue so that we can fill it up again
                    this.unpauseQueue();
                }
            }
        }

        this.updateWaiting();
    }

    protected void updateState(TrackingState lastState, @NonNull TrackingState nextState, @NonNull Set<TilePos> untrackingPositions) {
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

    /**
     * @return whether or not the load queue is currently paused
     */
    protected boolean isQueuePaused() {
        return this.queuePausedLock.isLocked();
    }

    /**
     * Pauses the load queue.
     * <p>
     * {@link #unpauseQueue()} must be called at some point after calling this method, otherwise tile loading will stop.
     */
    protected void pauseQueue() { //synchronizing is, in fact, critical to making this work (i think)
        assert !Thread.holdsLock(this) : "current thread may not hold a lock";

        synchronized (this) {
            this.queuePausedLock.lock();
        }
    }

    /**
     * Unpauses the load queue, allowing waiting positions to be added again.
     */
    protected void unpauseQueue() {
        assert !Thread.holdsLock(this) : "current thread must hold this tracker's lock";
        assert this.isQueuePaused() : "queue must be paused";
        assert this.queuePausedLock.isHeldByCurrentThread() : "queue must be paused by the current thread";

        this.queuePausedLock.unlock();
    }

    /**
     * Stops tracking all tiles that are being waited on and re-adds them to the queue.
     * <p>
     * The queue must be paused (using {@link #pauseQueue()}) when this method is called.
     */
    protected void clearWaiting() {
        assert this.isQueuePaused() : "queue must be paused";

        //move completed positions from waitingPositions to loadedPositions
        for (TilePos pos; (pos = this.doneWaitingPositions.poll()) != null; ) {
            if (this.waitingPositions.remove(pos)) {
                this.loadedPositions.add(pos);
            } else {
                fp2().log().trace("clearWaiting: failed to promote " + pos + " from waiting to loaded, was it cancelled?");
            }
        }

        //remove the rest of the waiting positions, stop tracking them and re-add them to the load queue
        Set<TilePos> waitingPositions = DirectTilePosAccess.clonePositionsAsSet(this.waitingPositions);
        this.waitingPositions.clear();

        //stop tracking all positions in the set
        waitingPositions.forEach(pos -> this.manager.stopTracking(this, pos));

        //re-add all of the now cancelled tasks to the queue
        this.queuedPositions.addAll(waitingPositions);
    }

    /**
     * Mark completed tiles as loaded, and replaces them by beginning to wait on new positions from the queue (if possible).
     */
    protected void updateWaiting() {
        if (Thread.holdsLock(this)) { //this thread already holds this tracker's lock! to avoid recursive invocations to this.manager.beginTracking(), we'll schedule a call
            //  to doUpdate() from the tracker executor, which will eventually call updateWaiting() again.
            this.manager.scheduler().schedule(this);
            return;
        }

        int targetLoadQueueSize = fp2().globalConfig().performance().terrainThreads();
        List<TilePos> positions = DirectTilePosAccess.newPositionList();

        do {
            if (this.isQueuePaused()) { //the tracker update thread has specifically requested to pause queue polling, so we shouldn't do anything here
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
                if (this.isQueuePaused()) { //the queue might have been paused in the time between the first check and acquiring this tracker's monitor
                    return;
                }

                //move completed positions from waitingPositions to loadedPositions
                for (TilePos pos; (pos = this.doneWaitingPositions.poll()) != null; ) {
                    if (this.waitingPositions.remove(pos)) {
                        this.loadedPositions.add(pos);
                    } else {
                        fp2().log().trace("updateWaiting: failed to promote " + pos + " from waiting to loaded, was it cancelled?");
                    }
                }

                if (this.queuedPositions.isEmpty()) { //the queue is empty, so there's nothing left to do
                    return;
                }

                //keep adding positions from the queue until waitingPositions has targetLoadQueueSize elements or the queue is drained
                for (int count = targetLoadQueueSize - this.waitingPositions.size(); count > 0; count--) {
                    TilePos pos = this.queuedPositions.poll();
                    if (pos == null) { //nothing left in the queue, therefore nothing left to do!
                        break;
                    }

                    //buffer the positions we want to add in a list (we don't want to being tracking them while holding the monitor since that could deadlock)
                    positions.add(pos);
                }

                //begin tracking all of the added positions
                // unfortunately, we still have to be holding the lock while this happens, as otherwise a subsequent update may try to tell the manager to untrack a waiting position
                // before we've even begun tracking it...
                this.waitingPositions.addAll(positions);
                positions.forEach(pos -> this.manager.beginTracking(this, pos));
                positions.clear();
            } finally {
                PUnsafe.monitorExit(this);
            }
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
    protected void notifyChanged(@BorrowOwnership @NonNull ITileSnapshot snapshot) {
        try {
            this.context.sendTile(uncheckedCast(snapshot.retain()));

            TilePos pos = snapshot.pos();
            if (this.waitingPositions.contains(pos)) { //this tile has been initially loaded
                //mark the position as done waiting
                checkState(this.doneWaitingPositions.add(pos), "couldn't mark completed position as done waiting: ", pos);

                //try to fill up the load queue again to compensate for the removal of this position
                this.updateWaiting();
            }
        } catch (Throwable t) {
            this.context.tileProvider().world().workerManager().handle(t);
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
    protected void notifyUnloaded(@NonNull TilePos pos) {
        this.context.sendTileUnload(pos);
    }

    /**
     * Closes this tracker, unloading all tiles and releasing all resources.
     * <p>
     * Once this method has been called, calling any method on this instance will result in undefined behavior.
     */
    @CalledFromServerThread
    public void close() {
        //pause the queue to prevent workers from doing anything else
        this.pauseQueue();

        try {
            checkState(!this.closed, "already closed!");
            this.closed = true;

            //tell the client to unload all tiles
            this.context.sendMultiTileUnload(this.loadedPositions);

            //untrack all positions
            //  (using temporary set to avoid CME)
            {
                Set<TilePos> tmp = DirectTilePosAccess.newPositionSet();

                tmp.addAll(this.waitingPositions);
                tmp.addAll(this.loadedPositions);

                tmp.forEach(pos -> this.manager.stopTracking(this, pos));
            }

            //release everything
            this.queuedPositions.close();
            this.waitingPositions.clear();
            this.doneWaitingPositions.clear();
        } finally {
            this.unpauseQueue();
        }
    }

    public DebugStats.Tracking debugStats() {
        //i don't care that i'm calling #count() and #size() in a not thread-safe manner - worst-case scenario, the count is reported incorrectly for a split second

        return DebugStats.Tracking.builder()
                .tilesLoaded(this.loadedPositions.size())
                .tilesLoading(this.waitingPositions.size())
                .tilesQueued(this.queuedPositions.size())
                .tilesTrackedGlobal(this.manager.entries().size())
                .lastUpdateDuration(this.lastUpdateTime)
                .avgUpdateDuration(this.lastUpdateTime)
                .build();
    }

    /**
     * Computes the current {@link TrackingState} from the given context.
     *
     * @param context the {@link IFarServerContext} which we are tracking for
     * @return a new {@link TrackingState}
     */
    protected abstract TrackingState currentState(@NonNull IFarServerContext context);

    /**
     * Checks whether or not the difference between two given {@link TrackingState}s is sufficiently drastic to warrant triggering a tracking update.
     *
     * @param oldState the old {@link TrackingState}
     * @param newState the new {@link TrackingState}
     * @return whether or not a tracking update should be triggered
     */
    protected abstract boolean shouldTriggerUpdate(@NonNull TrackingState oldState, @NonNull TrackingState newState);

    /**
     * Enumerates every tile position visible in the given {@link TrackingState}.
     *
     * @param state    the {@link TrackingState}
     * @param callback a callback function which should be called once for every visible tile position
     */
    protected abstract void allPositions(@NonNull TrackingState state, @NonNull Consumer<TilePos> callback);

    /**
     * Computes the tile positions whose visibility changed between two given {@link TrackingState}s.
     *
     * @param oldState the old {@link TrackingState}
     * @param newState the new {@link TrackingState}
     * @param added    a callback function which should be called once for every tile position which was not visible in the old state but is now visible
     * @param removed  a callback function which should be called once for every tile position which was visible in the old state but is no longer visible
     */
    protected abstract void deltaPositions(@NonNull TrackingState oldState, @NonNull TrackingState newState, @NonNull Consumer<TilePos> added, @NonNull Consumer<TilePos> removed);

    /**
     * Checks whether or not the given tile position is visible in the given state.
     *
     * @param state the {@link TrackingState}
     * @param pos   the tile position to check
     * @return whether or not the tile position is visible
     */
    protected abstract boolean isVisible(@NonNull TrackingState state, @NonNull TilePos pos);

    /**
     * Gets a {@link Comparator} which can be used for sorting the tile positions visible in the given {@link TrackingState} by their load priority.
     *
     * @param state the {@link TrackingState}
     * @return a {@link Comparator} for sorting visible tile positions
     */
    protected abstract Comparator<TilePos> comparatorFor(@NonNull TrackingState state);
}
