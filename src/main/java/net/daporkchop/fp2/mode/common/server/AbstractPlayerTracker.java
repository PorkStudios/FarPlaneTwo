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
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.net.server.SPacketTileData;
import net.daporkchop.fp2.net.server.SPacketUnloadTile;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.datastructure.RecyclingArrayDeque;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractPlayerTracker<POS extends IFarPos, T extends IFarTile> implements IFarPlayerTracker<POS, T> {
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

    protected static final Ref<SimpleRecycler<Set<?>>> SET_RECYCLER = ThreadRef.soft(() -> new SimpleRecycler<Set<?>>() {
        @Override
        protected Set<?> allocate0() {
            return new ObjectOpenHashSet<>();
        }

        @Override
        protected void reset0(@NonNull Set<?> value) {
            value.clear();
        }
    });

    protected static final Ref<SimpleRecycler<ObjIntMap<?>>> MAP_RECYCLER = ThreadRef.soft(() -> new SimpleRecycler<ObjIntMap<?>>() {
        @Override
        protected ObjIntMap<?> allocate0() {
            return new ObjIntOpenHashMap<>();
        }

        @Override
        protected void reset0(@NonNull ObjIntMap<?> value) {
            value.clear();
        }
    });

    protected static <T> Set<T> allocateSet() {
        return uncheckedCast(SET_RECYCLER.get().allocate());
    }

    protected static <T> void releaseSet(@NonNull Set<T> set) {
        SET_RECYCLER.get().release(uncheckedCast(set));
    }

    protected static <T> ObjIntMap<T> allocateMap() {
        return uncheckedCast(MAP_RECYCLER.get().allocate());
    }

    protected static <T> void releaseMap(@NonNull ObjIntMap<T> set) {
        MAP_RECYCLER.get().release(uncheckedCast(set));
    }

    @NonNull
    protected final IFarWorld<POS, T> world;

    protected final Map<POS, Entry> entries = new ConcurrentHashMap<>();
    protected final Map<EntityPlayerMP, Context> contexts = new ConcurrentHashMap<>();

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
        ServerThreadExecutor.INSTANCE.checkServerThread();

        this.contexts.compute(playerIn, (player, ctx) -> {
            checkState(ctx != null, "attempted to update untracked player: %s", player);

            ctx.scheduleUpdateIfNeeded();
            return ctx;
        });
    }

    @Override
    public void tileChanged(@NonNull Compressed<POS, T> tile) {
        if (tile.isGenerated()) {
            this.entries.computeIfPresent(tile.pos(), (pos, entry) -> {
                //notify entry that the tile has been changed
                entry.tileChanged(tile);

                return entry;
            });
        }
    }

    @Override
    public void debug_dropAllTiles() {
        if (!ServerThreadExecutor.INSTANCE.isServerThread()) {
            ServerThreadExecutor.INSTANCE.execute(this::debug_dropAllTiles);
            return;
        }

        Collection<EntityPlayerMP> trackedPlayersSnapshot = new ArrayList<>(this.contexts.keySet());
        trackedPlayersSnapshot.forEach(this::playerRemove);

        //it should be reasonably safe to assume that all pending tasks will have been cancelled by now

        trackedPlayersSnapshot.forEach(this::playerAdd);
    }

    protected abstract void allPositions(@NonNull EntityPlayerMP player, double posX, double posY, double posZ, @NonNull Consumer<POS> callback);

    protected abstract void deltaPositions(@NonNull EntityPlayerMP player, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, @NonNull Consumer<POS> added, @NonNull Consumer<POS> removed);

    protected abstract boolean isVisible(@NonNull EntityPlayerMP player, double posX, double posY, double posZ, @NonNull POS pos);

    protected abstract boolean shouldTriggerUpdate(@NonNull EntityPlayerMP player, double oldX, double oldY, double oldZ, double newX, double newY, double newZ);

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
     * A tracking context used for a single player.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class Context extends AbstractReleasable {
        @NonNull
        protected final EntityPlayerMP player;

        protected final EventExecutor executor = TRACKER_THREADS.next();

        //only ever accessed to by tracker thread
        protected final Set<POS> loadedPositions = allocateSet();
        protected final Set<POS> waitingPositions = allocateSet();
        protected RecyclingArrayDeque<POS> queuedPositions;

        //these are using Vec3d instead of 3 doubles to allow the value to be replaced atomically. to ensure coherent access to the coordinates,
        // readers must take care never to dereference the fields more than once.
        protected volatile Vec3d lastPos;
        protected volatile Vec3d nextPos;

        {
            this.executor.submit(() -> this.queuedPositions = new RecyclingArrayDeque<>()); //create on tracker thread to ensure it uses the correct recycler
        }

        /**
         * Considers scheduling this player for a tracking update.
         * <p>
         * May only be called from the server thread.
         */
        public void scheduleUpdateIfNeeded() {
            Vec3d lastPos = this.lastPos;
            if (lastPos == null || AbstractPlayerTracker.this.shouldTriggerUpdate(this.player, lastPos.x, lastPos.y, lastPos.z, this.player.posX, this.player.posY, this.player.posZ)) {
                //set nextPos to be used while updating
                this.nextPos = new Vec3d(this.player.posX, this.player.posY, this.player.posZ);

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

                Vec3d lastPos = this.lastPos;
                Vec3d nextPos = this.nextPos;
                if (lastPos != null && (nextPos == null || !AbstractPlayerTracker.this.shouldTriggerUpdate(this.player, lastPos.x, lastPos.y, lastPos.z, nextPos.x, nextPos.y, nextPos.z))) {
                    //there is no next position (possibly already updated?), so we shouldn't do anything
                    return;
                }

                //inform the server thread that this update has started, by updating the current position and clearing the next one
                this.lastPos = nextPos;
                this.nextPos = null;

                //remove all positions from load queue
                this.queuedPositions.clear();

                if (lastPos != null) { //if lastPos exists, we can diff the positions (which is faster than iterating over all of them)
                    AbstractPlayerTracker.this.deltaPositions(this.player, lastPos.x, lastPos.y, lastPos.z, nextPos.x, nextPos.y, nextPos.z,
                            pos -> checkState(AbstractPlayerTracker.this.isVisible(this.player, nextPos.x, nextPos.y, nextPos.z, pos), "couldn't add position %s", pos),
                            pos -> {
                                checkState(!AbstractPlayerTracker.this.isVisible(this.player, nextPos.x, nextPos.y, nextPos.z, pos), "couldn't remove position %s", pos);

                                //stop loading the tile if needed
                                if (this.loadedPositions.contains(pos) || this.waitingPositions.contains(pos)) {
                                    AbstractPlayerTracker.this.stopTracking(this, pos);

                                    //stopTracking() won't call notifyUnload() if the tile hasn't finished loading yet, so we should manually remove it from both sets afterwards just in case
                                    this.loadedPositions.remove(pos);
                                    this.waitingPositions.remove(pos);
                                }
                            });
                } else { //no positions have been added so far, so we need to iterate over them all
                    AbstractPlayerTracker.this.allPositions(this.player, nextPos.x, nextPos.y, nextPos.z,
                            pos -> checkState(AbstractPlayerTracker.this.isVisible(this.player, nextPos.x, nextPos.y, nextPos.z, pos), "couldn't add position %s", pos));
                }

                //re-add all positions to queue
                AbstractPlayerTracker.this.allPositions(this.player, nextPos.x, nextPos.y, nextPos.z, pos -> {
                    checkState(AbstractPlayerTracker.this.isVisible(this.player, nextPos.x, nextPos.y, nextPos.z, pos), "invalid position %s", pos);

                    if (!this.loadedPositions.contains(pos) && !this.waitingPositions.contains(pos)) {
                        //the position is neither loaded nor waiting to be loaded, so let's add it to the queue
                        this.queuedPositions.addLast(pos);
                    }
                });

                //double-check to make sure the load queue is totally filled
                this.fillLoadQueue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void fillLoadQueue() {
            this.assertOnTrackerThread();

            for (POS pos; this.waitingPositions.size() < FP2Config.generationThreads && (pos = this.queuedPositions.poll()) != null; ) {
                checkState(this.waitingPositions.add(pos), "position already queued?!?");
                AbstractPlayerTracker.this.beginTracking(this, pos);
            }
        }

        /**
         * Enqueues the given tile to be sent to the player.
         *
         * @param tile the tile to be sent
         */
        public void notifyChanged(@NonNull Compressed<POS, ?> tile) {
            if (this.executor.inEventLoop()) {
                this.notifyChangedSync(tile);
            } else {
                this.executor.submit(() -> this.notifyChangedSync(tile));
            }
        }

        private void notifyChangedSync(@NonNull Compressed<POS, ?> tile) {
            this.assertOnTrackerThread();

            Vec3d lastPos = this.lastPos;
            if (!AbstractPlayerTracker.this.isVisible(this.player, lastPos.x, lastPos.y, lastPos.z, tile.pos())) { //the position has been unloaded since the task was enqueued, we can assume it's safe to ignore
                return;
            }

            NETWORK_WRAPPER.sendTo(new SPacketTileData().mode(AbstractPlayerTracker.this.world.mode()).tile(tile), this.player);
            if (this.waitingPositions.remove(tile.pos())) { //we were waiting to load this tile, and now it's loaded!
                checkState(this.loadedPositions.add(tile.pos()), "position already loaded?!?");

                //make sure as many tiles as possible are queued for loading
                this.fillLoadQueue();
            } else {
                checkState(this.loadedPositions.contains(tile.pos()), "position loaded but not in queue?!?");
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
            this.assertOnTrackerThread();

            if (this.loadedPositions.remove(pos)) {
                checkState(!this.waitingPositions.remove(pos), "tile at %s was loaded and queued at once?!?", pos);

                NETWORK_WRAPPER.sendTo(new SPacketUnloadTile().mode(AbstractPlayerTracker.this.world.mode()).pos(pos), this.player);
            } else {
                checkState(this.waitingPositions.remove(pos), "tile at %s wasn't loaded?!?", pos);

                this.fillLoadQueue();
            }
        }

        @Override
        protected void doRelease() {
            if (!this.executor.inEventLoop()) { //make sure we're on the tracker thread
                //set nextPos to null to prevent any update tasks that might be in the queue from running
                this.nextPos = null;

                this.executor.submit(this::doRelease).syncUninterruptibly(); //block until the task is actually executed
                return;
            }

            //remove player from all tracking positions
            // (using temporary set to avoid CME)
            Set<POS> tmp = allocateSet();
            tmp.addAll(this.waitingPositions);
            tmp.addAll(this.loadedPositions);
            tmp.forEach(pos -> AbstractPlayerTracker.this.stopTracking(this, pos));
            releaseSet(tmp);

            //release everything
            this.queuedPositions.close();
            releaseSet(this.waitingPositions);
            releaseSet(this.loadedPositions);
        }

        @Override
        public String toString() {
            return this.player.toString();
        }

        private void assertOnTrackerThread() {
            checkState(this.executor.inEventLoop(), "may only be called by tracker thread, not %s", Thread.currentThread());
        }
    }

    @ToString
    public class Entry {
        protected final POS pos;
        protected final Set<Context> contexts = new ReferenceOpenHashSet<>();

        protected Compressed<POS, T> tile;

        public Entry(@NonNull POS pos) {
            this.pos = pos;

            AbstractPlayerTracker.this.world.retainTileFuture(pos).thenAcceptAsync(AbstractPlayerTracker.this::tileChanged, TRACKER_THREADS);
        }

        public Entry addContext(@NonNull Context ctx) {
            checkState(this.contexts.add(ctx), "player %s was already added to entry %s!", ctx, this);

            if (this.tile != null) { //the tile has already been loaded, let's send it to the player
                ctx.notifyChanged(this.tile);
            }

            return this;
        }

        public Entry removePlayer(@NonNull Context ctx) {
            checkState(this.contexts.remove(ctx), "player %s did not belong to entry %s!", ctx, this);

            if (this.tile != null) { //the tile has already been sent to the player, so it needs to be unloaded on their end
                ctx.notifyUnload(this.pos);
            }

            if (this.contexts.isEmpty()) { //no more players are tracking this entry, so it can be removed
                //release the tile load future (potentially removing it from the load/generation queue)
                AbstractPlayerTracker.this.world.releaseTileFuture(this.pos);

                //this entry should be replaced with null
                return null;
            } else {
                return this;
            }
        }

        public void tileChanged(@NonNull Compressed<POS, T> tile) {
            this.tile = tile;

            //send packet to all players
            //TODO: make this not be async after fixing exact generator
            this.contexts.forEach(ctx -> ctx.notifyChanged(tile));
            //TODO: figure out what the above TODO was referring to
            //TODO: i have now figured out what it was referring to, actually fix it now
            //TODO: actually, i don't think there's any way i can reasonably fix this without breaking AsyncBlockAccess. i'll need to rework
            // how tiles are stored on the server (the issue is a deadlock when the server thread is trying to serialize a tile while a worker
            // generating said tile is waiting for the server thread to load a chunk required for generating the tile into AsyncBlockAccess)
        }
    }
}
