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

package net.daporkchop.fp2.core.server.player;

import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.ctx.VoxelServerContext;
import net.daporkchop.fp2.core.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.core.network.packet.debug.client.CPacketDebugDropAllTiles;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromServerThread;

import java.util.Objects;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarPlayerServer implements IFarPlayerServer {
    protected FP2Config clientConfig;
    protected FP2Config serverConfig;
    protected FP2Config mergedConfig;

    protected IFarLevelServer world;

    protected IFarServerContext context;

    protected boolean sessionOpen;
    protected boolean closed;

    @CalledFromAnyThread
    @Override
    public void fp2_IFarPlayerServer_handle(@NonNull Object packet) {
        //delegate packet handling to server thread
        this.world.workerManager().rootExecutor().execute(() -> { //TODO: do this on the invoking thread
            if (packet instanceof CPacketClientConfig) {
                this.handle((CPacketClientConfig) packet);
            } else if (packet instanceof CPacketDebugDropAllTiles) {
                this.handleDebug((CPacketDebugDropAllTiles) packet);
            } else {
                throw new IllegalArgumentException("don't know how to handle " + className(packet));
            }
        });
    }

    protected void handle(@NonNull CPacketClientConfig packet) {
        this.updateConfig(this.serverConfig, packet.config());
    }

    protected void handleDebug(@NonNull CPacketDebugDropAllTiles packet) {
        this.world.workerManager().rootExecutor().execute(() -> {
            this.fp2().log().info("Dropping all tiles");
            this.world.tileProvider().trackerManager().dropAllTiles();
        });
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_serverConfig(FP2Config serverConfig) {
        this.updateConfig(serverConfig, this.clientConfig);
    }

    protected void updateConfig(FP2Config serverConfig, FP2Config clientConfig) {
        checkState(!this.closed, "already closed!");

        if (!Objects.equals(this.serverConfig, serverConfig)) { //re-send server config if it changed
            this.fp2_IFarPlayer_sendPacket(new SPacketUpdateConfig.Server().config(serverConfig));
        }

        this.serverConfig = serverConfig;
        this.clientConfig = clientConfig;
        FP2Config mergedConfig = FP2Config.merge(serverConfig, clientConfig);
        if (Objects.equals(this.mergedConfig, mergedConfig)) { //config hasn't changed, do nothing
            return;
        }

        if ((this.mergedConfig != null) == (mergedConfig != null)) { //the merged config hasn't changed: we're either preserving the already open session, or the session will remain closed
            this.updateMergedConfig(mergedConfig);

            if (this.sessionOpen) { //the session is active, we should notify the currently active context that the config has changed
                this.context.notifyConfigChange(mergedConfig);
            }
        } else { //either the currently open session needs to be closed, or we need to open a new session
            if (this.sessionOpen) {
                this.endSession();
            }

            this.updateMergedConfig(mergedConfig);

            if (this.canBeginSession()) {
                this.beginSession();
            }
        }
    }

    protected void updateMergedConfig(FP2Config mergedConfig) {
        this.mergedConfig = mergedConfig;
        this.fp2_IFarPlayer_sendPacket(new SPacketUpdateConfig.Merged().config(mergedConfig));
    }

    protected boolean canBeginSession() {
        return this.world != null //player is in a world
               && this.mergedConfig != null; //both server and client have set their config
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_joinedWorld(@NonNull IFarLevelServer world) {
        if (this.sessionOpen) { //close any existing session, as it's in a world which is no longer the current one
            this.endSession();
        }

        this.world = world;

        if (this.canBeginSession()) { //the merged config is non-null - we can open a new session!
            this.beginSession();
        }
    }

    protected void beginSession() {
        checkState(!this.sessionOpen, "a session is already open!");
        this.sessionOpen = true;

        if (this.mergedConfig != null) {
            this.fp2_IFarPlayer_sendPacket(new SPacketSessionBegin().coordLimits(this.world.coordLimits()));

            this.context = new VoxelServerContext(this, this.world, this.mergedConfig);
        }
    }

    protected void endSession() {
        checkState(this.sessionOpen, "no session is currently open!");
        this.sessionOpen = false;

        if (this.context != null) {
            this.context.close();
            this.context = null;

            this.fp2_IFarPlayer_sendPacket(new SPacketSessionEnd());
        }
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_update() {
        checkState(!this.closed, "already closed!");

        if (this.context != null) {
            this.context.update();
        }
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_close() {
        checkState(!this.closed, "already closed!");

        this.updateConfig(null, null);
        this.closed = true;
    }
}
