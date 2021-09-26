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

package net.daporkchop.fp2.asm.core.client.multiplayer;

import net.daporkchop.fp2.asm.core.world.MixinWorld;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.util.annotation.CalledFromNetworkThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.text.TextComponentTranslation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends MixinWorld implements IFarWorldClient {
    @Shadow
    @Final
    private Minecraft mc;

    @Unique
    private FP2Config config;

    @Unique
    private IFarClientContext<?, ?> context;

    @Unique
    private boolean sessionOpen;

    @CalledFromClientThread
    @Override
    public void fp2_IFarWorld_init() {
        super.fp2_IFarWorld_init();

        this.mc.player.sendMessage(new TextComponentTranslation(MODID + ".playerJoinWarningMessage"));
    }

    @CalledFromNetworkThread
    @Override
    public void fp2_IFarWorldClient_config(FP2Config config) {
        if (Objects.equals(this.config, config)) { //nothing changed, so nothing to do!
            return;
        }

        this.config = config;

        if (this.context != null) {
            if (this.modeFor(this.config) == this.context.mode()) {
                this.context.notifyConfigChange(config);
            } else {
                FP2_LOG.warn("render mode was switched while a session is active!");
            }
        }
    }

    @Override
    public FP2Config fp2_IFarWorldClient_config() {
        return this.config;
    }

    @CalledFromNetworkThread
    @Override
    public void fp2_IFarWorldClient_beginSession() {
        checkState(!this.sessionOpen, "a session is already open!");
        this.sessionOpen = true;

        IFarRenderMode<?, ?> mode = this.modeFor(this.config);
        if (mode != null) {
            this.context = mode.clientContext(uncheckedCast(this), this.config);
        }
    }

    @Unique
    protected IFarRenderMode<?, ?> modeFor(FP2Config config) {
        return config != null && config.renderModes().length != 0 ? IFarRenderMode.REGISTRY.get(config.renderModes()[0]) : null;
    }

    @CalledFromNetworkThread
    @Override
    public void fp2_IFarWorldClient_endSession() {
        checkState(this.sessionOpen, "no session is currently open!");
        this.sessionOpen = false;

        if (this.context != null) {
            this.context.close();
            this.context = null;
        }
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarClientContext<POS, T> fp2_IFarWorldClient_activeContext() {
        return uncheckedCast(this.context);
    }

    @CalledFromClientThread
    @Override
    public void fp2_IFarWorld_close() {
        //TODO: see if i need to do any cleanup on the client thread
    }
}
