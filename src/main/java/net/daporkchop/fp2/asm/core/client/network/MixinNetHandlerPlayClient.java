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
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.net.packet.client.CPacketClientConfig;
import net.daporkchop.fp2.net.packet.server.SPacketHandshake;
import net.daporkchop.fp2.net.packet.server.SPacketSessionBegin;
import net.daporkchop.fp2.net.packet.server.SPacketSessionEnd;
import net.daporkchop.fp2.net.packet.server.SPacketTileData;
import net.daporkchop.fp2.net.packet.server.SPacketUnloadTile;
import net.daporkchop.fp2.net.packet.server.SPacketUnloadTiles;
import net.daporkchop.fp2.net.packet.server.SPacketUpdateConfig;
import net.daporkchop.fp2.util.annotation.CalledFromNetworkThread;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient implements IFarPlayerClient {
    @Unique
    private FP2Config serverConfig;
    @Unique
    private FP2Config config;

    @Unique
    private IFarClientContext<?, ?> context;

    @Unique
    private boolean handshakeReceived;
    @Unique
    private boolean sessionOpen;

    @CalledFromNetworkThread
    @Override
    public void fp2_IFarPlayerClient_handle(@NonNull Object packet) {
        if (packet instanceof SPacketHandshake) {
            this.handshake((SPacketHandshake) packet);
        } else if (packet instanceof SPacketSessionBegin) {
            this.beginSession((SPacketSessionBegin) packet);
        } else if (packet instanceof SPacketSessionEnd) {
            this.endSession((SPacketSessionEnd) packet);
        } else if (packet instanceof SPacketUpdateConfig) {
            this.updateConfig((SPacketUpdateConfig) packet);
        } else if (packet instanceof SPacketTileData) {
            this.handleTileData((SPacketTileData) packet);
        } else if (packet instanceof SPacketUnloadTile) {
            this.handleUnloadTile((SPacketUnloadTile) packet);
        } else if (packet instanceof SPacketUnloadTiles) {
            this.handleUnloadTiles((SPacketUnloadTiles) packet);
        } else {
            throw new IllegalArgumentException("don't know how to handle " + className(packet));
        }
    }

    @Unique
    private void handshake(@NonNull SPacketHandshake packet) {
        checkState(!this.handshakeReceived, "handshake packet has already been received!");
        this.handshakeReceived = true;

        NETWORK_WRAPPER.sendToServer(new CPacketClientConfig().config(FP2Config.global()));
    }

    @Unique
    private void beginSession(@NonNull SPacketSessionBegin packet) {
        checkState(!this.sessionOpen, "a session is already open!");
        this.sessionOpen = true;

        IFarRenderMode<?, ?> mode = this.modeFor(this.config);
        if (mode != null) {
            this.context = mode.clientContext(packet.fakeWorldClient(), this.config);
        }
    }

    @Unique
    private void endSession(@NonNull SPacketSessionEnd packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        this.sessionOpen = false;

        if (this.context != null) {
            this.context.close();
            this.context = null;
        }
    }

    @Unique
    private void updateConfig(@NonNull SPacketUpdateConfig packet) {
        this.serverConfig = packet.serverConfig();

        FP2Config mergedConfig = packet.mergedConfig();
        if (Objects.equals(this.config, mergedConfig)) { //nothing changed, so nothing to do!
            return;
        }

        this.config = mergedConfig;

        if (this.context != null) {
            if (this.modeFor(this.config) == this.context.mode()) {
                this.context.notifyConfigChange(mergedConfig);
            } else {
                FP2_LOG.warn("render mode was switched while a session is active!");
            }
        }
    }

    @Unique
    private void handleTileData(@NonNull SPacketTileData packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        this.context.tileCache().receiveTile(uncheckedCast(packet.tile().compressed()));
    }

    @Unique
    private void handleUnloadTile(@NonNull SPacketUnloadTile packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        this.context.tileCache().unloadTile(uncheckedCast(packet.pos()));
    }

    @Unique
    private void handleUnloadTiles(@NonNull SPacketUnloadTiles packet) {
        checkState(this.sessionOpen, "no session is currently open!");
        checkState(this.context != null, "active session has no render mode!");

        packet.positions().forEach(PorkUtil.<IFarTileCache<IFarPos, ?>>uncheckedCast(this.context.tileCache())::unloadTile);
    }

    @Override
    public FP2Config fp2_IFarPlayerClient_serverConfig() {
        return this.serverConfig;
    }

    @Override
    public FP2Config fp2_IFarPlayerClient_config() {
        return this.config;
    }

    @Unique
    protected IFarRenderMode<?, ?> modeFor(FP2Config config) {
        return config != null && config.renderModes().length != 0 ? IFarRenderMode.REGISTRY.get(config.renderModes()[0]) : null;
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarClientContext<POS, T> fp2_IFarPlayerClient_activeContext() {
        return uncheckedCast(this.context);
    }
}
