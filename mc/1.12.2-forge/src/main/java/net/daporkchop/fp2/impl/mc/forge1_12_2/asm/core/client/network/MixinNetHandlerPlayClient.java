/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.client.network;

import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.network.IMixinNetHandlerPlayClient;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.player.FarPlayerClient1_12;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient implements IMixinNetHandlerPlayClient {
    @Unique
    protected FarPlayerClient1_12 fp2_farPlayerClient;

    @Override
    public FarPlayerClient1_12 fp2_farPlayerClient() {
        if (this.fp2_farPlayerClient == null) {
            synchronized (this) {
                if (this.fp2_farPlayerClient == null) {
                    this.fp2_farPlayerClient = new FarPlayerClient1_12((FP2Forge1_12_2) fp2(), uncheckedCast(this));
                }
            }
        }
        return this.fp2_farPlayerClient;
    }

    @Inject(method = "Lnet/minecraft/client/network/NetHandlerPlayClient;cleanup()V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_cleanup_closeContext(CallbackInfo ci) {
        if (this.fp2_farPlayerClient != null) {
            this.fp2_farPlayerClient.fp2_IFarPlayerClient_close();
        }
    }
}
