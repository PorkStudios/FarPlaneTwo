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
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.common.asm.DelegatingClassLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
class ListenerListGenerator {
    private static final LoadingCache<Class<?>, Function<Set<?>, ?>> FACTORY_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues().concurrencyLevel(1)
            .build(CacheLoader.from(ListenerListGenerator::generateInstanceFactory0));

    @SneakyThrows
    public static <Listener> Function<Set<Listener>, Listener> generateInstanceFactory(@NonNull Class<Listener> listenerClass) {
        return uncheckedCast(FACTORY_CACHE.get(listenerClass));
    }

    @SneakyThrows
    private static Function<Set<?>, ?> generateInstanceFactory0(Class<?> listenerClass) {
        //TODO: generate implementations lazily?
        final int specializeForCounts = 5;

        checkArg(listenerClass.isInterface(), "not an interface: %s", listenerClass);

        List<Method> methods = Stream.of(listenerClass.getMethods())
                .filter(method -> (method.getModifiers() & Modifier.STATIC) == 0)
                .peek(method -> checkArg(method.getReturnType() == void.class, "listener method doesn't return void: %s", method))
                .collect(Collectors.toList());

        String listenerClassInternalName = getInternalName(listenerClass);
        String factoryClassInternalName = listenerClassInternalName + "$GeneratedImpl$Factory";

        String[] specializedClassInternalNames = new String[specializeForCounts];
        for (int i = 0; i < specializeForCounts; i++) {
            specializedClassInternalNames[i] = listenerClassInternalName + "$GeneratedImpl$Specialized" + i;
        }
        String arrayClassInternalName = listenerClassInternalName + "$GeneratedImpl$Array";

        DelegatingClassLoader classLoader = new DelegatingClassLoader(listenerClass.getClassLoader());
        for (int i = 0; i < specializeForCounts; i++) {
            classLoader.defineClass(specializedClassInternalNames[i].replace('/', '.'), generateListenerImpl(methods, listenerClassInternalName, specializedClassInternalNames[i], i));
        }
        classLoader.defineClass(arrayClassInternalName.replace('/', '.'), generateListenerImpl(methods, listenerClassInternalName, arrayClassInternalName, null));

        String setInternalName = getInternalName(Set.class);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, factoryClassInternalName, null, "java/lang/Object", new String[]{ "java/util/function/Function" });

        { //<init>
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //apply(Object)
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, setInternalName);
            mv.visitLdcInsn(0);
            mv.visitTypeInsn(ANEWARRAY, listenerClassInternalName);
            mv.visitMethodInsn(INVOKEINTERFACE, setInternalName, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "[L" + listenerClassInternalName + ';');
            mv.visitVarInsn(ASTORE, 1);

            Label[] countLabels = new Label[specializeForCounts];
            for (int i = 0; i < specializeForCounts; i++) {
                countLabels[i] = new Label();
            }
            Label defaultLbl = new Label();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitTableSwitchInsn(0, specializeForCounts - 1, defaultLbl, countLabels);

            for (int i = 0; i < specializeForCounts; i++) {
                mv.visitLabel(countLabels[i]);
                mv.visitTypeInsn(NEW, specializedClassInternalNames[i]);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, specializedClassInternalNames[i], "<init>", "([L" + listenerClassInternalName + ";)V", false);
                mv.visitInsn(ARETURN);
            }

            mv.visitLabel(defaultLbl);
            mv.visitTypeInsn(NEW, arrayClassInternalName);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, arrayClassInternalName, "<init>", "([L" + listenerClassInternalName + ";)V", false);
            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        dumpClass(factoryClassInternalName, cw);

        Class<?> factoryClass = classLoader.defineClass(factoryClassInternalName.replace('/', '.'), cw.toByteArray());
        MethodHandle factoryCtor = MethodHandles.publicLookup().findConstructor(factoryClass, MethodType.methodType(void.class));
        return uncheckedCast(factoryCtor.invoke());
    }

    private static byte[] generateListenerImpl(List<Method> methods, String listenerClassInternalName, String implClassInternalName, Integer fixedListenerCount) {
        String listenerClassDesc = 'L' + listenerClassInternalName + ';';
        String listenerClassArrayDesc = '[' + listenerClassDesc;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, implClassInternalName, null, "java/lang/Object", new String[]{ listenerClassInternalName });

        if (fixedListenerCount == null) {
            cw.visitField(ACC_PRIVATE | ACC_FINAL, "listeners", listenerClassArrayDesc, null, null).visitEnd();
        } else {
            for (int i = 0; i < fixedListenerCount; i++) {
                cw.visitField(ACC_PRIVATE | ACC_FINAL, "listener" + i, listenerClassDesc, null, null).visitEnd();
            }
        }

        { //<init>
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([" + listenerClassDesc + ")V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            if (fixedListenerCount == null) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, implClassInternalName, "listeners", listenerClassArrayDesc);
            } else {
                for (int i = 0; i < fixedListenerCount; i++) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitLdcInsn(i);
                    mv.visitInsn(AALOAD);
                    mv.visitFieldInsn(PUTFIELD, implClassInternalName, "listener" + i, listenerClassDesc);
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        for (Method method : methods) {
            assert method.getReturnType() == void.class : method;
            Type[] argumentTypes = getArgumentTypes(method);
            String desc = getMethodDescriptor(method);
            int lvtAlloc = (getArgumentsAndReturnSizes(desc) >> 2) + 1;

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, method.getName(), desc, null, null);
            mv.visitCode();

            if (fixedListenerCount == null) {
                final int listenersLvtIndex = lvtAlloc++;
                final int indexLvtIndex = lvtAlloc++;

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, implClassInternalName, "listeners", listenerClassArrayDesc);
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
                for (int i = 0; i < fixedListenerCount; i++) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, implClassInternalName, "listener" + i, listenerClassDesc);
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

        dumpClass(implClassInternalName, cw);

        return cw.toByteArray();
    }

    private static void dumpClass(String internalName, ClassWriter cw) {
        if (false) {
            Path path = Paths.get(".fp2/core/listener_list/" + internalName + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, cw.toByteArray());
        }
    }
}
