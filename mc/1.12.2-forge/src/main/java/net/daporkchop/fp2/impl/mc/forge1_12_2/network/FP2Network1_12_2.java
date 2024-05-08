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

package net.daporkchop.fp2.impl.mc.forge1_12_2.network;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.RegisterPacketsEvent;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.network.IMixinNetHandlerPlayClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.network.IMixinNetHandlerPlayServer1_12;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.invoke.MethodHandle;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class FP2Network1_12_2 {
    private static final BetterNetworkWrapper1_12<IPacket> PROTOCOL_FP2 = new BetterNetworkWrapper1_12<>(MODID);

    private boolean INITIALIZED = false;

    /**
     * Called during {@link FMLPreInitializationEvent}.
     */
    public synchronized void init() {
        checkState(!INITIALIZED, "already initialized!");
        INITIALIZED = true;

        registerStandard();
    }

    private void registerStandard() {
        BiConsumer<IPacket, INetHandler> serverboundHandler =
                (message, netHandler) -> ((IMixinNetHandlerPlayServer1_12) (NetHandlerPlayServer) netHandler).fp2_farPlayerServer().fp2_IFarPlayerServer_handle(message);
        BiConsumer<IPacket, INetHandler> clientboundHandler = fp2().hasClient() ?
                (message, netHandler) -> ((IMixinNetHandlerPlayClient1_12) (NetHandlerPlayClient) netHandler).fp2_playerClient().get().handle(message) :
                (message, netHandler) -> {
                    throw new IllegalStateException("attempted to handle clientbound packet on dedicated server: " + className(message));
                };

        fp2().eventBus().fire(new RegisterPacketsEvent() {
            int id = 0;

            @Override
            public RegisterPacketsEvent registerClientbound(@NonNull Class<? extends IPacket> clazz) {
                PROTOCOL_FP2.registerMessage(clientboundHandler, clazz, this.id++, Side.CLIENT);
                return this;
            }

            @Override
            public RegisterPacketsEvent registerServerbound(@NonNull Class<? extends IPacket> clazz) {
                PROTOCOL_FP2.registerMessage(serverboundHandler, clazz, this.id++, Side.SERVER);
                return this;
            }
        });
    }

    public void sendToServer(@NonNull IPacket packet) {
        PROTOCOL_FP2.sendToServer(packet);
    }

    public void sendToPlayer(@NonNull IPacket packet, @NonNull EntityPlayerMP player) {
        PROTOCOL_FP2.sendTo(packet, player);
    }

    public void sendToPlayer(@NonNull IPacket packet, @NonNull EntityPlayerMP player, Consumer<Throwable> action) {
        if (action == null) {
            sendToPlayer(packet, player);
            return;
        }

        PROTOCOL_FP2.sendTo(packet, player, future -> {
            if (future.isSuccess()) {
                action.accept(null);
            } else {
                future.channel().pipeline().fireExceptionCaught(future.cause());
                action.accept(future.cause());
            }
        });
    }
}
