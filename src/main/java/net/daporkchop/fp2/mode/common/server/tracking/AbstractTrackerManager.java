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

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.mode.api.server.tracking.IFarTracker;
import net.daporkchop.fp2.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.util.annotation.CalledFromServerThread;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.fp2.util.datastructure.CompactReferenceArraySet;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.fp2.util.threading.scheduler.NoFutureScheduler;
import net.daporkchop.fp2.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractTrackerManager<POS extends IFarPos, T extends IFarTile> implements IFarTrackerManager<POS, T>, IFarStorage.Listener<POS, T> {
    protected final IFarTileProvider<POS, T> tileProvider;

    protected final Map<POS, Entry> entries = new ConcurrentHashMap<>();
    protected final Map<IFarServerContext<POS, T>, AbstractTracker<POS, T, ?>> trackers = new IdentityHashMap<>();

    protected final Scheduler<AbstractTracker<POS, T, ?>, Void> scheduler; //TODO: make this global rather than per-mode and per-dimension

    protected final int generationThreads = FP2Config.global().performance().terrainThreads();

    public AbstractTrackerManager(@NonNull IFarTileProvider<POS, T> tileProvider) {
        this.tileProvider = tileProvider;

        this.scheduler = new NoFutureScheduler<>(AbstractTracker::doUpdate,
                ThreadingHelper.workerGroupBuilder()
                        .world(tileProvider.world())
                        .threads(FP2Config.global().performance().trackingThreads())
                        .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId()
                                .name(PStrings.fastFormat("FP2 %s DIM%d Tracker #%%d", tileProvider.mode().name(), ((IFarWorldServer) tileProvider.world()).fp2_IFarWorld_dimensionId())).build()));

        tileProvider.storage().addListener(this);
    }

    @CalledFromServerThread
    @Override
    public void close() {
        this.tileProvider.storage().removeListener(this);
    }

    @CalledFromServerThread
    @Override
    public IFarTracker<POS, T> beginTracking(@NonNull IFarServerContext<POS, T> context) {
        return this.trackers.compute(context, (ctx, tracker) -> {
            checkArg(tracker == null, "tracker for %s already exists!", ctx);

            return this.createTrackerFor(ctx);
        });
    }

    /**
     * Creates a new {@link AbstractTracker} instance for the given {@link IFarServerContext}.
     *
     * @param context the {@link IFarServerContext}
     * @return a new {@link AbstractTracker}
     */
    protected abstract AbstractTracker<POS, T, ?> createTrackerFor(@NonNull IFarServerContext<POS, T> context);

    protected void tileLoaded(@NonNull ITileHandle<POS, T> handle) {
        class State implements BiFunction<POS, Entry, Entry>, Runnable {
            Entry entry;

            @Override
            public Entry apply(@NonNull POS pos, @NonNull Entry entry) {
                PUnsafe.monitorEnter(entry);
                this.entry = entry;

                return entry;
            }

            @Override
            public void run() {
                if (this.entry != null) {
                    try {
                        this.entry.tileLoaded(handle);
                    } finally {
                        PUnsafe.monitorExit(this.entry);
                    }
                }
            }
        }

        State state = new State();
        this.entries.computeIfPresent(handle.pos(), state);
        state.run();
    }

    @Override
    public void tilesChanged(@NonNull Stream<POS> positions) {
        /*BiFunction<POS, Entry, Entry> func = (pos, entry) -> {
            //notify entry that the tile has been changed
            entry.tileChanged();

            return entry;
        };

        positions.forEach(pos -> this.entries.computeIfPresent(pos, func));*/
        //TODO: this
    }

    @Override
    public void tilesDirty(@NonNull Stream<POS> positions) {
        /*BiFunction<POS, Entry, Entry> func = (pos, entry) -> {
            //notify entry that the tile is now dirty
            entry.tileDirty();

            return entry;
        };

        positions.forEach(pos -> this.entries.computeIfPresent(pos, func));*/
        //TODO: this
    }

    protected void recheckDirty(@NonNull ITileHandle<POS, T> handle) {
        /*this.entries.computeIfPresent(handle.pos(), (pos, entry) -> {
            //notify entry that the tile has been loaded
            entry.checkDirty();

            return entry;
        });*/
        //TODO: this
    }

    @DebugOnly
    @CalledFromServerThread
    @Override
    public void dropAllTiles() {
        /*Collection<IFarServerContext<POS, T>> trackedContextsSnapshot = new ArrayList<>(this.contexts.keySet());
        trackedContextsSnapshot.forEach(this::playerRemove);

        //it should be reasonably safe to assume that all pending tasks will have been cancelled by now

        trackedContextsSnapshot.forEach(this::playerAdd);*/
        //TODO: this
    }

    protected void beginTracking(@NonNull AbstractTracker<POS, T, ?> tracker, @NonNull POS posIn) {
        class State implements BiFunction<POS, Entry, Entry>, Runnable {
            Entry entry;

            @Override
            public Entry apply(@NonNull POS pos, Entry entry) {
                if (entry == null) { //no entry exists at this position, so we should make a new one
                    entry = new Entry(pos);
                }

                PUnsafe.monitorEnter(entry);
                this.entry = entry;

                return entry;
            }

            @Override
            public void run() {
                try {
                    this.entry.addTracker(tracker);
                } finally {
                    PUnsafe.monitorExit(this.entry);
                }
            }
        }

        State state = new State();
        this.entries.compute(posIn, state);
        state.run();
    }

    protected void stopTracking(@NonNull AbstractTracker<POS, T, ?> tracker, @NonNull POS posIn) {
        class State implements BiFunction<POS, Entry, Entry> {
            boolean repeat;

            @Override
            public Entry apply(@NonNull POS pos, Entry entry) {
                checkState(entry != null, "cannot remove player %s from non-existent tracking entry at %s", tracker, pos);

                if (!PUnsafe.tryMonitorEnter(entry)) { //failed to acquire a lock, break out and spin
                    this.repeat = true;
                    return entry;
                }

                try {
                    this.repeat = false;
                    return entry.removeTracker(tracker);
                } finally {
                    PUnsafe.monitorExit(entry);
                }
            }
        }

        State state = new State();
        do {
            this.entries.compute(posIn, state);
        } while (state.repeat);
    }

    /**
     * Associates a tile position to the {@link AbstractTracker}s which are tracking it.
     *
     * @author DaPorkchop_
     */
    @ToString
    protected class Entry extends CompactReferenceArraySet<AbstractTracker<POS, T, ?>> implements Consumer<ITileHandle<POS, T>> {
        //we're using an ArraySet because even though all the operations run in O(n) time, it shouldn't ever be an issue - this should still be plenty fast even if there are
        //  hundreds of players tracking the same tile, and it uses a fair amount less memory than an equivalent HashSet.
        //we extend from CompactReferenceArraySet rather than having it as a field in order to minimize memory wasted by JVM object headers and the likes, as well as reduced
        //  pointer chasing.

        protected final POS pos;

        protected CompletableFuture<ITileHandle<POS, T>> future;
        protected ITileHandle<POS, T> handle;

        public Entry(@NonNull POS pos) {
            this.pos = pos;

            synchronized (this) { //synchronize here so that we can detect whether or not the callback was immediate in #accept(ITileHandle)
                this.future = AbstractTrackerManager.this.tileProvider.requestLoad(pos);
                this.future.thenAccept(this);
            }
        }

        /**
         * @deprecated internal API, do not touch!
         */
        @Override
        @Deprecated
        public void accept(@NonNull ITileHandle<POS, T> handle) {
            if (Thread.holdsLock(this)) { //the thenAccept callback was fired immediately (from the constructor)
                this.tileLoaded(handle);
            } else { //future was completed from another thread - go through TrackerManager in order to acquire a lock
                AbstractTrackerManager.this.tileLoaded(handle);
            }
        }

        public void tileLoaded(@NonNull ITileHandle<POS, T> handle) {
            checkState(this.handle == null, "already loaded handle at %s?!?", this.pos);
            this.future = null;
            this.handle = handle;

            this.notifyTrackers();
            this.checkDirty();
        }

        public void addTracker(@NonNull AbstractTracker<POS, T, ?> tracker) {
            checkState(super.add(tracker), "player %s was already added to entry %s!", tracker, this);

            tracker.notifyStartedTracking(this.pos);

            if (this.handle != null) { //the tile has already been initialized, let's send it to the player
                tracker.notifyLoaded(this.handle.snapshot());
            }
        }

        public Entry removeTracker(@NonNull AbstractTracker<POS, T, ?> tracker) {
            checkState(super.remove(tracker), "player %s did not belong to entry %s!", tracker, this);

            if (this.handle != null) {
                tracker.notifyUnloaded(this.pos);
            }

            tracker.notifyStoppedTracking(this.pos);

            if (super.isEmpty()) { //no more players are tracking this entry, so it can be removed
                //release the tile load/update future (potentially removing it from the scheduler)
                if (this.future != null) {
                    this.future.cancel(false);
                    this.future = null;
                }

                //this entry should be replaced with null
                return null;
            } else {
                return this;
            }
        }

        public void tileChanged() {
            if (this.handle == null) { //tile hasn't been loaded yet, we don't care about any updates
                return;
            }

            this.notifyTrackers();
            this.checkDirty();
        }

        protected void notifyTrackers() {
            checkState(this.handle.isInitialized(), "handle at %s hasn't been initialized yet!", this.pos);

            //notify all players which have this tile loaded
            ITileSnapshot<POS, T> snapshot = this.handle.snapshot();
            super.forEach(tracker -> tracker.notifyLoaded(snapshot));
        }

        public void tileDirty() {
            if (this.handle == null) { //tile hasn't been loaded yet, we don't care about any updates
                return;
            }

            this.checkDirty();
        }

        protected void checkDirty() {
            if (true) { //TODO: this
                return;
            }

            if (this.future != null && this.future.isDone()) { //an update was previously pending and has been completed
                this.future = null;
            }

            if (this.future != null //tile is still being loaded or is already being updated, we shouldn't try to update it again
                || this.handle.dirtyTimestamp() == ITileMetadata.TIMESTAMP_BLANK) { //tile isn't dirty, no need to update
                return;
            }

            //schedule a new update task for this tile
            this.future = AbstractTrackerManager.this.tileProvider.requestUpdate(this.pos);
            this.future.thenAccept(AbstractTrackerManager.this::recheckDirty);
        }
    }
}
