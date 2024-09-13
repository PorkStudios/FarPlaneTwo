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

package net.daporkchop.fp2.core.util.listener;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
final class ListenerListGenerator<Listener> extends CacheLoader<Integer, MethodHandle> {
    private static final int MAX_SPECIALIZE_COUNT = 5;

    private static final LoadingCache<Class<?>, ListenerListGenerator<?>> FACTORY_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues().concurrencyLevel(1)
            .build(CacheLoader.from(listenerClass -> {
                checkArg(listenerClass.isInterface(), "not an interface: %s", listenerClass);

                Method[] methods = Stream.of(listenerClass.getMethods())
                        .filter(method -> (method.getModifiers() & Modifier.STATIC) == 0)
                        .peek(method -> checkArg(method.getReturnType() == void.class, "listener method doesn't return void: %s", method))
                        .toArray(Method[]::new);

                return new ListenerListGenerator<>(listenerClass, methods);
            }));

    @SneakyThrows
    public static <Listener> ListenerListGenerator<Listener> get(@NonNull Class<Listener> listenerClass) {
        return uncheckedCast(FACTORY_CACHE.get(listenerClass));
    }

    private final @NonNull Class<Listener> listenerClass;
    private final @NonNull Method[] methods;
    private final @NonNull LoadingCache<Integer, MethodHandle> cachedListeners = CacheBuilder.newBuilder().softValues().build(this);

    @SneakyThrows
    public Listener get(@NonNull Collection<Listener> listeners) {
        Object[] arr = listeners.toArray();
        return uncheckedCast((Object) this.cachedListeners.get(arr.length <= MAX_SPECIALIZE_COUNT ? arr.length : -1).invokeExact(arr));
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public MethodHandle load(@NonNull Integer key) throws Exception {
        Integer fixedListenerCount = key < 0 ? null : key;

        Class<?> listenerArrayClass = Array.newInstance(this.listenerClass, 0).getClass();

        String listenerClassInternalName = getInternalName(this.listenerClass);
        String listenerClassDesc = getDescriptor(this.listenerClass);
        String listenerArrayClassInternalName = getInternalName(listenerArrayClass);
        String listenerArrayClassDesc = getDescriptor(listenerArrayClass);

        String dispatcherClassInternalName = fixedListenerCount != null
                ? listenerClassInternalName + "$Dispatcher$Specialized" + fixedListenerCount
                : listenerClassInternalName + "$Dispatcher$Generic";

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, dispatcherClassInternalName, null, "java/lang/Object", new String[]{ listenerClassInternalName });

        if (fixedListenerCount == null) {
            cw.visitField(ACC_PRIVATE | ACC_FINAL, "listeners", listenerArrayClassDesc, null, null).visitEnd();
        } else {
            for (int i = 0; i < fixedListenerCount; i++) {
                cw.visitField(ACC_PRIVATE | ACC_FINAL, "listener" + i, listenerClassDesc, null, null).visitEnd();
            }
        }

        { //<init>
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/Object;)V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            if (fixedListenerCount == null) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitInsn(ARRAYLENGTH);
                mv.visitLdcInsn(getType(listenerArrayClass));
                mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOf", "([Ljava/lang/Object;ILjava/lang/Class;)[Ljava/lang/Object;", false);
                mv.visitTypeInsn(CHECKCAST, listenerArrayClassInternalName);
                mv.visitFieldInsn(PUTFIELD, dispatcherClassInternalName, "listeners", listenerArrayClassDesc);
            } else {
                for (int i = 0; i < fixedListenerCount; i++) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitLdcInsn(i);
                    mv.visitInsn(AALOAD);
                    mv.visitFieldInsn(PUTFIELD, dispatcherClassInternalName, "listener" + i, listenerClassDesc);
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        for (Method method : this.methods) {
            assert method.getReturnType() == void.class : method;
            Type[] argumentTypes = getArgumentTypes(method);
            String desc = getMethodDescriptor(method);
            int lvtAlloc = getArgumentsAndReturnSizes(desc) >> 2;

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, method.getName(), desc, null, null);
            mv.visitCode();

            if (fixedListenerCount == null) {
                final int listenersLvtIndex = lvtAlloc++;
                final int indexLvtIndex = lvtAlloc++;

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, dispatcherClassInternalName, "listeners", listenerArrayClassDesc);
                mv.visitVarInsn(ASTORE, listenersLvtIndex);

                mv.visitLdcInsn(0);
                mv.visitVarInsn(ISTORE, indexLvtIndex);

                Label headLbl = new Label();
                Label tailLbl = new Label();

                mv.visitLabel(headLbl);
                mv.visitVarInsn(ILOAD, indexLvtIndex);
                mv.visitVarInsn(ALOAD, listenersLvtIndex);
                mv.visitInsn(ARRAYLENGTH);
                mv.visitJumpInsn(IF_ICMPGE, tailLbl);

                mv.visitVarInsn(ALOAD, listenersLvtIndex);
                mv.visitVarInsn(ILOAD, indexLvtIndex);
                mv.visitInsn(AALOAD);
                int lvt = 1;
                for (Type argumentType : argumentTypes) {
                    mv.visitVarInsn(argumentType.getOpcode(ILOAD), lvt);
                    lvt += argumentType.getSize();
                }
                mv.visitMethodInsn(INVOKEINTERFACE, listenerClassInternalName, method.getName(), desc, true);

                mv.visitIincInsn(indexLvtIndex, 1);
                mv.visitJumpInsn(GOTO, headLbl);

                mv.visitLabel(tailLbl);
            } else {
                final int listenerLvtBase = lvtAlloc;
                lvtAlloc += fixedListenerCount;

                //load all the listeners into local variables
                for (int i = 0; i < fixedListenerCount; i++) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, dispatcherClassInternalName, "listener" + i, listenerClassDesc);
                    mv.visitVarInsn(ASTORE, listenerLvtBase + i);
                }

                //actually invoke all the listeners
                for (int i = 0; i < fixedListenerCount; i++) {
                    mv.visitVarInsn(ALOAD, listenerLvtBase + i);
                    int lvt = 1;
                    for (Type argumentType : argumentTypes) {
                        mv.visitVarInsn(argumentType.getOpcode(ILOAD), lvt);
                        lvt += argumentType.getSize();
                    }
                    mv.visitMethodInsn(INVOKEINTERFACE, listenerClassInternalName, method.getName(), desc, true);
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        if (false) {
            Path path = Paths.get(".fp2/core/listener_list/" + dispatcherClassInternalName + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, cw.toByteArray());
        }

        Class<?> dispatcherClass = ClassloadingUtils.defineHiddenClass(this.listenerClass.getClassLoader(), cw.toByteArray());
        return MethodHandles.publicLookup()
                .findConstructor(dispatcherClass, MethodType.methodType(void.class, Object[].class))
                .asType(MethodType.methodType(Object.class, Object[].class));
    }
}
