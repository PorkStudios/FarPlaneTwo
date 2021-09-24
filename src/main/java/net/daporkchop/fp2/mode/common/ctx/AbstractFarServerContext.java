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

package net.daporkchop.fp2.mode.common.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.config.FP2ConfigOld;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.net.server.SPacketRenderMode;
import net.daporkchop.fp2.util.IFarPlayer;
import net.daporkchop.fp2.util.annotation.CalledFromServerThread;
import net.minecraft.entity.player.EntityPlayerMP;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IFarServerContext}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarServerContext<POS extends IFarPos, T extends IFarTile> implements IFarServerContext<POS, T> {
    protected final IFarPlayer player;
    protected final IFarWorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final IFarTileProvider<POS, T> tileProvider;

    protected FP2Config config;

    protected boolean active = false;

    public AbstractFarServerContext(@NonNull IFarPlayer player, @NonNull IFarWorldServer world, @NonNull IFarRenderMode<POS, T> mode) {
        this.player = player;
        this.world = world;
        this.mode = mode;
        this.tileProvider = world.fp2_IFarWorldServer_tileProviderFor(mode);
    }

    @CalledFromServerThread
    @Override
    public void activate(@NonNull FP2Config config) {
        checkState(!this.active, "already active!");
        this.active = true;
        this.config = config;

        //tell the client that we're switching render modes before adding them to the tracker to ensure that it arrives before any tile data packets
        this.player.fp2_IFarPlayer_sendPacket(new SPacketRenderMode().mode(this.mode));

        this.tileProvider.tracker().playerAdd((EntityPlayerMP) this.player);
    }

    @CalledFromServerThread
    @Override
    public void notifyConfigChange(@NonNull FP2Config config) {
        checkState(this.active, "inactive context!");
        this.config = config;

        //no reason to bother scheduling an update immediately, it'll happen on the next server tick anyway
    }

    @CalledFromServerThread
    @Override
    public void deactivate() {
        checkState(this.active, "inactive context!");
        this.active = false;

        this.tileProvider.tracker().playerRemove((EntityPlayerMP) this.player);
    }

    @CalledFromServerThread
    @Override
    public void update() {
        checkState(this.active, "inactive context!");

        this.tileProvider.tracker().playerMove((EntityPlayerMP) this.player);
    }
}
