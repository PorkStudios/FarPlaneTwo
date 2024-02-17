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
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.RegisterPacketsEvent;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.network.IMixinNetHandlerPlayClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.network.IMixinNetHandlerPlayServer1_12;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class FP2Network1_12_2 {
    private static final SimpleNetworkWrapper PROTOCOL_FP2 = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    private static final Map<Class<? extends IPacket>, MethodHandle> WRAPPER_CONSTRUCTOR_HANDLES_CLIENTBOUND = new IdentityHashMap<>();
    private static final Map<Class<? extends IPacket>, MethodHandle> WRAPPER_CONSTRUCTOR_HANDLES_SERVERBOUND = new IdentityHashMap<>();

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
        class ServerboundHandler implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                ((IMixinNetHandlerPlayServer1_12) ctx.getServerHandler()).fp2_farPlayerServer().fp2_IFarPlayerServer_handle(((Supplier) message).get());
                return null;
            }
        }

        class ClientboundHandler implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                ((IMixinNetHandlerPlayClient1_12) ctx.getClientHandler()).fp2_playerClient().get().handle(((Supplier) message).get());
                return null;
            }
        }

        class ClientboundHandlerOnDedicatedServer implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                throw new IllegalStateException("attempted to handle clientbound packet on dedicated server: " + className(message));
            }
        }

        IMessageHandler<IMessage, IMessage> serverboundHandler = new ServerboundHandler();
        IMessageHandler<IMessage, IMessage> clientboundHandler = fp2().hasClient() ? new ClientboundHandler() : new ClientboundHandlerOnDedicatedServer();

        fp2().eventBus().fire(new RegisterPacketsEvent() {
            int id = 0;

            @Override
            @SneakyThrows
            public RegisterPacketsEvent registerClientbound(@NonNull Class<? extends IPacket> clazz) {
                Class<? extends IMessage> wrapperClass = this.generateWrapperClass(clazz);
                PROTOCOL_FP2.registerMessage(clientboundHandler, wrapperClass, this.id++, Side.CLIENT);
                WRAPPER_CONSTRUCTOR_HANDLES_CLIENTBOUND.put(clazz, MethodHandles.publicLookup().unreflectConstructor(wrapperClass.getConstructor(clazz)));
                return this;
            }

            @Override
            @SneakyThrows
            public RegisterPacketsEvent registerServerbound(@NonNull Class<? extends IPacket> clazz) {
                Class<? extends IMessage> wrapperClass = this.generateWrapperClass(clazz);
                PROTOCOL_FP2.registerMessage(serverboundHandler, wrapperClass, this.id++, Side.SERVER);
                WRAPPER_CONSTRUCTOR_HANDLES_SERVERBOUND.put(clazz, MethodHandles.publicLookup().unreflectConstructor(wrapperClass.getConstructor(clazz)));
                return this;
            }

            protected Class<? extends IMessage> generateWrapperClass(@NonNull Class<? extends IPacket> clazz) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, clazz.getSimpleName() + "_IMessageWrapper", null, "java/lang/Object", new String[]{
                        Type.getInternalName(Supplier.class),
                        Type.getInternalName(IMessage.class),
                });

                writer.visitField(ACC_PRIVATE, "child", Type.getDescriptor(clazz), null, null).visitEnd();

                { //constructor
                    MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitTypeInsn(NEW, Type.getInternalName(clazz));
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(clazz), "<init>", "()V", false);
                    mv.visitFieldInsn(PUTFIELD, clazz.getSimpleName() + "_IMessageWrapper", "child", Type.getDescriptor(clazz));
                    mv.visitInsn(RETURN);

                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                { //constructor
                    MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(Type.getInternalName(clazz))), null, null);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitFieldInsn(PUTFIELD, clazz.getSimpleName() + "_IMessageWrapper", "child", Type.getDescriptor(clazz));
                    mv.visitInsn(RETURN);

                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                { //void fromBytes(ByteBuf buf)
                    MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "fromBytes", "(Lio/netty/buffer/ByteBuf;)V", null, null);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, clazz.getSimpleName() + "_IMessageWrapper", "child", Type.getDescriptor(clazz));
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitLdcInsn(false);
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/binary/stream/DataIn", "wrap", "(Lio/netty/buffer/ByteBuf;Z)Lnet/daporkchop/lib/binary/stream/DataIn;", true);
                    mv.visitMethodInsn(INVOKEINTERFACE, "net/daporkchop/fp2/core/network/IPacket", "read", "(Lnet/daporkchop/lib/binary/stream/DataIn;)V", true);
                    mv.visitInsn(RETURN);

                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                { //void toBytes(ByteBuf buf)
                    MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "toBytes", "(Lio/netty/buffer/ByteBuf;)V", null, null);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, clazz.getSimpleName() + "_IMessageWrapper", "child", Type.getDescriptor(clazz));
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitLdcInsn(false);
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/binary/stream/DataOut", "wrap", "(Lio/netty/buffer/ByteBuf;Z)Lnet/daporkchop/lib/binary/stream/DataOut;", true);
                    mv.visitMethodInsn(INVOKEINTERFACE, "net/daporkchop/fp2/core/network/IPacket", "write", "(Lnet/daporkchop/lib/binary/stream/DataOut;)V", true);
                    mv.visitInsn(RETURN);

                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                { //Object get()
                    MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "get", "()Ljava/lang/Object;", null, null);

                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, clazz.getSimpleName() + "_IMessageWrapper", "child", Type.getDescriptor(clazz));
                    mv.visitInsn(ARETURN);

                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                writer.visitEnd();

                if (false) {
                    try {
                        Files.write(Paths.get(clazz.getSimpleName() + "_IMessageWrapper.class"), writer.toByteArray());
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }

                return uncheckedCast(ClassloadingUtils.defineHiddenClass(clazz.getClassLoader(), writer.toByteArray()));
            }
        });
    }

    @SneakyThrows
    public void sendToServer(@NonNull IPacket packet) {
        PROTOCOL_FP2.sendToServer((IMessage) WRAPPER_CONSTRUCTOR_HANDLES_SERVERBOUND.get(packet.getClass()).invoke(packet));
    }

    @SneakyThrows
    public void sendToPlayer(@NonNull IPacket packet, @NonNull EntityPlayerMP player) {
        PROTOCOL_FP2.sendTo((IMessage) WRAPPER_CONSTRUCTOR_HANDLES_CLIENTBOUND.get(packet.getClass()).invoke(packet), player);
    }
}
