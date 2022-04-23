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

package net.daporkchop.fp2.core.client.player;

import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.world.AbstractWorldClient;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromNetworkThread;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Objects;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarPlayerClient<F extends FP2Core> implements IFarPlayerClient {
    protected FP2Config serverConfig;
    protected FP2Config config;

    protected IFarClientContext<?, ?> context;

    protected boolean handshakeReceived;
    protected boolean clientReady;
    protected boolean initialConfigSent;
    protected boolean sessionOpen;

    protected DebugStats.Tracking debugServerStats;

    @CalledFromNetworkThread
    @Override
    public void handle(@NonNull Object packet) {
        if (packet instanceof SPacketHandshake) {
            this.handle((SPacketHandshake) packet);
        } else if (packet instanceof SPacketSessionBegin) {
            this.handle((SPacketSessionBegin) packet);
        } else if (packet instanceof SPacketSessionEnd) {
            this.handle((SPacketSessionEnd) packet);
        } else if (packet instanceof SPacketTileData) {
            this.handle((SPacketTileData) packet);
        } else if (packet instanceof SPacketUnloadTile) {
            this.handle((SPacketUnloadTile) packet);
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

    protected void handle(@NonNull SPacketHandshake packet) {
        checkState(!this.handshakeReceived, "handshake packet has already been received!");
        this.handshakeReceived = true;

        this.fp2().log().info("received server handshake");

        this.trySendInitialConfig();
    }

    protected void handle(@NonNull SPacketSessionBegin packet) {
        checkState(!this.sessionOpen, "a session is already open!");
        this.sessionOpen = true;

        IFarRenderMode<?, ?> mode = this.modeFor(this.config);
        this.fp2().log().info("beginning session with mode %s", mode);

        if (mode != null) {
            AbstractWorldClient.COORD_LIMITS_HACK.set(packet.coordLimits());
            try {
                this.context = mode.clientContext(this.loadActiveWorld(), this.config);
            } finally {
                AbstractWorldClient.COORD_LIMITS_HACK.remove();
            }
        }
    }

    protected abstract IFarLevelClient loadActiveWorld(); //TODO: i want to get rid of this and figure out how to *not* have to include the coordinate bounds in the session begin packet

    protected void handle(@NonNull SPacketSessionEnd packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        this.sessionOpen = false;

        this.fp2().log().info("ending session");

        if (this.context != null) {
            this.context.close();
            this.world().unloadLevel(this.context.level().id()); //unload the level, since we loaded it earlier
            this.context = null;
        }
    }

    protected void handle(@NonNull SPacketTileData packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        this.context.tileCache().receiveTile(uncheckedCast(packet.tile()));
        //TODO: tile compression on the network thread is simply too expensive and causes lots of issues... we need congestion control
        //this.fp2_context.tileCache().receiveTile(uncheckedCast(packet.tile().compressed()));
    }

    protected void handle(@NonNull SPacketUnloadTile packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        this.context.tileCache().unloadTile(uncheckedCast(packet.pos()));
    }

    protected void handle(@NonNull SPacketUnloadTiles packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        packet.positions().forEach(PorkUtil.<IFarTileCache<IFarPos, ?>>uncheckedCast(this.context.tileCache())::unloadTile);
    }

    protected void handle(@NonNull SPacketUpdateConfig.Merged packet) {
        if (Objects.equals(this.config, packet.config())) { //nothing changed, so nothing to do!
            return;
        }

        this.config = packet.config();
        this.fp2().log().info("server notified merged config update: %s", this.config);

        if (this.context != null) {
            if (this.modeFor(this.config) == this.context.mode()) {
                this.context.notifyConfigChange(packet.config());
            } else {
                this.fp2().log().warn("render mode was switched while a session is active!");
            }
        }
    }

    protected void handle(@NonNull SPacketUpdateConfig.Server packet) {
        this.serverConfig = packet.config();
        this.fp2().log().info("server notified remote config update: %s", this.serverConfig);
    }

    protected void handleDebug(@NonNull SPacketDebugUpdateStatistics packet) {
        this.debugServerStats = packet.tracking();
    }

    @CalledFromAnyThread
    @Override
    public DebugStats.Tracking debugServerStats() {
        return this.debugServerStats;
    }

    @CalledFromAnyThread
    @CalledFromClientThread
    @Override
    public void ready() {
        if (!this.clientReady) {
            this.clientReady = true;

            this.trySendInitialConfig();
        }
    }

    protected synchronized void trySendInitialConfig() {
        if (!this.initialConfigSent) {
            if (this.handshakeReceived && this.clientReady) {
                this.initialConfigSent = true;

                this.send(new CPacketClientConfig().config(this.fp2().globalConfig()));
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

    protected IFarRenderMode<?, ?> modeFor(FP2Config config) {
        return config != null && config.renderModes().length != 0 ? IFarRenderMode.REGISTRY.get(config.renderModes()[0]) : null;
    }

    @CalledFromAnyThread
    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarClientContext<POS, T> activeContext() {
        return uncheckedCast(this.context);
    }
}
