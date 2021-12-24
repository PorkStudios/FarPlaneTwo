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

package net.daporkchop.fp2.asm.core.client.network;

import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromNetworkThread;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FakeFarWorldClient;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.net.FP2Network;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient implements IFarPlayerClient {
    @Shadow
    @Final
    public NetworkManager netManager;
    @Shadow
    public WorldClient world;

    @Unique
    private FP2Config fp2_serverConfig;
    @Unique
    private FP2Config fp2_config;

    @Unique
    private IFarClientContext<?, ?> fp2_context;

    @Unique
    private boolean fp2_handshakeReceived;
    @Unique
    private boolean fp2_clientReady;
    @Unique
    private boolean fp2_initialConfigSent;
    @Unique
    private boolean fp2_sessionOpen;

    @Unique
    private DebugStats.Tracking fp2_debugServerStats;

    @CalledFromNetworkThread
    @Override
    public void fp2_IFarPlayerClient_handle(@NonNull Object packet) {
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

    @Unique
    private void handle(@NonNull SPacketHandshake packet) {
        checkState(!this.fp2_handshakeReceived, "handshake packet has already been received!");
        this.fp2_handshakeReceived = true;

        this.trySendInitialConfig();
    }

    @Unique
    private void handle(@NonNull SPacketSessionBegin packet) {
        checkState(!this.fp2_sessionOpen, "a session is already open!");
        this.fp2_sessionOpen = true;

        IFarRenderMode<?, ?> mode = this.modeFor(this.fp2_config);
        if (mode != null) {
            this.fp2_context = mode.clientContext(new FakeFarWorldClient(this.world, packet.coordLimits()), this.fp2_config);
        }
    }

    @Unique
    private void handle(@NonNull SPacketSessionEnd packet) {
        checkState(this.fp2_sessionOpen, "no session is currently open!");
        this.fp2_sessionOpen = false;

        if (this.fp2_context != null) {
            this.fp2_context.close();
            this.fp2_context.world().fp2_IFarWorld_close();
            this.fp2_context = null;
        }
    }

    @Unique
    private void handle(@NonNull SPacketTileData packet) {
        checkState(this.fp2_sessionOpen, "no session is currently open!");
        checkState(this.fp2_context != null, "active session has no render mode!");

        this.fp2_context.tileCache().receiveTile(uncheckedCast(packet.tile()));
        //TODO: tile compression on the network thread is simply too expensive and causes lots of issues... we need congestion control
        //this.fp2_context.tileCache().receiveTile(uncheckedCast(packet.tile().compressed()));
    }

    @Unique
    private void handle(@NonNull SPacketUnloadTile packet) {
        checkState(this.fp2_sessionOpen, "no session is currently open!");
        checkState(this.fp2_context != null, "active session has no render mode!");

        this.fp2_context.tileCache().unloadTile(uncheckedCast(packet.pos()));
    }

    @Unique
    private void handle(@NonNull SPacketUnloadTiles packet) {
        checkState(this.fp2_sessionOpen, "no session is currently open!");
        checkState(this.fp2_context != null, "active session has no render mode!");

        packet.positions().forEach(PorkUtil.<IFarTileCache<IFarPos, ?>>uncheckedCast(this.fp2_context.tileCache())::unloadTile);
    }

    @Unique
    private void handle(@NonNull SPacketUpdateConfig.Merged packet) {
        if (Objects.equals(this.fp2_config, packet.config())) { //nothing changed, so nothing to do!
            return;
        }

        this.fp2_config = packet.config();

        if (this.fp2_context != null) {
            if (this.modeFor(this.fp2_config) == this.fp2_context.mode()) {
                this.fp2_context.notifyConfigChange(packet.config());
            } else {
                fp2().log().warn("render mode was switched while a session is active!");
            }
        }
    }

    @Unique
    private void handle(@NonNull SPacketUpdateConfig.Server packet) {
        this.fp2_serverConfig = packet.config();
    }

    @Unique
    private void handleDebug(@NonNull SPacketDebugUpdateStatistics packet) {
        this.fp2_debugServerStats = packet.tracking();
    }

    @Override
    public void fp2_IFarPlayerClient_send(@NonNull IPacket packet) {
        FP2Network.sendToServer(packet);
    }

    @CalledFromAnyThread
    @Override
    public DebugStats.Tracking fp2_IFarPlayerClient_debugServerStats() {
        return this.fp2_debugServerStats;
    }

    @CalledFromAnyThread
    @CalledFromClientThread
    @Override
    public void fp2_IFarPlayerClient_ready() {
        if (!this.fp2_clientReady) {
            this.fp2_clientReady = true;

            this.trySendInitialConfig();
        }
    }

    @Unique
    protected synchronized void trySendInitialConfig() {
        if (!this.fp2_initialConfigSent) {
            if (this.fp2_handshakeReceived && this.fp2_clientReady) {
                this.fp2_initialConfigSent = true;

                this.fp2_IFarPlayerClient_send(new CPacketClientConfig().config(fp2().globalConfig()));
            }
        }
    }

    @Override
    public FP2Config fp2_IFarPlayerClient_serverConfig() {
        return this.fp2_serverConfig;
    }

    @CalledFromAnyThread
    @Override
    public FP2Config fp2_IFarPlayerClient_config() {
        return this.fp2_config;
    }

    @Unique
    protected IFarRenderMode<?, ?> modeFor(FP2Config config) {
        return config != null && config.renderModes().length != 0 ? IFarRenderMode.REGISTRY.get(config.renderModes()[0]) : null;
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarClientContext<POS, T> fp2_IFarPlayerClient_activeContext() {
        return uncheckedCast(this.fp2_context);
    }

    @Inject(method = "Lnet/minecraft/client/network/NetHandlerPlayClient;cleanup()V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_cleanup_closeContext(CallbackInfo ci) {
        this.netManager.channel().eventLoop().execute(() -> {
            if (this.fp2_sessionOpen) {
                this.handle(new SPacketSessionEnd());
            }
        });
    }
}
