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
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.net.server.SPacketTileData;
import net.daporkchop.fp2.net.server.SPacketUnloadTile;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractPlayerTracker<POS extends IFarPos> implements IFarPlayerTracker<POS> {
    /**
     * The squared distance a player must move from their previous position in order to trigger a tracking update.
     * <p>
     * The default value of {@code (T_VOXELS / 2)²} is based on the equivalent value of {@code 64} (which is {@code (CHUNK_SIZE / 2)²}) used by vanilla.
     */
    protected static final double UPDATE_TRIGGER_DISTANCE_SQUARED = sq(T_VOXELS >> 1);

    //TODO: consider using a KeyedExecutor for this
    protected static final EventExecutorGroup TRACKER_THREADS = new DefaultEventLoopGroup(
            FP2Config.performance.trackingThreads,
            PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Player Tracker #%%d").build());

    protected static final SimpleRecycler<Set<?>> SET_RECYCLER = new SimpleRecycler<Set<?>>() {
        @Override
        protected Set<?> allocate0() {
            return new ObjectOpenHashSet<>();
        }

        @Override
        protected void reset0(@NonNull Set<?> value) {
            value.clear();
        }
    };

    protected static <POS extends IFarPos> Set<POS> allocateSet() {
        return uncheckedCast(SET_RECYCLER.allocate());
    }

    protected static <POS extends IFarPos> void releaseSet(@NonNull Set<POS> set) {
        SET_RECYCLER.release(uncheckedCast(set));
    }

    @NonNull
    protected final IFarWorld<POS, ?> world;

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
    public void tileChanged(@NonNull Compressed<POS, ?> tile) {
        if (tile.isGenerated()) {
            if (!ServerThreadExecutor.INSTANCE.isServerThread()) {
                ServerThreadExecutor.INSTANCE.execute(() -> this.tileChanged(tile));
                return;
            }

            Entry entry = this.entries.get(tile.pos());
            if (entry != null) {
                entry.tileChanged(tile);
            }
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

    protected abstract Stream<POS> getPositions(@NonNull EntityPlayerMP player, double posX, double posY, double posZ);

    protected abstract boolean shouldTriggerUpdate(double x0, double y0, double z0, double x1, double y1, double z1);

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

        //only ever written to by tracker thread
        protected volatile Set<POS> trackingPositions = allocateSet();

        //these are using Vec3d instead of 3 doubles to allow the value to be replaced atomically. to ensure coherent access to the coordinates,
        // readers must take care never to dereference the fields more than once.
        protected volatile Vec3d lastPos = new Vec3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        protected volatile Vec3d nextPos;

        /**
         * Considers scheduling this player for a tracking update.
         * <p>
         * May only be called from the server thread.
         */
        public void scheduleUpdateIfNeeded() {
            Vec3d lastPos = this.lastPos;
            if (AbstractPlayerTracker.this.shouldTriggerUpdate(lastPos.x, lastPos.y, lastPos.z, this.player.posX, this.player.posY, this.player.posZ)) {
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
                Vec3d lastPos = this.lastPos;
                Vec3d nextPos = this.nextPos;
                if (nextPos == null || !AbstractPlayerTracker.this.shouldTriggerUpdate(lastPos.x, lastPos.y, lastPos.z, nextPos.x, nextPos.y, nextPos.z)) {
                    //there is no next position (possibly already updated?), so we shouldn't do anything
                    return;
                }

                //inform the server thread that this update has started, by updating the current position and clearing the next one
                this.lastPos = nextPos;
                this.nextPos = null;

                Set<POS> oldPositions = this.trackingPositions;
                Set<POS> newPositions = allocateSet();

                AbstractPlayerTracker.this.getPositions(this.player, nextPos.x, nextPos.y, nextPos.z)
                        .forEach(pos -> {
                            newPositions.add(pos);

                            if (!oldPositions.remove(pos)) {
                                //the position wasn't being tracked before, so we should begin tracking it
                                AbstractPlayerTracker.this.beginTracking(this, pos);
                            }
                        });

                //by this point, oldPositions contains all the positions that are no longer needed. we can simply iterate through it and stop tracking them
                oldPositions.forEach(pos -> AbstractPlayerTracker.this.stopTracking(this, pos));

                this.trackingPositions = newPositions;
                releaseSet(oldPositions);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        /**
         * Enqueues the given tile to be sent to the player.
         *
         * @param tile the tile to be sent
         */
        public void sendTile(@NonNull Compressed<POS, ?> tile) {
            NETWORK_WRAPPER.sendTo(new SPacketTileData().mode(AbstractPlayerTracker.this.world.mode()).tile(tile), this.player);
        }

        /**
         * Enqueues the given tile to be unloaded by the player.
         *
         * @param pos the position of the tile to be unloaded
         */
        public void unloadTile(@NonNull POS pos) {
            NETWORK_WRAPPER.sendTo(new SPacketUnloadTile().mode(AbstractPlayerTracker.this.world.mode()).pos(pos), this.player);
        }

        @Override
        protected void doRelease() {
            if (!this.executor.inEventLoop()) { //make sure we're on the tracker thread
                this.executor.submit(this::doRelease);
                return;
            }

            //remove player from all tracking positions
            this.trackingPositions.forEach(pos -> AbstractPlayerTracker.this.stopTracking(this, pos));
            releaseSet(this.trackingPositions);
        }

        @Override
        public String toString() {
            return this.player.toString();
        }
    }

    @ToString
    public class Entry {
        protected final POS pos;
        protected final Set<Context> contexts = new ReferenceOpenHashSet<>();

        protected Compressed<POS, ?> tile;

        public Entry(@NonNull POS pos) {
            this.pos = pos;

            AbstractPlayerTracker.this.world.retainTileFuture(pos).thenAccept(this::tileChanged);
        }

        public Entry addContext(@NonNull Context ctx) {
            checkState(this.contexts.add(ctx), "player %s was already added to entry %s!", ctx, this);

            if (this.tile != null) { //the tile has already been loaded, let's send it to the player
                ctx.sendTile(this.tile);
            }

            return this;
        }

        public Entry removePlayer(@NonNull Context ctx) {
            checkState(this.contexts.remove(ctx), "player %s did not belong to entry %s!", ctx, this);

            if (this.tile != null) { //the tile has already been sent to the player, so it needs to be unloaded on their end
                ctx.unloadTile(this.pos);
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

        public void tileChanged(@NonNull Compressed<POS, ?> tile) {
            this.tile = tile;

            //send packet to all players
            //TODO: make this not be async after fixing exact generator
            this.contexts.forEach(ctx -> ctx.sendTile(tile));
            //TODO: figure out what the above TODO was referring to
            //TODO: i have now figured out what it was referring to, actually fix it now
            //TODO: actually, i don't think there's any way i can reasonably fix this without breaking AsyncBlockAccess. i'll need to rework
            // how tiles are stored on the server (the issue is a deadlock when the server thread is trying to serialize a tile while a worker
            // generating said tile is waiting for the server thread to load a chunk required for generating the tile into AsyncBlockAccess)
            //this.players.forEach(player -> NETWORK_WRAPPER.sendTo(packet, player));
        }
    }
}
