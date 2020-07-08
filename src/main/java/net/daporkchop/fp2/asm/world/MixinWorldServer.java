/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.daporkchop.fp2.util.vanilla.VanillaCachedBlockAccessImpl;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World implements CachedBlockAccess.Holder {
    protected final CachedBlockAccess cachedBlockAccess = new VanillaCachedBlockAccessImpl((WorldServer) (Object) this);

    protected MixinWorldServer() {
        super(null, null, null, null, false);
    }

    @Override
    public CachedBlockAccess fp2_cachedBlockAccess() {
        return this.cachedBlockAccess;
    }

    @Inject(method = "Lnet/minecraft/world/WorldServer;tick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/IChunkProvider;tick()Z",
                    shift = At.Shift.AFTER))
    private void tick_postChunkProviderTick(CallbackInfo ci) {
        this.cachedBlockAccess.gc();
    }
}
