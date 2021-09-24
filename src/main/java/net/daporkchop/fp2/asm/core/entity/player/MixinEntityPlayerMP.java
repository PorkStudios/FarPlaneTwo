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
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.util.IFarPlayer;
import net.daporkchop.fp2.util.annotation.CalledFromServerThread;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.spongepowered.asm.mixin.Mixin;
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
public abstract class MixinEntityPlayerMP extends EntityPlayer implements IFarPlayer {
    @Unique
    private FP2Config clientConfig;
    @Unique
    private FP2Config serverConfig;
    @Unique
    private FP2Config mergedConfig;

    @Unique
    private IFarRenderMode activeMode;
    @Unique
    private IFarServerContext activeContext;

    @Unique
    private boolean closed;

    public MixinEntityPlayerMP() {
        super(null, null);
    }

    @Override
    public IFarWorldServer fp2_IFarPlayer_world() {
        return uncheckedCast(this.world);
    }

    @Override
    public FP2Config fp2_IFarPlayer_config() {
        return this.mergedConfig;
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

        this.serverConfig = serverConfig;
        this.clientConfig = clientConfig;
        FP2Config mergedConfig = FP2Config.merge(serverConfig, clientConfig);
        if (Objects.equals(this.mergedConfig, mergedConfig)) { //config hasn't changed, do nothing
            return;
        }
        this.mergedConfig = mergedConfig;

        IFarRenderMode<?, ?> mode = this.mergedConfig == null ? null : Stream.of(this.mergedConfig.renderModes())
                .filter(IFarRenderMode.REGISTRY::contains)
                .map(IFarRenderMode.REGISTRY::get)
                .findFirst().orElse(null);

        if (this.activeMode == mode) { //render mode hasn't changed
            if (this.activeContext != null) { //render mode is non-null, we should notify the currently active context that the config has changed
                this.activeContext.notifyConfigChange(this.mergedConfig);
            }
        } else {
            this.activateContext(mode == null ? null : mode.serverContext(this, this.fp2_IFarPlayer_world()));
        }
    }

    @Unique
    private void activateContext(IFarServerContext<?, ?> context) {
        if (this.activeContext != null) { //an existing context is active, we need to shut it down before it's replaced
            this.activeContext.deactivate();
        }

        this.activeContext = context;

        if (this.activeContext != null) { //the new config is non-null, let's activate it!
            this.activeContext.activate(this.mergedConfig);
        }
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarServerContext<POS, T> fp2_IFarPlayer_activeContext() {
        return uncheckedCast(this.activeContext);
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IMessage packet) {
        NETWORK_WRAPPER.sendTo(packet, uncheckedCast(this));
    }

    @CalledFromServerThread
    @Override
    public void fp2_IFarPlayer_update() {
        checkState(!this.closed, "already closed!");

        if (this.activeContext != null) {
            this.activeContext.update();
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
