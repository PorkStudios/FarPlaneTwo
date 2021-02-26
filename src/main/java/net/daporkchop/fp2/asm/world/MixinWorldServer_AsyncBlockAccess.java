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

import lombok.Getter;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.fp2.util.threading.asyncblockaccess.cc.CCAsyncBlockAccessImpl;
import net.daporkchop.fp2.util.threading.asyncblockaccess.vanilla.VanillaAsyncBlockAccessImpl;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldServer.class)
@Implements({
        @Interface(iface = AsyncBlockAccess.Holder.class, prefix = "fp2_asyncBlockAccess$", unique = true)
})
public abstract class MixinWorldServer_AsyncBlockAccess extends World implements AsyncBlockAccess.Holder {
    @Getter
    @Unique
    protected AsyncBlockAccess asyncBlockAccess;
    @Unique
    protected int cbaGcTicks;

    protected MixinWorldServer_AsyncBlockAccess() {
        super(null, null, null, null, false);
    }

    @Inject(method = "Lnet/minecraft/world/WorldServer;init()Lnet/minecraft/world/World;",
            at = @At("TAIL"))
    private void fp2_init_setAsyncBlockAccessInstance(CallbackInfoReturnable<World> ci) {
        checkState(this.asyncBlockAccess == null, "already initialized!");
        this.asyncBlockAccess = Constants.isCubicWorld(this)
                ? new CCAsyncBlockAccessImpl(uncheckedCast(this))
                : new VanillaAsyncBlockAccessImpl(uncheckedCast(this));
    }

    @Inject(method = "Lnet/minecraft/world/WorldServer;tick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/IChunkProvider;tick()Z",
                    shift = At.Shift.AFTER))
    private void fp2_tick_asyncBlockAccessGc(CallbackInfo ci) {
        if (this.cbaGcTicks++ > 40) {
            this.cbaGcTicks = 0;
            this.asyncBlockAccess.gc();
        }
    }
}
