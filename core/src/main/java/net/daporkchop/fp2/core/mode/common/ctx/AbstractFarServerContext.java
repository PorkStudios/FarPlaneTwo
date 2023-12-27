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

package net.daporkchop.fp2.core.mode.common.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.engine.tile.TileSnapshot;
import net.daporkchop.fp2.core.mode.common.server.tracking.AbstractTracker;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;
import net.daporkchop.lib.common.annotation.TransferOwnership;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IFarServerContext}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarServerContext implements IFarServerContext {
    protected final IFarPlayerServer player;
    protected final IFarLevelServer world;
    protected final IFarTileProvider tileProvider;

    protected final AbstractTracker tracker;

    protected final Map<TilePos, Optional<TileSnapshot>> sendQueue = new TreeMap<>();

    protected FP2Config config;

    protected boolean closed = false;

    private int debugLastUpdateSent;

    public AbstractFarServerContext(@NonNull IFarPlayerServer player, @NonNull IFarLevelServer world, @NonNull FP2Config config) {
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

    protected void flushSendQueue() {
        //check if the send queue contains any items to see if any additional work is necessary.
        // this is safe to call without synchronization: it only accesses the map size, which involves reading a single int field. the worst that can happen is we miss a
        // queued tile for one tick.
        if (this.sendQueue.isEmpty()) {
            //the send queue is empty, nothing to do!
            return;
        }

        //make a snapshot of the queue contents, then clear it.
        List<Map.Entry<TilePos, Optional<TileSnapshot>>> sendQueueSnapshot;
        synchronized (this.sendQueue) {
            sendQueueSnapshot = new ArrayList<>(this.sendQueue.entrySet());
            this.sendQueue.clear();
        }

        //group queue items by type
        List<TilePos> unloadedPositions = DirectTilePosAccess.newPositionList();
        List<TileSnapshot> loadedSnapshots = new ArrayList<>();
        sendQueueSnapshot.forEach(entry -> {
            if (entry.getValue().isPresent()) { //non-empty optional, the tile is being loaded
                loadedSnapshots.add(entry.getValue().get());
            } else { //empty optional, the tile is being unloaded
                unloadedPositions.add(entry.getKey());
            }
        });
        sendQueueSnapshot = null; //allow GC

        //send packets for unloaded tiles
        switch (unloadedPositions.size()) {
            case 0: //there are no tiles to unload, do nothing
                break;
            case 1: //we're only unloading a single tile
                this.player.fp2_IFarPlayer_sendPacket(new SPacketUnloadTile().pos(unloadedPositions.get(0)));
                break;
            default: //we're unloading more than one tile, batch it into a single packet
                this.player.fp2_IFarPlayer_sendPacket(new SPacketUnloadTiles().positions(unloadedPositions));
                break;
        }

        //send packets for loaded/updated tiles
        loadedSnapshots.forEach(snapshot -> this.player.fp2_IFarPlayer_sendPacket(new SPacketTileData().tile(snapshot)));
    }

    private void debugUpdate() {
        if (!FP2_DEBUG) { //debug mode not enabled, do nothing
            //TODO: remove this once @DebugOnly actually works
            return;
        }

        if (++this.debugLastUpdateSent == 20) { //send a debug statistics update packet once every 20s
            this.debugLastUpdateSent = 0;

            this.player.fp2_IFarPlayer_sendPacket(new SPacketDebugUpdateStatistics().tracking(this.tracker.debugStats()));
        }
    }

    @CalledFromServerThread
    @Override
    public void close() {
        checkState(!this.closed, "already closed!");
        this.closed = true;

        this.tracker.close();
    }

    protected BiFunction<Optional<TileSnapshot>, Optional<TileSnapshot>, Optional<TileSnapshot>> sendQueueMergeOperator() {
        return (prev, next) -> {
            prev.ifPresent(TileSnapshot::release);
            return next;
        };
    }

    @Override
    public void sendTile(@TransferOwnership @NonNull TileSnapshot snapshot) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this.sendQueue) {
            this.sendQueue.merge(snapshot.pos(), Optional.of(snapshot), this.sendQueueMergeOperator());
        }
    }

    @Override
    public void sendTileUnload(@NonNull TilePos pos) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this.sendQueue) {
            this.sendQueue.merge(pos, Optional.empty(), this.sendQueueMergeOperator());
        }
    }

    @Override
    public void sendMultiTileUnload(@NonNull Iterable<TilePos> positions) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this.sendQueue) {
            positions.forEach(pos -> this.sendQueue.merge(pos, Optional.empty(), this.sendQueueMergeOperator()));
        }
    }
}
