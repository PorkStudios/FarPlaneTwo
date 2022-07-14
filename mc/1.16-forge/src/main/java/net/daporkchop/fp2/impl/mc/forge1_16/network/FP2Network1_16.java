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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.network;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.RegisterPacketsEvent;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.client.network.play.IMixinClientPlayNetHandler1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.network.play.IMixinServerPlayNetHandler1_16;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDispatcher;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class FP2Network1_16 {
    public static final String PROTOCOL_VERSION = "0.0.0"; //TODO: make this dynamic

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "network"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private boolean INITIALIZED = false;

    public synchronized void init(@NonNull FP2Forge1_16 fp2) {
        checkState(!INITIALIZED, "already initialized!");
        INITIALIZED = true;

        fp2.eventBus().fire(new RegisterPacketsEvent() {
            int id = 0;

            @Override
            @SneakyThrows
            public RegisterPacketsEvent registerClientbound(@NonNull Class<? extends IPacket> clazz) {
                MethodHandle constructor = MethodHandles.publicLookup().unreflectConstructor(clazz.getConstructor());
                CHANNEL.registerMessage(this.id++, uncheckedCast(clazz),
                        (packet, buffer) -> {
                            try {
                                packet.write(DataOut.wrap(buffer, false));
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        },
                        buffer -> {
                            try {
                                IPacket packet = (IPacket) constructor.invoke();
                                packet.read(DataIn.wrap(buffer, false));
                                return packet;
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        },
                        (packet, contextSupplier) -> {
                            NetworkEvent.Context context = contextSupplier.get();
                            ((IMixinClientPlayNetHandler1_16) context.getNetworkManager().getPacketListener()).fp2_playerClient().get().handle(packet);
                            context.setPacketHandled(true);
                        },
                        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
                return this;
            }

            @Override
            @SneakyThrows
            public RegisterPacketsEvent registerServerbound(@NonNull Class<? extends IPacket> clazz) {
                MethodHandle constructor = MethodHandles.publicLookup().unreflectConstructor(clazz.getConstructor());
                CHANNEL.registerMessage(this.id++, uncheckedCast(clazz),
                        (packet, buffer) -> {
                            try {
                                packet.write(DataOut.wrap(buffer, false));
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        },
                        buffer -> {
                            try {
                                IPacket packet = (IPacket) constructor.invoke();
                                packet.read(DataIn.wrap(buffer, false));
                                return packet;
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        },
                        (packet, contextSupplier) -> {
                            NetworkEvent.Context context = contextSupplier.get();
                            ((IMixinServerPlayNetHandler1_16) context.getNetworkManager().getPacketListener()).fp2_farPlayerServer().fp2_IFarPlayerServer_handle(packet);
                            context.setPacketHandled(true);
                        },
                        Optional.of(NetworkDirection.PLAY_TO_SERVER));
                return this;
            }
        });
    }

    @SneakyThrows
    public void sendToServer(@NonNull IPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    @SneakyThrows
    public void sendToPlayer(@NonNull IPacket packet, @NonNull NetworkManager networkManager) {
        CHANNEL.sendTo(packet, networkManager, NetworkDirection.PLAY_TO_CLIENT);
    }
}
