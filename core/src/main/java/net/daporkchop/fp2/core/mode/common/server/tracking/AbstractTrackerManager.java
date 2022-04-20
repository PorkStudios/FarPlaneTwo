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

package net.daporkchop.fp2.core.mode.common.server.tracking;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.storage.FTileStorage;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTracker;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;
import net.daporkchop.fp2.core.util.datastructure.CompactReferenceArraySet;
import net.daporkchop.fp2.core.util.threading.scheduler.NoFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractTrackerManager<POS extends IFarPos, T extends IFarTile> implements IFarTrackerManager<POS, T>, FTileStorage.Listener<POS, T> {
    protected final IFarTileProvider<POS, T> tileProvider;

    protected final Map<POS, Entry> entries = new ConcurrentHashMap<>();
    protected final Map<IFarServerContext<POS, T>, AbstractTracker<POS, T, ?>> trackers = new IdentityHashMap<>();

    protected final Scheduler<AbstractTracker<POS, T, ?>, Void> scheduler; //TODO: make this global rather than per-mode and per-dimension

    protected final int generationThreads = fp2().globalConfig().performance().terrainThreads();

    public AbstractTrackerManager(@NonNull IFarTileProvider<POS, T> tileProvider) {
        this.tileProvider = tileProvider;

        this.scheduler = new NoFutureScheduler<>(AbstractTracker::doUpdate,
                tileProvider.world().workerManager().createChildWorkerGroup()
                        .threads(fp2().globalConfig().performance().trackingThreads())
                        .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId()
                                .name(PStrings.fastFormat("FP2 %s %s Tracker #%%d", tileProvider.mode().name(), tileProvider.world().id())).build()));

        tileProvider.storage().addListener(this);
    }

    @CalledFromServerThread
    @Override
    public void close() {
        this.tileProvider.storage().removeListener(this);

        this.scheduler.close();
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
        new AbstractEntryOperation_VoidIfPresent(handle.pos()) {
            @Override
            protected void run(@NonNull Entry entry) {
                entry.tileLoaded(handle);
            }
        }.run();
    }

    protected void tileUpdated(@NonNull ITileHandle<POS, T> handle) {
        new AbstractEntryOperation_VoidIfPresent(handle.pos()) {
            @Override
            protected void run(@NonNull Entry entry) {
                entry.tileUpdated(handle);
            }
        }.run();
    }

    @Override
    public void tilesChanged(@NonNull Stream<POS> positions) {
    }

    @Override
    public void tilesDirty(@NonNull Stream<POS> positions) {
        positions.forEach(pos -> new AbstractEntryOperation_VoidIfPresent(pos) {
            @Override
            protected void run(@NonNull Entry entry) {
                entry.tileDirty();
            }
        }.run());
    }

    protected void recheckDirty(@NonNull ITileHandle<POS, T> handle) {
        new AbstractEntryOperation_VoidIfPresent(handle.pos()) {
            @Override
            protected void run(@NonNull Entry entry) {
                entry.checkDirty(handle);
            }
        }.run();
    }

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
                } else if (PUnsafe.tryMonitorEnter(entry)) {
                    this.entry = entry;
                }

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
        do {
            this.entries.compute(posIn, state);
        } while (state.entry == null);
        state.run();
    }

    protected void stopTracking(@NonNull AbstractTracker<POS, T, ?> tracker, @NonNull POS posIn) {
        class State implements BiFunction<POS, Entry, Entry> {
            boolean spin;

            @Override
            public Entry apply(@NonNull POS pos, Entry entry) {
                checkState(entry != null, "cannot remove player %s from non-existent tracking entry at %s", tracker, pos);

                if (!PUnsafe.tryMonitorEnter(entry)) { //failed to acquire a lock, break out and spin
                    this.spin = true;
                    return entry;
                }

                try {
                    this.spin = false;
                    return entry.removeTracker(tracker);
                } finally {
                    PUnsafe.monitorExit(entry);
                }
            }
        }

        State state = new State();
        do {
            this.entries.compute(posIn, state);
        } while (state.spin);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected abstract class AbstractEntryOperation_VoidIfPresent implements BiFunction<POS, Entry, Entry>, Runnable {
        @NonNull
        protected final POS pos;

        protected Entry entry;
        protected boolean spin;

        @Override
        public Entry apply(@NonNull POS pos, @NonNull Entry entry) {
            this.entry = entry;
            this.spin = !PUnsafe.tryMonitorEnter(entry);

            return entry;
        }

        @Override
        public void run() {
            do {
                AbstractTrackerManager.this.entries.computeIfPresent(this.pos, this);
            } while (this.spin);

            if (this.entry != null) {
                try {
                    this.run(this.entry);
                } finally {
                    PUnsafe.monitorExit(this.entry);
                }
            }
        }

        protected abstract void run(@NonNull Entry entry);
    }

    /**
     * Associates a tile position to the {@link AbstractTracker}s which are tracking it.
     *
     * @author DaPorkchop_
     */
    @ToString(callSuper = true)
    protected class Entry extends CompactReferenceArraySet<AbstractTracker<POS, T, ?>> implements Consumer<ITileHandle<POS, T>>, Function<ITileHandle<POS, T>, Void> {
        //we're using an ArraySet because even though all the operations run in O(n) time, it shouldn't ever be an issue - this should still be plenty fast even if there are
        //  hundreds of players tracking the same tile, and it uses a fair amount less memory than an equivalent HashSet.
        //we extend from CompactReferenceArraySet rather than having it as a field in order to minimize memory wasted by JVM object headers and the likes, as well as reduced
        //  pointer chasing.

        protected final POS pos;

        protected CompletableFuture<ITileHandle<POS, T>> loadFuture;
        protected CompletableFuture<ITileHandle<POS, T>> updateFuture;
        protected Set<AbstractTracker<POS, T, ?>> trackersWaitingForLoad; //all tracker instances which are waiting for the load future to be completed, or null if empty

        protected long lastSentTimestamp = ITileMetadata.TIMESTAMP_BLANK;

        public Entry(@NonNull POS pos) {
            this.pos = pos;
        }

        public void addTracker(@NonNull AbstractTracker<POS, T, ?> tracker) {
            checkState(super.add(tracker), "player %s was already added to entry %s!", tracker, this);

            this.addWaitingForLoad(tracker);
        }

        public Entry removeTracker(@NonNull AbstractTracker<POS, T, ?> tracker) {
            checkState(super.remove(tracker), "player %s did not belong to entry %s!", tracker, this);

            if (!this.removeWaitingForLoad(tracker)) { //the tracker wasn't waiting for the tile to be loaded, which means it's already been loaded for the tracker and we need to
                //  send a tile unload notification now
                tracker.notifyUnloaded(this.pos);
            }

            if (super.isEmpty()) { //no more players are tracking this entry, so it can be removed
                //release the tile update future if present (potentially removing it from the scheduler)
                if (this.updateFuture != null) {
                    this.updateFuture.cancel(false);
                    this.updateFuture = null;
                }

                //this entry should be replaced with null
                return null;
            } else {
                return this;
            }
        }

        protected void addWaitingForLoad(@NonNull AbstractTracker<POS, T, ?> tracker) {
            if (this.trackersWaitingForLoad == null) {
                this.trackersWaitingForLoad = new CompactReferenceArraySet<>();
            }

            checkState(this.trackersWaitingForLoad.add(tracker), "already waiting for load: %s", tracker);

            if (this.loadFuture == null) { //loadFuture isn't set, schedule a new one
                this.loadFuture = AbstractTrackerManager.this.tileProvider.requestLoad(this.pos);
                this.loadFuture.thenAccept(this);
            }
        }

        protected boolean isWaitingForLoad(@NonNull AbstractTracker<POS, T, ?> tracker) {
            return this.trackersWaitingForLoad != null && this.trackersWaitingForLoad.contains(tracker);
        }

        protected boolean removeWaitingForLoad(@NonNull AbstractTracker<POS, T, ?> tracker) {
            if (this.trackersWaitingForLoad != null && this.trackersWaitingForLoad.remove(tracker)) {
                if (this.trackersWaitingForLoad.isEmpty()) { //set is now empty, set it to null to save memory
                    this.trackersWaitingForLoad = null;

                    //cancel the load future since it's no longer needed
                    this.loadFuture.cancel(false);
                    this.loadFuture = null;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * @deprecated internal API, do not touch!
         */
        @Override
        @Deprecated
        public void accept(@NonNull ITileHandle<POS, T> handle) { //used by loadFuture
            if (Thread.holdsLock(this)) { //the thenAccept callback was fired immediately
                this.tileLoaded(handle);
            } else { //future was completed from another thread - go through TrackerManager in order to acquire a lock
                AbstractTrackerManager.this.tileLoaded(handle);
            }
        }

        public void tileLoaded(@NonNull ITileHandle<POS, T> handle) {
            checkState(handle.isInitialized(), "handle at %s hasn't been initialized yet!", this.pos);

            if (this.loadFuture != null) { //cancel loadFuture if needed
                this.loadFuture.cancel(false);
                this.loadFuture = null;
            }

            ITileSnapshot<POS, T> snapshot = handle.snapshot();
            if (snapshot.timestamp() > this.lastSentTimestamp) { //tile is newer than the tile previously sent to all trackers, so we'll broadcast it to everyone
                this.lastSentTimestamp = snapshot.timestamp();

                super.forEach(tracker -> tracker.notifyChanged(snapshot));

                //all of the trackers which were waiting for load have been notified as well
                this.trackersWaitingForLoad = null;
            } else if (this.trackersWaitingForLoad != null) { //notify only the players which were waiting for the tile to be loaded
                this.trackersWaitingForLoad.forEach(tracker -> tracker.notifyChanged(snapshot));
                this.trackersWaitingForLoad = null;
            }

            this.checkDirty(handle);
        }

        public void tileUpdated(@NonNull ITileHandle<POS, T> handle) {
            checkState(handle.isInitialized(), "handle at %s hasn't been initialized yet!", this.pos);

            checkState(this.updateFuture != null, "tileUpdated called at %s even though it wasn't scheduled for an update!", this.pos);
            checkState(this.updateFuture.isDone(), "tileUpdated called at %s even though it wasn't complete!", this.pos);
            this.updateFuture = null;

            ITileSnapshot<POS, T> snapshot = handle.snapshot();
            if (snapshot.timestamp() > this.lastSentTimestamp) { //tile is newer than the tile previously sent to all trackers, so we'll broadcast it to all the trackers
                //  which aren't waiting for an initial load
                this.lastSentTimestamp = snapshot.timestamp();

                super.forEach(tracker -> {
                    if (!this.isWaitingForLoad(tracker)) {
                        tracker.notifyChanged(snapshot);
                    }
                });
            }

            this.checkDirty(handle);
        }

        public void tileDirty() {
            this.checkDirty(AbstractTrackerManager.this.tileProvider.storage().handleFor(this.pos));
        }

        protected void checkDirty(@NonNull ITileHandle<POS, T> handle) {
            if (false) { //TODO: re-enable updates
                //TODO: figure out why i had this disabled
                return;
            }

            if (this.updateFuture != null && this.updateFuture.isDone()) { //an update was previously pending and has been completed
                this.updateFuture = null;
            }

            if (this.updateFuture != null //tile is still being loaded or is already being updated, we shouldn't try to update it again
                || handle.dirtyTimestamp() == ITileMetadata.TIMESTAMP_BLANK) { //tile isn't dirty, no need to update
                return;
            }

            //schedule a new update task for this tile
            this.updateFuture = AbstractTrackerManager.this.tileProvider.requestUpdate(this.pos);
            this.updateFuture.thenApply(this);
        }

        /**
         * @deprecated internal API, do not touch!
         */
        @Override
        @Deprecated
        public Void apply(ITileHandle<POS, T> handle) { //used by updateFuture
            if (Thread.holdsLock(this)) { //the thenApply callback was fired immediately
                this.tileUpdated(handle);
            } else { //future was completed from another thread - go through TrackerManager in order to acquire a lock
                AbstractTrackerManager.this.tileUpdated(handle);
            }
            return null;
        }
    }
}
