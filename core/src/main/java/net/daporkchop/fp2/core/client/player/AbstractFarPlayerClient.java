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

package net.daporkchop.fp2.core.client.player;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.world.AbstractWorldClient;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.ctx.ClientContext;
import net.daporkchop.fp2.core.engine.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.core.util.annotation.CalledWithMonitor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Objects;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarPlayerClient<F extends FP2Core> implements IFarPlayerClient {
    protected FP2Config serverConfig;
    protected FP2Config config;

    protected IFarClientContext context;
    protected long lastTileDataPacketTime;

    protected boolean handshakeReceived;
    protected boolean clientReady;
    protected boolean initialConfigSent;
    protected boolean sessionOpen;
    protected boolean closed;

    protected DebugStats.Tracking debugServerStats;

    @CalledFromAnyThread
    @Override
    public synchronized void handle(@NonNull Object packet) {
        if (this.closed) { //the player has been closed, we should drop any incoming packets as they are likely just backlogged and hadn't been processed yet when the player was closed
            this.fp2().log().warn(className(this) + ": received unexpected packet " + className(packet) + " after player had been closed");
            return;
        }

        if (packet instanceof SPacketHandshake) {
            this.handle((SPacketHandshake) packet);
        } else if (packet instanceof SPacketSessionBegin) {
            this.handle((SPacketSessionBegin) packet);
        } else if (packet instanceof SPacketSessionEnd) {
            this.handle((SPacketSessionEnd) packet);
        } else if (packet instanceof SPacketTileData) {
            this.handle((SPacketTileData) packet);
        } else if (packet instanceof SPacketUnloadTiles) {
            this.handle((SPacketUnloadTiles) packet);
        } else if (packet instanceof SPacketUpdateConfig.Merged) {
            this.handle((SPacketUpdateConfig.Merged) packet);
        } else if (packet instanceof SPacketUpdateConfig.Server) {
            this.handle((SPacketUpdateConfig.Server) packet);
        } else if (packet instanceof SPacketDebugUpdateStatistics) {
            this.handleDebug((SPacketDebugUpdateStatistics) packet);
        } else {
            throw new IllegalArgumentException("don't know how to handle " + className(packet));
        }
    }

    @CalledWithMonitor
    protected void handle(@NonNull SPacketHandshake packet) {
        checkState(!this.handshakeReceived, "handshake packet has already been received!");
        this.handshakeReceived = true;

        this.fp2().log().info("received server handshake");

        this.trySendInitialConfig();
    }

    @CalledWithMonitor
    protected void handle(@NonNull SPacketSessionBegin packet) {
        checkState(!this.sessionOpen, "a session is already open!");
        this.sessionOpen = true;

        this.fp2().log().info("beginning session");

        if (this.config != null) {
            AbstractWorldClient.COORD_LIMITS_HACK.set(packet.coordLimits);
            try {
                IFarLevelClient activeLevel = this.loadActiveLevel();
                try {
                    this.context = new ClientContext(activeLevel, this.config, packet.sessionId);
                } catch (Throwable t) { //something went wrong, try to unload active level again
                    try {
                        ((AbstractWorldClient<?, ?, ?, ?, ?>) this.world()).unloadLevel(activeLevel.id());
                    } catch (Throwable t1) {
                        t.addSuppressed(t1);
                    }

                    //rethrow original exception
                    PUnsafe.throwException(t);
                }
            } finally {
                AbstractWorldClient.COORD_LIMITS_HACK.remove();
            }
        }
    }

    protected abstract IFarLevelClient loadActiveLevel(); //TODO: i want to get rid of this and figure out how to *not* have to include the coordinate bounds in the session begin packet

    @CalledWithMonitor
    protected void handle(SPacketSessionEnd packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        this.sessionOpen = false;

        this.fp2().log().info("ending session");
        this.closeSessionIfOpen();
    }

    @CalledWithMonitor
    protected void handle(@NonNull SPacketTileData packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        long now = System.nanoTime();
        long lastTileDataPacketTime = this.lastTileDataPacketTime;
        this.lastTileDataPacketTime = now;
        this.send(packet.ackPacket(this.context.sessionId(), now - lastTileDataPacketTime));

        packet.tiles.forEach(this.context.tileCache()::receiveTile);
    }

    @CalledWithMonitor
    protected void handle(@NonNull SPacketUnloadTiles packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        packet.positions.forEach(this.context.tileCache()::unloadTile);
    }

    @CalledWithMonitor
    protected void handle(@NonNull SPacketUpdateConfig.Merged packet) {
        if (Objects.equals(this.config, packet.config)) { //nothing changed, so nothing to do!
            return;
        }

        this.config = packet.config;
        this.fp2().log().info("server notified merged config update: %s", this.config);

        if (this.context != null) {
            checkState(this.config != null, "server sent null config update while a session is active!");
            this.context.notifyConfigChange(this.config);
        }
    }

    @CalledWithMonitor
    protected void handle(@NonNull SPacketUpdateConfig.Server packet) {
        this.serverConfig = packet.config;
        this.fp2().log().info("server notified remote config update: %s", this.serverConfig);
    }

    @CalledWithMonitor
    protected void handleDebug(@NonNull SPacketDebugUpdateStatistics packet) {
        this.debugServerStats = packet.tracking;
    }

    @CalledFromAnyThread
    @Override
    public DebugStats.Tracking debugServerStats() {
        return this.debugServerStats;
    }

    @CalledFromAnyThread
    @CalledFromClientThread
    @Override
    public synchronized void ready() {
        if (!this.clientReady) {
            this.clientReady = true;

            this.trySendInitialConfig();
        }
    }

    @CalledWithMonitor
    protected void trySendInitialConfig() {
        if (!this.initialConfigSent) {
            if (this.handshakeReceived && this.clientReady) {
                this.initialConfigSent = true;

                this.send(CPacketClientConfig.create(this.fp2().globalConfig()));
            }
        }
    }

    @CalledFromAnyThread
    @Override
    public synchronized void close() {
        checkState(!this.closed, "already closed!");
        this.closed = true;

        this.closeSessionIfOpen();
    }

    @CalledWithMonitor
    @SneakyThrows
    protected void closeSessionIfOpen() {
        if (this.context != null) { //a context is set, we should close it
            Identifier levelId = this.context.level().id();

            //TODO: better solution than casting to AbstractWorldClient
            try (AutoCloseable unloadLevel = () -> ((AbstractWorldClient<?, ?, ?, ?, ?>) this.world()).unloadLevel(levelId); //unload the level, since we loaded it earlier
                 IFarClientContext context = this.context) {
                this.context = null;
            }
        }
    }

    @CalledFromAnyThread
    @Override
    public FP2Config serverConfig() {
        return this.serverConfig;
    }

    @CalledFromAnyThread
    @Override
    public FP2Config config() {
        return this.config;
    }

    @CalledFromAnyThread
    @Override
    public IFarClientContext activeContext() {
        return this.context;
    }
}
