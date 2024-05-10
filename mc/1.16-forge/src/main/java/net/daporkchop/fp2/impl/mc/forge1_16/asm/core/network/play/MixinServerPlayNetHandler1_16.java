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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.network.play;

import lombok.Getter;
import net.daporkchop.fp2.core.netty.network.flow.NettyFlowControl;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.network.play.IMixinServerPlayNetHandler1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.player.FarPlayerServer1_16;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
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
@Mixin(ServerPlayNetHandler.class)
public abstract class MixinServerPlayNetHandler1_16 implements IMixinServerPlayNetHandler1_16 {
    @Getter
    @Unique
    protected FarPlayerServer1_16 fp2_farPlayerServer;

    @Inject(method = "<init>*",
            at = @At("RETURN"),
            allow = 1, require = 1)
    private void fp2_$init$_createFarPlayerServer(MinecraftServer server, NetworkManager connection, ServerPlayerEntity player, CallbackInfo ci) {
        this.fp2_farPlayerServer = new FarPlayerServer1_16((FP2Forge1_16) fp2(), uncheckedCast(this), NettyFlowControl.createFlowControlFor(connection.channel()));
    }
}
