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
import lombok.Synchronized;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTracker;
import net.daporkchop.fp2.core.mode.api.tile.TileSnapshot;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;

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

    @Synchronized("sendQueue")
    protected void flushSendQueue() {
        if (!this.sendQueue.isEmpty()) {
            this.sendQueue.forEach((pos, optionalSnapshot) -> this.player.fp2_IFarPlayer_sendPacket(optionalSnapshot.isPresent()
                    ? new SPacketTileData().mode(this.mode).tile(optionalSnapshot.get())
                    : new SPacketUnloadTile().mode(this.mode).pos(pos)));
            this.sendQueue.clear();
        }
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
    @Synchronized("sendQueue")
    public void sendTile(@NonNull TileSnapshot<POS, T> snapshot) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        this.sendQueue.put(snapshot.pos(), Optional.of(snapshot));
    }

    @Override
    @Synchronized("sendQueue")
    public void sendTileUnload(@NonNull POS pos) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        this.sendQueue.put(pos, Optional.empty());
    }

    @Override
    @Synchronized("sendQueue")
    public void sendMultiTileUnload(@NonNull Iterable<POS> positions) {
        if (this.closed) { //this context has been closed - silently discard all tile data
            return;
        }

        positions.forEach(pos -> this.sendQueue.put(pos, Optional.empty()));
    }
}
