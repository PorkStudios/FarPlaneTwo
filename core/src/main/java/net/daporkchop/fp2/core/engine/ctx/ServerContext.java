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

package net.daporkchop.fp2.core.engine.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.engine.server.tracking.Tracker;
import net.daporkchop.fp2.core.engine.tile.TileSnapshot;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketTileAck;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;
import net.daporkchop.lib.common.annotation.TransferOwnership;
import net.daporkchop.lib.common.math.PMath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class ServerContext implements IFarServerContext {
    protected final IFarPlayerServer player;
    protected final IFarLevelServer world;
    protected final IFarTileProvider tileProvider;

    protected final Tracker tracker;

    protected Set<TilePos> unloadTilesQueue = DirectTilePosAccess.newPositionHashSet();
    protected final Map<TilePos, TileSnapshot> sendTilesQueue = DirectTilePosAccess.newPositionKeyedTreeMap();
    protected final FlowControl flowControl = new FlowControl();

    protected FP2Config config;

    protected boolean closed = false;

    private int debugLastUpdateSent;

    public ServerContext(@NonNull IFarPlayerServer player, @NonNull IFarLevelServer world, @NonNull FP2Config config) {
        this.player = player;
        this.world = world;
        this.config = config;

        this.tileProvider = world.tileProvider();
        this.tracker = this.tileProvider.trackerManager().beginTracking(this);
    }

    @CalledFromServerThread
    @Override
    public void notifyConfigChange(@NonNull FP2Config config) {
        checkState(!this.closed, "already closed!");
        this.config = config;

        //no reason to bother scheduling an update immediately, it'll happen on the next server tick anyway
    }

    @CalledFromServerThread
    @Override
    public void update() {
        checkState(!this.closed, "already closed!");

        this.tracker.update();

        this.flushSendQueue();
        this.debugUpdate();
    }

    @Override
    public void notifyAck(@NonNull CPacketTileAck ack) {
        if (ack.size != 0L) {
            this.flowControl.ack(ack);
        }
    }

    protected void flushSendQueue() {
        //check if the send queue contains any items to see if any additional work is necessary.
        // this is safe to call without synchronization: it only accesses the map size, which involves reading a single int field. the worst that can happen is we miss a
        // queued tile for one tick.
        if (this.unloadTilesQueue.isEmpty() && this.sendTilesQueue.isEmpty()) {
            //the send queue is empty, nothing to do!
            return;
        }

        Set<TilePos> unloadedTiles = null;
        List<TileSnapshot> loadedTiles = null;
        long loadedTilesSize = 0L;

        synchronized (this) {
            if (!this.unloadTilesQueue.isEmpty()) {
                unloadedTiles = this.unloadTilesQueue;
                this.unloadTilesQueue = DirectTilePosAccess.newPositionHashSet(); //replace with a new set
            }

            if (!this.sendTilesQueue.isEmpty()) {
                Iterator<TileSnapshot> itr = this.sendTilesQueue.values().iterator();
                loadedTiles = new ArrayList<>();
                long maxSendSize = this.flowControl.maxSendSizeThisTick();
                do { //using a do-while loop to ensure that we always send at least one tile per tick
                    TileSnapshot next = itr.next();
                    itr.remove();

                    loadedTiles.add(next);
                    loadedTilesSize += next.dataSize();
                } while (loadedTilesSize < maxSendSize && itr.hasNext());
            }
        }

        //send packet for unloaded tiles
        if (unloadedTiles != null) {
            this.player.fp2_IFarPlayer_sendPacket(SPacketUnloadTiles.create(unloadedTiles));
        }

        //send packets for loaded/updated tiles
        if (loadedTiles != null) {
            long timestamp = 0L;
            if (loadedTilesSize > 0L) {
                //if we're sending a tile data packet which isn't empty, we want it to be tracked for flow control! also, we'll send an empty tile data packet beforehand so
                // that the timeSinceLastTileDataPacket field in the Ack packet will indicate the time it took to send just a single batch of tiles.
                timestamp = this.flowControl.submit(loadedTilesSize);
                this.player.fp2_IFarPlayer_sendPacket(SPacketTileData.create(timestamp, Collections.emptyList()));
            }
            this.player.fp2_IFarPlayer_sendPacket(SPacketTileData.create(timestamp, loadedTiles));
        }

        /*if (unloadedTiles != null || loadedTiles != null) {
            System.out.printf("sent %d loads and %d unloads this tick (%d remaining, window size=%d)\n",
                    loadedTiles != null ? loadedTiles.size() : 0,
                    unloadedTiles != null ? unloadedTiles.size() : 0,
                    this.sendTilesQueue.size(), this.flowControl.windowSize);
        }*/
    }

    private void debugUpdate() {
        if (!FP2_DEBUG) { //debug mode not enabled, do nothing
            //TODO: remove this once @DebugOnly actually works
            return;
        }

        if (++this.debugLastUpdateSent == 20) { //send a debug statistics update packet once every 20s
            this.debugLastUpdateSent = 0;

            this.player.fp2_IFarPlayer_sendPacket(SPacketDebugUpdateStatistics.create(this.tracker.debugStats()));
        }
    }

    @CalledFromServerThread
    @Override
    public void close() {
        checkState(!this.closed, "already closed!");
        this.closed = true;

        this.tracker.close();
    }

    @Override
    public void sendTile(@TransferOwnership @NonNull TileSnapshot snapshot) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this) {
            this.unloadTilesQueue.remove(snapshot.pos());

            TileSnapshot old = this.sendTilesQueue.put(snapshot.pos(), snapshot);
            if (old != null) { //we're replacing another snapshot which was already queued, release it
                old.release();
            }
        }
    }

    @Override
    public void sendTileUnload(@NonNull TilePos pos) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this) {
            if (this.unloadTilesQueue.add(pos)) {
                TileSnapshot old = this.sendTilesQueue.remove(pos);
                if (old != null) { //the tile was previously queued to be sent, release it
                    old.release();
                }
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    static final class FlowControl {
        private static final long WINDOW_STEP_SIZE = 4096L;

        private long latestTime = System.nanoTime();

        private final Deque<UUID> inFlightTileBatches = new ArrayDeque<>(); //UUID is simply a convenient class with two longs
        private long inFlightDataSize;

        private long windowSize = WINDOW_STEP_SIZE;

        public long maxSendSizeThisTick() {
            return Math.max(this.windowSize - this.inFlightDataSize, 0L);
        }

        public long submit(long size) {
            positive(size, "size");
            long timestamp = System.nanoTime();

            //impossible unless a game tick takes less than one nanosecond (???) or nanoTime() overflows (in which case missing terrain packets in a block game is the least of our worries)
            checkState(timestamp > this.latestTime);
            this.latestTime = timestamp;

            this.inFlightDataSize += size;
            this.inFlightTileBatches.add(new UUID(timestamp, size));
            return timestamp;
        }

        public void ack(CPacketTileAck ack) {
            long timestamp = ack.timestamp;
            long size = ack.size;

            UUID firstBatch = this.inFlightTileBatches.remove();
            long firstTimestamp = firstBatch.getMostSignificantBits();
            long firstSize = firstBatch.getLeastSignificantBits();

            checkArg(timestamp == firstTimestamp, "received ACK with timestamp %s (expected %s)", timestamp, firstTimestamp);
            checkArg(size == firstSize, "received ACK with size %s (expected %s)", size, firstSize);

            this.inFlightDataSize -= size;

            if (ack.timeSinceLastTileDataPacket >= TimeUnit.MILLISECONDS.toNanos(50L)) {
                //multiply by 0.75
                this.windowSize = Math.max((this.windowSize >> 1L) + (this.windowSize >> 2L), WINDOW_STEP_SIZE);
            } else {
                //increase by step size
                this.windowSize = Math.min(this.windowSize + WINDOW_STEP_SIZE, PMath.roundUp(Integer.MAX_VALUE, WINDOW_STEP_SIZE));
            }
        }
    }
}
