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

package net.daporkchop.fp2.asm.core.network;

import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.packet.debug.client.CPacketDebugDropAllTiles;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.core.util.annotation.CalledFromNetworkThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerServer;
import net.daporkchop.fp2.net.FP2Network;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.lib.math.vector.Vec3d;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer implements IFarPlayerServer {
    @Shadow
    public EntityPlayerMP player;

    @Unique
    private FP2Config fp2_clientConfig;
    @Unique
    private FP2Config fp2_serverConfig;
    @Unique
    private FP2Config fp2_mergedConfig;

    @Unique
    private IFarWorldServer fp2_world;

    @Unique
    private IFarRenderMode<?, ?> fp2_mode;
    @Unique
    private IFarServerContext<?, ?> fp2_context;

    @Unique
    private boolean fp2_sessionOpen;
    @Unique
    private boolean fp2_closed;

    @Override
    public Vec3d fp2_IFarPlayer_position() {
        EntityPlayerMP player = this.player;
        return Vec3d.of(player.posX, player.posY, player.posZ);
    }

    @CalledFromNetworkThread
    @Override
    public void fp2_IFarPlayerServer_handle(@NonNull Object packet) {
        this.fp2_world.fp2_IFarWorld_workerManager().rootExecutor().execute(() -> { //TODO: move all logic to network threads
            if (packet instanceof CPacketClientConfig) {
                this.handle((CPacketClientConfig) packet);
            } else if (packet instanceof CPacketDebugDropAllTiles) {
                this.handleDebug((CPacketDebugDropAllTiles) packet);
            } else {
                throw new IllegalArgumentException("don't know how to handle " + className(packet));
            }
        });
    }

    @Unique
    private void handle(@NonNull CPacketClientConfig packet) {
        this.updateConfig(this.fp2_serverConfig, packet.config());
    }

    @Unique
    private void handleDebug(@NonNull CPacketDebugDropAllTiles packet) {
        this.fp2_world.fp2_IFarWorld_workerManager().rootExecutor().execute(() -> {
            FP2_LOG.info("Dropping all tiles");
            this.fp2_world.fp2_IFarWorldServer_forEachTileProvider(tileProvider -> tileProvider.trackerManager().dropAllTiles());
        });
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_serverConfig(FP2Config serverConfig) {
        this.updateConfig(serverConfig, this.fp2_clientConfig);
    }

    @Unique
    private void updateConfig(FP2Config serverConfig, FP2Config clientConfig) {
        checkState(!this.fp2_closed, "already closed!");

        if (!Objects.equals(this.fp2_serverConfig, serverConfig)) { //re-send server config if it changed
            this.fp2_IFarPlayer_sendPacket(new SPacketUpdateConfig.Server().config(serverConfig));
        }

        this.fp2_serverConfig = serverConfig;
        this.fp2_clientConfig = clientConfig;
        FP2Config mergedConfig = FP2Config.merge(serverConfig, clientConfig);
        if (Objects.equals(this.fp2_mergedConfig, mergedConfig)) { //config hasn't changed, do nothing
            return;
        }

        IFarRenderMode<?, ?> mode = mergedConfig == null ? null : Stream.of(mergedConfig.renderModes())
                .filter(IFarRenderMode.REGISTRY::contains)
                .map(IFarRenderMode.REGISTRY::get)
                .findFirst().orElse(null);

        if (this.fp2_mode == mode) { //render mode hasn't changed
            this.updateMergedConfig(mergedConfig);

            if (this.fp2_sessionOpen) { //the session is active, we should notify the currently active context that the config has changed
                this.fp2_context.notifyConfigChange(mergedConfig);
            }
        } else { //render mode changed: end current session (if any), then set the current mode and begin a new session
            if (this.fp2_sessionOpen) {
                this.endSession();
            }

            this.fp2_mode = mode;
            this.updateMergedConfig(mergedConfig);

            if (this.canBeginSession()) {
                this.beginSession();
            }
        }
    }

    @Unique
    protected void updateMergedConfig(FP2Config mergedConfig) {
        this.fp2_mergedConfig = mergedConfig;
        this.fp2_IFarPlayer_sendPacket(new SPacketUpdateConfig.Merged().config(mergedConfig));
    }

    @Unique
    protected boolean canBeginSession() {
        return this.fp2_world != null && this.fp2_mergedConfig != null;
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_joinedWorld(@NonNull IFarWorldServer world) {
        if (this.fp2_sessionOpen) { //close any existing session, as it's in a world which is no longer the current one
            this.endSession();
        }

        this.fp2_world = world;

        if (this.canBeginSession()) { //the merged config is non-null - we can open a new session!
            this.beginSession();
        }
    }

    @Unique
    protected void beginSession() {
        checkState(!this.fp2_sessionOpen, "a session is already open!");
        this.fp2_sessionOpen = true;

        if (this.fp2_mode != null) {
            this.fp2_IFarPlayer_sendPacket(new SPacketSessionBegin().coordLimits(this.fp2_world.fp2_IFarWorld_coordLimits()));

            this.fp2_context = this.fp2_mode.serverContext(this, this.fp2_world, this.fp2_mergedConfig);
        }
    }

    @Unique
    protected void endSession() {
        checkState(this.fp2_sessionOpen, "no session is currently open!");
        this.fp2_sessionOpen = false;

        if (this.fp2_context != null) {
            this.fp2_context.close();
            this.fp2_context = null;

            this.fp2_IFarPlayer_sendPacket(new SPacketSessionEnd());
        }
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet) {
        if (!this.fp2_closed) {
            FP2Network.sendToPlayer(packet, this.player);
        }
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_update() {
        checkState(!this.fp2_closed, "already closed!");

        if (this.fp2_context != null) {
            this.fp2_context.update();
        }
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_close() {
        checkState(!this.fp2_closed, "already closed!");

        this.updateConfig(null, null);
        this.fp2_closed = true;
    }

    @Shadow
    public abstract void disconnect(ITextComponent textComponent);
}
