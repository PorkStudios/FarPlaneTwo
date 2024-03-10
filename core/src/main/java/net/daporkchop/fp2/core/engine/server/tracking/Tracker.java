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

package net.daporkchop.fp2.core.engine.server.tracking;

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TileCoordLimits;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.ctx.ServerContext;
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

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Per-context tracker instance.
 * <p>
 * This provides the generic functionality for scheduling tracker updates, manages the queue of tiles to be loaded and schedules loads for new tiles as necessary.
 *
 * @author DaPorkchop_
 * @see TrackerManager
 */
public final class Tracker {
    /**
     * The squared distance a player must move from their previous position in order to trigger a tracking update.
     * <p>
     * The default value of {@code (VT_VOXELS / 2)²} is based on the equivalent value of {@code 64} (which is {@code (CHUNK_SIZE / 2)²}) used by vanilla.
     */
    private static final double UPDATE_TRIGGER_DISTANCE_SQUARED = sq(T_VOXELS >> 1);

    private static boolean overlaps(int x0, int y0, int z0, int x1, int y1, int z1, int radius) {
        int dx = abs(x0 - x1);
        int dy = abs(y0 - y1);
        int dz = abs(z0 - z1);
        return dx <= radius && dy <= radius && dz <= radius;
    }

    private final TrackerManager manager;

    private final ServerContext context;
    private final TileCoordLimits coordLimits;

    /**
     * The actual queue of positions to load.
     * <p>
     * May only be accessed while holding this tracker's lock. All operations other than polling additionally require that the queue be paused.
     */
    private final RecyclingArrayDeque<TilePos> queuedPositions = new RecyclingArrayDeque<>();

    /**
     * The set of positions which are loaded.
     */
    private final Set<TilePos> loadedPositions;
    private final Set<TilePos> waitingPositions = ConcurrentHashMap.newKeySet();
    private final Queue<TilePos> doneWaitingPositions = new ConcurrentLinkedQueue<>();

    //these are using a single object reference instead of flattened fields to allow the value to be replaced atomically. to ensure coherent access to the values,
    // readers must take care never to dereference the fields more than once.
    private volatile TrackingState lastState;
    private volatile TrackingState nextState;

    private final ReentrantLock queuePausedLock = new ReentrantLock();
    private volatile boolean closed = false;

    private long lastUpdateTime;

    public Tracker(@NonNull TrackerManager manager, @NonNull ServerContext context) {
        this.manager = manager;

        this.context = context;
        this.coordLimits = manager.tileProvider().coordLimits();

        this.loadedPositions = DirectTilePosAccess.newPositionHashSet();
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
    void doUpdate() {
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
                        Set<TilePos> untrackingPositions = DirectTilePosAccess.newPositionHashSet();
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

    private void updateState(TrackingState lastState, @NonNull TrackingState nextState, @NonNull Set<TilePos> untrackingPositions) {
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
    private boolean isQueuePaused() {
        return this.queuePausedLock.isLocked();
    }

    /**
     * Pauses the load queue.
     * <p>
     * {@link #unpauseQueue()} must be called at some point after calling this method, otherwise tile loading will stop.
     */
    private void pauseQueue() { //synchronizing is, in fact, critical to making this work (i think)
        assert !Thread.holdsLock(this) : "current thread may not hold a lock";

        synchronized (this) {
            this.queuePausedLock.lock();
        }
    }

    /**
     * Unpauses the load queue, allowing waiting positions to be added again.
     */
    private void unpauseQueue() {
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
    private void clearWaiting() {
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
        Set<TilePos> waitingPositions = DirectTilePosAccess.clonePositionsAsHashSet(this.waitingPositions);
        this.waitingPositions.clear();

        //stop tracking all positions in the set
        waitingPositions.forEach(pos -> this.manager.stopTracking(this, pos));

        //re-add all of the now cancelled tasks to the queue
        this.queuedPositions.addAll(waitingPositions);
    }

    /**
     * Mark completed tiles as loaded, and replaces them by beginning to wait on new positions from the queue (if possible).
     */
    private void updateWaiting() {
        if (Thread.holdsLock(this)) { //this thread already holds this tracker's lock! to avoid recursive invocations to this.manager.beginTracking(), we'll schedule a call
            //  to doUpdate() from the tracker executor, which will eventually call updateWaiting() again.
            this.manager.scheduler().schedule(this);
            return;
        }

        int targetLoadQueueSize = fp2().globalConfig().performance().terrainThreads();
        List<TilePos> positions = DirectTilePosAccess.newPositionArrayList();

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
                for (int count = targetLoadQueueSize - this.waitingPositions.size() - this.context.queuedTilesToSend(); count > 0; count--) {
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
    void notifyChanged(@BorrowOwnership @NonNull ITileSnapshot snapshot) {
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
    void notifyUnloaded(@NonNull TilePos pos) {
        this.context.sendTileUnload(pos);
    }

    /**
     * Notifies the tracker that some tile data which was queued for unload has been sent.
     */
    @CalledFromServerThread
    public void notifyTilesSent() {
        //this will eventually call doUpdate(), which will call updateWaiting()
        this.manager.scheduler().schedule(this);
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

            //untrack all positions
            //  (using temporary set to avoid CME)
            {
                Set<TilePos> tmp = DirectTilePosAccess.newPositionHashSet();

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
     * @param context the {@link ServerContext} which we are tracking for
     * @return a new {@link TrackingState}
     */
    private TrackingState currentState(@NonNull ServerContext context) {
        return TrackingState.createDefault(context, T_SHIFT);
    }

    /**
     * Checks whether or not the difference between two given {@link TrackingState}s is sufficiently drastic to warrant triggering a tracking update.
     *
     * @param oldState the old {@link TrackingState}
     * @param newState the new {@link TrackingState}
     * @return whether or not a tracking update should be triggered
     */
    private boolean shouldTriggerUpdate(@NonNull TrackingState oldState, @NonNull TrackingState newState) {
        return oldState.cutoff() != newState.cutoff()
               || oldState.minLevel() != newState.minLevel()
               || oldState.maxLevel() != newState.maxLevel()
               || sq(oldState.x() - newState.x()) + sq(oldState.y() - newState.y()) + sq(oldState.z() - newState.z()) >= UPDATE_TRIGGER_DISTANCE_SQUARED;
    }

    /**
     * Enumerates every tile position visible in the given {@link TrackingState}.
     *
     * @param state    the {@link TrackingState}
     * @param callback a callback function which should be called once for every visible tile position
     */
    private void allPositions(@NonNull TrackingState state, @NonNull Consumer<TilePos> callback) {
        final int playerX = floorI(state.x());
        final int playerY = floorI(state.y());
        final int playerZ = floorI(state.z());

        for (int lvl = state.minLevel(); lvl < state.maxLevel(); lvl++) {
            final int baseX = asrRound(playerX, T_SHIFT + lvl);
            final int baseY = asrRound(playerY, T_SHIFT + lvl);
            final int baseZ = asrRound(playerZ, T_SHIFT + lvl);

            TilePos min = this.coordLimits.min(lvl);
            TilePos max = this.coordLimits.max(lvl);
            int minX = max(baseX - state.cutoff(), min.x());
            int minY = max(baseY - state.cutoff(), min.y());
            int minZ = max(baseZ - state.cutoff(), min.z());
            int maxX = min(baseX + state.cutoff(), max.x() - 1);
            int maxY = min(baseY + state.cutoff(), max.y() - 1);
            int maxZ = min(baseZ + state.cutoff(), max.z() - 1);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        callback.accept(new TilePos(lvl, x, y, z));
                    }
                }
            }
        }
    }

    /**
     * Computes the tile positions whose visibility changed between two given {@link TrackingState}s.
     *
     * @param oldState the old {@link TrackingState}
     * @param newState the new {@link TrackingState}
     * @param added    a callback function which should be called once for every tile position which was not visible in the old state but is now visible
     * @param removed  a callback function which should be called once for every tile position which was visible in the old state but is no longer visible
     */
    private void deltaPositions(@NonNull TrackingState oldState, @NonNull TrackingState newState, @NonNull Consumer<TilePos> added, @NonNull Consumer<TilePos> removed) {
        final int oldPlayerX = floorI(oldState.x());
        final int oldPlayerY = floorI(oldState.y());
        final int oldPlayerZ = floorI(oldState.z());
        final int newPlayerX = floorI(newState.x());
        final int newPlayerY = floorI(newState.y());
        final int newPlayerZ = floorI(newState.z());

        for (int lvl = min(oldState.minLevel(), newState.minLevel()); lvl < max(oldState.maxLevel(), newState.maxLevel()); lvl++) {
            final int oldBaseX = asrRound(oldPlayerX, T_SHIFT + lvl);
            final int oldBaseY = asrRound(oldPlayerY, T_SHIFT + lvl);
            final int oldBaseZ = asrRound(oldPlayerZ, T_SHIFT + lvl);
            final int newBaseX = asrRound(newPlayerX, T_SHIFT + lvl);
            final int newBaseY = asrRound(newPlayerY, T_SHIFT + lvl);
            final int newBaseZ = asrRound(newPlayerZ, T_SHIFT + lvl);

            if (oldState.hasLevel(lvl) && newState.hasLevel(lvl) && oldState.cutoff() == newState.cutoff()
                && oldBaseX == newBaseX && oldBaseY == newBaseY && oldBaseZ == newBaseZ) { //nothing changed, skip this level
                continue;
            }

            TilePos min = this.coordLimits.min(lvl);
            TilePos max = this.coordLimits.max(lvl);

            //removed positions
            if (!newState.hasLevel(lvl) || oldState.hasLevel(lvl)) {
                int minX = max(oldBaseX - oldState.cutoff(), min.x());
                int minY = max(oldBaseY - oldState.cutoff(), min.y());
                int minZ = max(oldBaseZ - oldState.cutoff(), min.z());
                int maxX = min(oldBaseX + oldState.cutoff(), max.x() - 1);
                int maxY = min(oldBaseY + oldState.cutoff(), max.y() - 1);
                int maxZ = min(oldBaseZ + oldState.cutoff(), max.z() - 1);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (!newState.hasLevel(lvl) || !overlaps(x, y, z, newBaseX, newBaseY, newBaseZ, newState.cutoff())) {
                                removed.accept(new TilePos(lvl, x, y, z));
                            }
                        }
                    }
                }
            }

            //added positions
            if (!oldState.hasLevel(lvl) || newState.hasLevel(lvl)) {
                int minX = max(newBaseX - newState.cutoff(), min.x());
                int minY = max(newBaseY - newState.cutoff(), min.y());
                int minZ = max(newBaseZ - newState.cutoff(), min.z());
                int maxX = min(newBaseX + newState.cutoff(), max.x() - 1);
                int maxY = min(newBaseY + newState.cutoff(), max.y() - 1);
                int maxZ = min(newBaseZ + newState.cutoff(), max.z() - 1);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (!oldState.hasLevel(lvl) || !overlaps(x, y, z, oldBaseX, oldBaseY, oldBaseZ, oldState.cutoff())) {
                                added.accept(new TilePos(lvl, x, y, z));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks whether or not the given tile position is visible in the given state.
     *
     * @param state the {@link TrackingState}
     * @param pos   the tile position to check
     * @return whether or not the tile position is visible
     */
    private boolean isVisible(@NonNull TrackingState state, @NonNull TilePos pos) {
        return state.hasLevel(pos.level())
               && this.coordLimits.contains(pos)
               && abs(pos.x() - asrRound(floorI(state.x()), T_SHIFT + pos.level())) <= state.cutoff()
               && abs(pos.y() - asrRound(floorI(state.y()), T_SHIFT + pos.level())) <= state.cutoff()
               && abs(pos.z() - asrRound(floorI(state.z()), T_SHIFT + pos.level())) <= state.cutoff();
    }

    /**
     * Gets a {@link Comparator} which can be used for sorting the tile positions visible in the given {@link TrackingState} by their load priority.
     *
     * @param state the {@link TrackingState}
     * @return a {@link Comparator} for sorting visible tile positions
     */
    private Comparator<TilePos> comparatorFor(@NonNull TrackingState state) {
        class TilePosAndComparator extends TilePos implements Comparator<TilePos> {
            public TilePosAndComparator(int level, int x, int y, int z) {
                super(level, x, y, z);
            }

            @Override
            public int compare(TilePos o1, TilePos o2) {
                int d;
                if ((d = o1.level() - o2.level()) != 0) {
                    return d;
                }
                return Integer.compare(this.manhattanDistance(o1), this.manhattanDistance(o2));
            }
        }

        return new TilePosAndComparator(0, asrRound(floorI(state.x()), T_SHIFT), asrRound(floorI(state.y()), T_SHIFT), asrRound(floorI(state.z()), T_SHIFT));
    }
}
