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

package net.daporkchop.fp2.mode.common.server;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.net.server.SPacketTileData;
import net.daporkchop.fp2.net.server.SPacketUnloadTile;
import net.daporkchop.fp2.net.server.SPacketUnloadTiles;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.datastructure.CompactReferenceArraySet;
import net.daporkchop.fp2.util.datastructure.RecyclingArrayDeque;
import net.daporkchop.fp2.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractPlayerTracker<POS extends IFarPos, T extends IFarTile, STATE> implements IFarPlayerTracker<POS, T> {
    private static final long ABSTRACTRELEASABLE_RELEASED_OFFSET = PUnsafe.pork_getOffset(AbstractReleasable.class, "released");

    /**
     * The squared distance a player must move from their previous position in order to trigger a tracking update.
     * <p>
     * The default value of {@code (T_VOXELS / 2)²} is based on the equivalent value of {@code 64} (which is {@code (CHUNK_SIZE / 2)²}) used by vanilla.
     */
    protected static final double UPDATE_TRIGGER_DISTANCE_SQUARED = sq(T_VOXELS >> 1);

    /**
     * An additional radius (in tiles) to load around players in order to prevent visible artifacts when moving.
     * <p>
     * TODO: this should be removed after merging dev/fix-holes-with-stencil-hackery
     */
    protected static final int TILE_PRELOAD_PADDING_RADIUS = 3;

    //TODO: consider using a KeyedExecutor for this
    protected static final EventExecutorGroup TRACKER_THREADS = new DefaultEventLoopGroup(
            FP2Config.performance.trackingThreads,
            PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Player Tracker #%d").build());

    protected final IFarWorld<POS, T> world;
    protected final IntAxisAlignedBB[] coordLimits;

    protected final Map<POS, Entry> entries = new ConcurrentHashMap<>();
    protected final Map<EntityPlayerMP, Context> contexts = new ConcurrentHashMap<>();

    public AbstractPlayerTracker(@NonNull IFarWorld<POS, T> world) {
        this.world = world;
        this.coordLimits = ((IFarWorldServer) world.world()).fp2_IFarWorld_coordLimits();
    }

    @Override
    public void playerAdd(@NonNull EntityPlayerMP playerIn) {
        this.contexts.compute(playerIn, (player, ctx) -> {
            checkState(ctx == null, "player already tracked: %s", player);

            FP2_LOG.debug("added {} to tracker", player);

            ctx = new Context(player);
            ctx.scheduleUpdateIfNeeded();
            return ctx;
        });
    }

    @Override
    public void playerRemove(@NonNull EntityPlayerMP playerIn) {
        this.contexts.compute(playerIn, (player, ctx) -> {
            checkState(ctx != null, "attempted to remove untracked player: %s", player);

            FP2_LOG.debug("removed {} from tracker", player);

            //release context
            ctx.release();

            //return null to have context be removed from map
            return null;
        });
    }

    @Override
    public void playerMove(@NonNull EntityPlayerMP playerIn) {
        checkState(FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread(), "must be called from server thread!");

        this.contexts.compute(playerIn, (player, ctx) -> {
            checkState(ctx != null, "attempted to update untracked player: %s", player);

            ctx.scheduleUpdateIfNeeded();
            return ctx;
        });
    }

    @Override
    public void tileChanged(@NonNull ITileHandle<POS, T> handle) {
        this.entries.computeIfPresent(handle.pos(), (pos, entry) -> {
            //notify entry that the tile has been changed
            entry.tileChanged(handle, false);

            return entry;
        });
    }

    public void tileChangedCallback(@NonNull ITileHandle<POS, T> handle) {
        this.entries.computeIfPresent(handle.pos(), (pos, entry) -> {
            //notify entry that the tile has been changed
            entry.tileChanged(handle, true);

            return entry;
        });
    }

    @Override
    public void debug_dropAllTiles() {
        ThreadingHelper.scheduleTaskInWorldThread(this.world.world(), () -> {
            Collection<EntityPlayerMP> trackedPlayersSnapshot = new ArrayList<>(this.contexts.keySet());
            trackedPlayersSnapshot.forEach(this::playerRemove);

            //it should be reasonably safe to assume that all pending tasks will have been cancelled by now

            trackedPlayersSnapshot.forEach(this::playerAdd);
        });
    }

    protected abstract STATE currentStateFor(@NonNull EntityPlayerMP player);

    protected abstract void allPositions(@NonNull EntityPlayerMP player, @NonNull STATE state, @NonNull Consumer<POS> callback);

    protected abstract void deltaPositions(@NonNull EntityPlayerMP player, @NonNull STATE oldState, @NonNull STATE newState, @NonNull Consumer<POS> added, @NonNull Consumer<POS> removed);

    protected abstract boolean isVisible(@NonNull EntityPlayerMP player, @NonNull STATE state, @NonNull POS pos);

    protected abstract Comparator<POS> comparatorFor(@NonNull EntityPlayerMP player, @NonNull STATE state);

    protected abstract boolean shouldTriggerUpdate(@NonNull EntityPlayerMP player, @NonNull STATE oldState, @NonNull STATE newState);

    protected void beginTracking(@NonNull Context ctx, @NonNull POS posIn) {
        this.entries.compute(posIn, (pos, entry) -> {
            if (entry == null) { //no entry exists at this position, so we should make a new one
                entry = new Entry(pos);
            }

            return entry.addContext(ctx);
        });
    }

    protected void stopTracking(@NonNull Context ctx, @NonNull POS posIn) {
        this.entries.compute(posIn, (pos, entry) -> {
            checkState(entry != null, "cannot remove player %s from non-existent tracking entry at %s", ctx, pos);

            return entry.removePlayer(ctx);
        });
    }

    /**
     * Manages the positions tracked by a single player.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class Context extends AbstractReleasable {
        @NonNull
        protected final EntityPlayerMP player;

        protected final EventExecutor executor = TRACKER_THREADS.next();

        //only ever accessed to by tracker thread
        protected final Set<POS> loadedPositions = new ObjectOpenHashSet<>();
        protected final Set<POS> waitingPositions = new ObjectOpenHashSet<>(FP2Config.generationThreads << 4);
        protected final RecyclingArrayDeque<POS> queuedPositions = new RecyclingArrayDeque<>();

        //these are using a single object reference instead of flattened fields to allow the value to be replaced atomically. to ensure coherent access to the values,
        // readers must take care never to dereference the fields more than once.
        protected volatile STATE lastState;
        protected volatile STATE nextState;

        /**
         * Considers scheduling this player for a tracking update.
         * <p>
         * May only be called from the server thread.
         */
        public void scheduleUpdateIfNeeded() {
            STATE lastPos = this.lastState;
            STATE nextState = AbstractPlayerTracker.this.currentStateFor(this.player);
            if (lastPos == null || AbstractPlayerTracker.this.shouldTriggerUpdate(this.player, lastPos, nextState)) {
                //set nextPos to be used while updating
                this.nextState = nextState;

                //add update task to execution queue
                this.executor.submit(this::update);
            }
        }

        /**
         * Actually updates this tracking context.
         * <p>
         * Must be called on this context's worker thread.
         */
        private void update() {
            try {
                this.assertOnTrackerThread();

                STATE lastState = this.lastState;
                STATE nextState = this.nextState;
                if (lastState != null && (nextState == null || !AbstractPlayerTracker.this.shouldTriggerUpdate(this.player, lastState, nextState))) {
                    //there is no next state (possibly already updated?), so we shouldn't do anything
                    return;
                }

                //inform the server thread that this update has started, by updating the current state and clearing the next one
                this.lastState = nextState;
                this.nextState = null;

                if (lastState != null) { //if lastState exists, we can diff the positions (which is faster than iterating over all of them)
                    //unqueue all positions which are no longer visible.
                    //  this is O(n), whereas removing them during the deltaPositions 'removed' callback would be O(n*m) (where n=queue size, m=number of positions in render
                    //  distance). the savings from this are significant - when flying around with 1level@400cutoff, tracker updates are reduced from 5000-10000ms to about 40-50ms.
                    this.queuedPositions.removeIf(pos -> !AbstractPlayerTracker.this.isVisible(this.player, nextState, pos));

                    AbstractPlayerTracker.this.deltaPositions(this.player, lastState, nextState,
                            this.queuedPositions::add,
                            pos -> {
                                //stop loading the tile if needed
                                if (this.loadedPositions.contains(pos) || this.waitingPositions.contains(pos)) {
                                    AbstractPlayerTracker.this.stopTracking(this, pos);

                                    //stopTracking() won't call notifyUnload() if the tile hasn't finished loading yet, so we should manually remove it from both sets afterwards just in case
                                    this.loadedPositions.remove(pos);
                                    this.waitingPositions.remove(pos);
                                }
                            });
                } else { //no positions have been added so far, so we need to iterate over them all
                    AbstractPlayerTracker.this.allPositions(this.player, nextState, this.queuedPositions::add);
                }

                //sort queue
                this.queuedPositions.sort(AbstractPlayerTracker.this.comparatorFor(this.player, nextState));
            } catch (Exception e) {
                ThreadingHelper.handle(AbstractPlayerTracker.this.world.world(), e);
            }

            //double-check to make sure the load queue is totally filled
            this.fillLoadQueue();
        }

        private void fillLoadQueue() {
            this.assertOnTrackerThread();

            for (POS pos; this.waitingPositions.size() < (FP2Config.generationThreads << 2) && (pos = this.queuedPositions.poll()) != null; ) {
                checkState(this.waitingPositions.add(pos), "position already queued?!?");
                AbstractPlayerTracker.this.beginTracking(this, pos);
            }
        }

        /**
         * Enqueues the given tile to be sent to the player.
         */
        public void notifyChanged(@NonNull ITileHandle<POS, T> handle) {
            if (this.executor.inEventLoop()) {
                this.notifyChangedSync(handle);
            } else {
                this.executor.submit(() -> this.notifyChangedSync(handle));
            }
        }

        private void notifyChangedSync(@NonNull ITileHandle<POS, T> handle) {
            try {
                this.assertOnTrackerThread();

                STATE lastState = this.lastState;
                if (this.isReleased() //context is released, ignore update
                    || !AbstractPlayerTracker.this.isVisible(this.player, lastState, handle.pos())) { //the position has been unloaded since the task was enqueued, we can assume it's safe to ignore
                    return;
                }

                NETWORK_WRAPPER.sendTo(new SPacketTileData().mode(AbstractPlayerTracker.this.world.mode()).tile(handle.snapshot()), this.player);
                if (this.waitingPositions.remove(handle.pos())) { //we were waiting to load this tile, and now it's loaded!
                    checkState(this.loadedPositions.add(handle.pos()), "position already loaded?!?");

                    //make sure as many tiles as possible are queued for loading
                    this.fillLoadQueue();
                } else {
                    checkState(this.loadedPositions.contains(handle.pos()), "position loaded but not in queue?!?");
                }
            } catch (Exception e) {
                ThreadingHelper.handle(AbstractPlayerTracker.this.world.world(), e);
            }
        }

        /**
         * Enqueues the given tile to be unloaded by the player.
         *
         * @param pos the position of the tile to be unloaded
         */
        public void notifyUnload(@NonNull POS pos) {
            if (this.executor.inEventLoop()) {
                this.notifyUnloadSync(pos);
            } else {
                this.executor.submit(() -> this.notifyUnloadSync(pos));
            }
        }

        private void notifyUnloadSync(@NonNull POS pos) {
            try {
                this.assertOnTrackerThread();

                if (this.isReleased()) { //context is released, ignore update
                    return;
                }

                if (this.loadedPositions.remove(pos)) {
                    checkState(!this.waitingPositions.remove(pos), "tile at %s was loaded and queued at once?!?", pos);

                    NETWORK_WRAPPER.sendTo(new SPacketUnloadTile().mode(AbstractPlayerTracker.this.world.mode()).pos(pos), this.player);
                } else {
                    checkState(this.waitingPositions.remove(pos), "tile at %s wasn't loaded?!?", pos);

                    this.fillLoadQueue();
                }
            } catch (Exception e) {
                ThreadingHelper.handle(AbstractPlayerTracker.this.world.world(), e);
            }
        }

        @Override
        protected void doRelease() {
            if (!this.executor.inEventLoop()) { //make sure we're on the tracker thread
                //set nextPos to null to prevent any update tasks that might be in the queue from running
                this.nextState = null;

                this.executor.submit(this::doRelease).syncUninterruptibly(); //block until the task is actually executed
                return;
            }

            //tell the client to unload all tiles
            NETWORK_WRAPPER.sendTo(new SPacketUnloadTiles().mode(AbstractPlayerTracker.this.world.mode()).positions(new ArrayList<>(this.loadedPositions)), this.player);

            //remove player from all tracking positions
            // (using temporary set to avoid CME)
            Set<POS> tmp = new ObjectOpenHashSet<>(this.waitingPositions.size() + this.loadedPositions.size());
            tmp.addAll(this.waitingPositions);
            tmp.addAll(this.loadedPositions);
            tmp.forEach(pos -> AbstractPlayerTracker.this.stopTracking(this, pos));
            tmp.clear();

            //release everything
            this.queuedPositions.close();
        }

        protected boolean isReleased() {
            return PUnsafe.getIntVolatile(this, ABSTRACTRELEASABLE_RELEASED_OFFSET) != 0;
        }

        @Override
        public String toString() {
            return this.player.toString();
        }

        protected void assertOnTrackerThread() {
            checkState(this.executor.inEventLoop(), "may only be called by tracker thread, not %s", Thread.currentThread());
        }
    }

    /**
     * Associates a tile position to the {@link Context}s which are tracking it.
     *
     * @author DaPorkchop_
     */
    @ToString
    protected class Entry extends CompactReferenceArraySet<Context> {
        //we're using an ArraySet because even though all the operations run in O(n) time, it shouldn't ever be an issue - this should still be plenty fast even if there are
        //  hundreds of players tracking the same tile, and it uses a fair amount less memory than an equivalent HashSet.
        //we extend from CompactReferenceArraySet rather than having it as a field in order to minimize memory wasted by JVM object headers and the likes, as well as reduced
        //  pointer chasing.

        protected final POS pos;

        protected CompletableFuture<ITileHandle<POS, T>> loadFuture;
        protected ITileHandle<POS, T> handle;

        public Entry(@NonNull POS pos) {
            this.pos = pos;

            //request that the tile be generated
            this.loadFuture = AbstractPlayerTracker.this.world.requestLoad(pos);
            this.loadFuture.thenAcceptAsync(AbstractPlayerTracker.this::tileChangedCallback, TRACKER_THREADS);
        }

        public Entry addContext(@NonNull Context ctx) {
            checkState(super.add(ctx), "player %s was already added to entry %s!", ctx, this);

            if (this.handle != null) { //the tile has already been initialized, let's send it to the player
                ctx.notifyChanged(this.handle);
            }

            return this;
        }

        public Entry removePlayer(@NonNull Context ctx) {
            checkState(super.remove(ctx), "player %s did not belong to entry %s!", ctx, this);

            if (this.handle != null) { //the tile has already been sent to the player, so it needs to be unloaded on their end
                ctx.notifyUnload(this.pos);
            }

            if (super.isEmpty()) { //no more players are tracking this entry, so it can be removed
                //release the tile load future (potentially removing it from the load/generation queue)
                if (this.loadFuture != null) {
                    this.loadFuture.cancel(false);
                    this.loadFuture = null;
                }

                //this entry should be replaced with null
                return null;
            } else {
                return this;
            }
        }

        public void tileChanged(@NonNull ITileHandle<POS, T> handle, boolean loadCallback) {
            checkState(handle.isInitialized(), "handle at %s hasn't been initialized yet!", this.pos);

            if (loadCallback) {
                checkState(this.handle == null, "already loaded handle at %s?!?", this.pos);
                this.loadFuture = null;
                this.handle = handle;
            } else {
                if (this.handle == null) { //we don't care about any potential modifications to a tile if it hasn't been loaded yet
                    return;
                } else {
                    checkState(this.handle == handle, "mismatched handles at %s!", this.pos);
                }
            }

            //notify all players which have this tile loaded
            super.forEach(ctx -> ctx.notifyChanged(handle));
        }
    }
}
