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

package net.daporkchop.fp2.core.mode.common.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTracker;
import net.daporkchop.fp2.core.mode.api.tile.TileSnapshot;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IFarServerContext}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarServerContext<POS extends IFarPos, T extends IFarTile> implements IFarServerContext<POS, T> {
    protected final IFarPlayerServer player;
    protected final IFarWorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final IFarTileProvider<POS, T> tileProvider;

    protected final IFarTracker<POS, T> tracker;

    protected final Map<POS, Optional<TileSnapshot<POS, T>>> sendQueue = new TreeMap<>();

    protected FP2Config config;

    protected boolean closed = false;

    private int debugLastUpdateSent;

    public AbstractFarServerContext(@NonNull IFarPlayerServer player, @NonNull IFarWorldServer world, @NonNull FP2Config config, @NonNull IFarRenderMode<POS, T> mode) {
        this.player = player;
        this.world = world;
        this.mode = mode;
        this.config = config;

        this.tileProvider = world.fp2_IFarWorldServer_tileProviderFor(mode);
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
        List<Map.Entry<POS, Optional<TileSnapshot<POS, T>>>> sendQueueSnapshot;
        synchronized (this.sendQueue) {
            sendQueueSnapshot = new ArrayList<>(this.sendQueue.entrySet());
            this.sendQueue.clear();
        }

        //group queue items by type
        List<POS> unloadedPositions = this.mode.directPosAccess().newPositionList();
        List<TileSnapshot<POS, T>> loadedSnapshots = new ArrayList<>();
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
                this.player.fp2_IFarPlayer_sendPacket(new SPacketUnloadTile().mode(this.mode).pos(unloadedPositions.get(0)));
                break;
            default: //we're unloading more than one tile, batch it into a single packet
                this.player.fp2_IFarPlayer_sendPacket(new SPacketUnloadTiles().mode(this.mode).positions(unloadedPositions));
                break;
        }

        //send packets for loaded/updated tiles
        loadedSnapshots.forEach(snapshot -> this.player.fp2_IFarPlayer_sendPacket(new SPacketTileData().mode(this.mode).tile(snapshot)));
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

    @Override
    public void sendTile(@NonNull TileSnapshot<POS, T> snapshot) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this.sendQueue) {
            this.sendQueue.put(snapshot.pos(), Optional.of(snapshot));
        }
    }

    @Override
    public void sendTileUnload(@NonNull POS pos) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this.sendQueue) {
            this.sendQueue.put(pos, Optional.empty());
        }
    }

    @Override
    public void sendMultiTileUnload(@NonNull Iterable<POS> positions) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        synchronized (this.sendQueue) {
            positions.forEach(pos -> this.sendQueue.put(pos, Optional.empty()));
        }
    }
}
