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

package net.daporkchop.fp2.asm.world;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.fp2.compat.cc.asyncblockaccess.CCAsyncBlockAccessImpl;
import net.daporkchop.fp2.compat.vanilla.asyncblockaccess.VanillaAsyncBlockAccessImpl;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldServer.class)
@Implements({
        @Interface(iface = IFarWorldServer.class, prefix = "fp2_world$", unique = true),
        @Interface(iface = AsyncBlockAccess.Holder.class, prefix = "fp2_asyncBlockAccess$", unique = true)
})
public abstract class MixinWorldServer extends World implements IFarWorldServer, AsyncBlockAccess.Holder {
    @Unique
    protected Map<IFarRenderMode, IFarServerContext> contextsByMode;
    @Unique
    protected IFarServerContext[] contexts;

    @Getter
    @Unique
    protected AsyncBlockAccess asyncBlockAccess;

    protected MixinWorldServer() {
        super(null, null, null, null, false);
    }

    @Override
    public void forEachContext(@NonNull Consumer<IFarServerContext<?, ?>> action) {
        for (IFarServerContext context : this.contexts) {
            action.accept(uncheckedCast(context));
        }
    }

    @Override
    public void fp2_init() {
        checkState(this.contextsByMode == null, "already initialized!");

        this.asyncBlockAccess = Constants.isCubicWorld(this)
                ? new CCAsyncBlockAccessImpl(uncheckedCast(this))
                : new VanillaAsyncBlockAccessImpl(uncheckedCast(this));

        ImmutableMap.Builder<IFarRenderMode, IFarServerContext> builder = ImmutableMap.builder();

        IFarRenderMode mode = IFarRenderMode.REGISTRY.get(FP2Config.renderMode);
        builder.put(mode, mode.serverContext(uncheckedCast(this)));

        //TODO:
        // IFarRenderMode.REGISTRY.forEachEntry((name, mode) -> builder.put(mode, mode.serverContext(uncheckedCast(this))));

        this.contextsByMode = builder.build();
        this.contexts = this.contextsByMode.values().toArray(new IFarServerContext[0]);
    }

    @Override
    public void close() {
        this.forEachContext(IFarServerContext::close);
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarServerContext<POS, T> contextFor(@NonNull IFarRenderMode<POS, T> mode) {
        IFarServerContext<POS, T> context = uncheckedCast(this.contextsByMode.get(mode));
        checkArg(context != null, "cannot find context for unknown render mode: %s", mode);
        return context;
    }
}
