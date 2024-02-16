/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.network;

import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.network.IMixinNetHandlerPlayServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.player.FarPlayerServer1_12;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer1_12 implements IMixinNetHandlerPlayServer1_12 {
    @Shadow
    public EntityPlayerMP player;

    @Unique
    protected FarPlayerServer1_12 fp2_farPlayerServer;

    @Override
    public FarPlayerServer1_12 fp2_farPlayerServer() {
        if (this.fp2_farPlayerServer == null) {
            synchronized (this) {
                if (this.fp2_farPlayerServer == null) {
                    this.fp2_farPlayerServer = new FarPlayerServer1_12((FP2Forge1_12) fp2(), () -> this.player);
                }
            }
        }
        return this.fp2_farPlayerServer;
    }
}
