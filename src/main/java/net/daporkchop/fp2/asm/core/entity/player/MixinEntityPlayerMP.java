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

package net.daporkchop.fp2.asm.core.entity.player;

import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.net.packet.server.SPacketSessionBegin;
import net.daporkchop.fp2.net.packet.server.SPacketSessionEnd;
import net.daporkchop.fp2.net.packet.server.SPacketUpdateConfig;
import net.daporkchop.fp2.mode.api.player.IFarPlayerServer;
import net.daporkchop.fp2.util.annotation.CalledFromServerThread;
import net.daporkchop.lib.math.vector.d.Vec3d;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
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
@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP extends EntityPlayer implements IFarPlayerServer {
    @Shadow
    public NetHandlerPlayServer connection;
    @Unique
    private FP2Config clientConfig;
    @Unique
    private FP2Config serverConfig;
    @Unique
    private FP2Config mergedConfig;

    @Unique
    private IFarWorldServer world;

    @Unique
    private IFarRenderMode<?, ?> mode;
    @Unique
    private IFarServerContext<?, ?> context;

    @Unique
    private boolean sessionOpen;
    @Unique
    private boolean closed;

    public MixinEntityPlayerMP() {
        super(null, null);
    }

    @Override
    public Vec3d fp2_IFarPlayer_position() {
        return new Vec3d(this.posX, this.posY, this.posZ);
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_serverConfig(FP2Config serverConfig) {
        this.updateConfig(serverConfig, this.clientConfig);
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_clientConfig(FP2Config clientConfig) {
        this.updateConfig(this.serverConfig, clientConfig);
    }

    @Unique
    private void updateConfig(FP2Config serverConfig, FP2Config clientConfig) {
        checkState(!this.closed, "already closed!");

        if (!Objects.equals(this.serverConfig, serverConfig)) { //re-send server config if it changed
            this.fp2_IFarPlayer_sendPacket(new SPacketUpdateConfig().serverConfig(this.serverConfig).mergedConfig(this.mergedConfig));
        }

        this.serverConfig = serverConfig;
        this.clientConfig = clientConfig;
        FP2Config mergedConfig = FP2Config.merge(serverConfig, clientConfig);
        if (Objects.equals(this.mergedConfig, mergedConfig)) { //config hasn't changed, do nothing
            return;
        }

        IFarRenderMode<?, ?> mode = mergedConfig == null ? null : Stream.of(mergedConfig.renderModes())
                .filter(IFarRenderMode.REGISTRY::contains)
                .map(IFarRenderMode.REGISTRY::get)
                .findFirst().orElse(null);

        if (this.mode == mode) { //render mode hasn't changed
            this.updateMergedConfig(mergedConfig);

            if (this.sessionOpen) { //the session is active, we should notify the currently active context that the config has changed
                this.context.notifyConfigChange(mergedConfig);
            }
        } else { //render mode changed: end current session (if any), then set the current mode and begin a new session
            if (this.sessionOpen) {
                this.endSession();
            }

            this.mode = mode;
            this.updateMergedConfig(mergedConfig);

            if (this.canBeginSession()) {
                this.beginSession();
            }
        }
    }

    @Unique
    protected void updateMergedConfig(FP2Config mergedConfig) {
        this.mergedConfig = mergedConfig;
        this.fp2_IFarPlayer_sendPacket(new SPacketUpdateConfig().serverConfig(this.serverConfig).mergedConfig(mergedConfig));
    }

    @Unique
    protected boolean canBeginSession() {
        return this.world != null && this.mergedConfig != null;
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_joinedWorld(@NonNull IFarWorldServer world) {
        if (this.sessionOpen) { //close any existing session, as it's in a world which is no longer the current one
            this.endSession();
        }

        this.world = world;

        if (this.canBeginSession()) { //the merged config is non-null - we can open a new session!
            this.beginSession();
        }
    }

    @Unique
    protected void beginSession() {
        checkState(!this.sessionOpen, "a session is already open!");
        this.sessionOpen = true;

        if (this.mode != null) {
            this.fp2_IFarPlayer_sendPacket(new SPacketSessionBegin().coordLimits(this.world.fp2_IFarWorld_coordLimits()));

            this.context = this.mode.serverContext(this, this.world, this.mergedConfig);
        }
    }

    @Unique
    protected void endSession() {
        checkState(this.sessionOpen, "no session is currently open!");
        this.sessionOpen = false;

        if (this.context != null) {
            this.context.close();
            this.context = null;

            this.fp2_IFarPlayer_sendPacket(new SPacketSessionEnd());
        }
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IMessage packet) {
        if (!this.closed) {
            NETWORK_WRAPPER.sendTo(packet, uncheckedCast(this));
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
