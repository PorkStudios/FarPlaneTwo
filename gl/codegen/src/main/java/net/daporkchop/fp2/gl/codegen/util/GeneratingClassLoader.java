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

package net.daporkchop.fp2.gl.codegen.util;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.DelegatingClassLoader;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@NoArgsConstructor
public abstract class GeneratingClassLoader extends DelegatingClassLoader {
    protected static final boolean WRITE_CLASSES = GeneratingClassLoader.class.desiredAssertionStatus();

    protected static final Type OBJECT_TYPE = getType(Object.class);

    protected static final long MEMCPY_USE_UNSAFE_THRESHOLD = Long.getLong(preventInline("fp2.gl.opengl.") + "memCpyUseUnsafeThreshold", 64L);

    public GeneratingClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected abstract Class<?> findClass(String name) throws ClassNotFoundException;

    //
    // CODEGEN UTILITY METHODS
    //

    @SuppressWarnings("SameParameterValue")
    protected static void generateMemcpy(MethodVisitor mv, int srcOffsetLvtIndex, int dstOffsetLvtIndex, @Positive long size) {
        if (positive(size, "size") >= MEMCPY_USE_UNSAFE_THRESHOLD) { //copy is larger than the configured threshold, delegate to PUnsafe.copyMemory()
            mv.visitVarInsn(LLOAD, dstOffsetLvtIndex);
            mv.visitVarInsn(LLOAD, srcOffsetLvtIndex);
            mv.visitLdcInsn(size);
            mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "copyMemory", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
            return;
        }

        //generate a sequence of instructions emulating a simple memcpy by copying one long at a time, and padding it with ints/shorts/bytes if not an exact multiple
        for (long pos = 0L; pos < size; ) {
            //dst
            mv.visitVarInsn(LLOAD, dstOffsetLvtIndex);
            mv.visitLdcInsn(pos);
            mv.visitInsn(LADD);

            //src
            mv.visitVarInsn(LLOAD, srcOffsetLvtIndex);
            mv.visitLdcInsn(pos);
            mv.visitInsn(LADD);

            //find the biggest integer type <= the remaining size and copy exactly one of it
            if (size - pos >= LONG_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getLong", getMethodDescriptor(LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putLong", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                pos += LONG_SIZE;
            } else if (size - pos >= INT_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getInt", getMethodDescriptor(INT_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putInt", getMethodDescriptor(VOID_TYPE, LONG_TYPE, INT_TYPE), PUnsafe.class.isInterface());
                pos += INT_SIZE;
            } else if (size - pos >= SHORT_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getShort", getMethodDescriptor(SHORT_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putShort", getMethodDescriptor(VOID_TYPE, LONG_TYPE, SHORT_TYPE), PUnsafe.class.isInterface());
                pos += SHORT_SIZE;
            } else if (size - pos >= BYTE_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getByte", getMethodDescriptor(BYTE_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putByte", getMethodDescriptor(VOID_TYPE, LONG_TYPE, BYTE_TYPE), PUnsafe.class.isInterface());
                pos += BYTE_SIZE;
            } else {
                throw new IllegalArgumentException(String.valueOf(size - pos));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected static void generateMemcpy(MethodVisitor mv, int srcBaseLvtIndex, int srcOffsetLvtIndex, int dstBaseLvtIndex, int dstOffsetLvtIndex, @Positive long size) {
        if (positive(size, "size") >= MEMCPY_USE_UNSAFE_THRESHOLD) { //copy is larger than the configured threshold, delegate to PUnsafe.copyMemory()
            mv.visitVarInsn(ALOAD, dstBaseLvtIndex);
            mv.visitVarInsn(LLOAD, dstOffsetLvtIndex);
            mv.visitVarInsn(ALOAD, srcBaseLvtIndex);
            mv.visitVarInsn(LLOAD, srcOffsetLvtIndex);
            mv.visitLdcInsn(size);
            mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "copyMemory", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, getType(Object.class), LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
            return;
        }

        //generate a sequence of instructions emulating a simple memcpy by copying one long at a time, and padding it with ints/shorts/bytes if not an exact multiple
        for (long pos = 0L; pos < size; ) {
            //dst
            mv.visitVarInsn(ALOAD, dstBaseLvtIndex);
            mv.visitVarInsn(LLOAD, dstOffsetLvtIndex);
            mv.visitLdcInsn(pos);
            mv.visitInsn(LADD);

            //src
            mv.visitVarInsn(ALOAD, srcBaseLvtIndex);
            mv.visitVarInsn(LLOAD, srcOffsetLvtIndex);
            mv.visitLdcInsn(pos);
            mv.visitInsn(LADD);

            //find the biggest integer type <= the remaining size and copy exactly one of it
            if (size - pos >= LONG_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getLong", getMethodDescriptor(LONG_TYPE, getType(Object.class), LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putLong", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                pos += LONG_SIZE;
            } else if (size - pos >= INT_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getInt", getMethodDescriptor(INT_TYPE, getType(Object.class), LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putInt", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, INT_TYPE), PUnsafe.class.isInterface());
                pos += INT_SIZE;
            } else if (size - pos >= SHORT_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getShort", getMethodDescriptor(SHORT_TYPE, getType(Object.class), LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putShort", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, SHORT_TYPE), PUnsafe.class.isInterface());
                pos += SHORT_SIZE;
            } else if (size - pos >= BYTE_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getByte", getMethodDescriptor(BYTE_TYPE, getType(Object.class), LONG_TYPE), PUnsafe.class.isInterface());
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putByte", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, BYTE_TYPE), PUnsafe.class.isInterface());
                pos += BYTE_SIZE;
            } else {
                throw new IllegalArgumentException(String.valueOf(size - pos));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected static void generateTryFinally(MethodVisitor mv, int lvtIndexAllocator, IntConsumer tryGenerator, IntConsumer finallyGenerator) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label tail = new Label();

        mv.visitTryCatchBlock(start, end, handler, null);

        //try
        mv.visitLabel(start);
        tryGenerator.accept(lvtIndexAllocator);
        mv.visitLabel(end);

        //finally
        finallyGenerator.accept(lvtIndexAllocator);

        //jump to tail
        mv.visitJumpInsn(GOTO, tail);

        //exception handler: finally, then re-throw exception
        mv.visitLabel(handler);
        finallyGenerator.accept(lvtIndexAllocator);
        mv.visitInsn(ATHROW);

        mv.visitLabel(tail);
    }

    @SuppressWarnings("SameParameterValue")
    protected static void generateTryWithCleanupOnException(MethodVisitor mv, int lvtIndexAllocator, IntConsumer tryGenerator, IntConsumer exceptionalCleanupGenerator) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label tail = new Label();

        mv.visitTryCatchBlock(start, end, handler, null);

        //try
        mv.visitLabel(start);
        tryGenerator.accept(lvtIndexAllocator);
        mv.visitLabel(end);

        //jump to tail
        mv.visitJumpInsn(GOTO, tail);

        //exception handler: clean up, then re-throw exception
        mv.visitLabel(handler);
        exceptionalCleanupGenerator.accept(lvtIndexAllocator);
        mv.visitInsn(ATHROW);

        mv.visitLabel(tail);
    }

    protected static byte[] generateClass(int access, String internalName, String superclass, String[] interfaces, Consumer<ClassVisitor> generator) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, access, internalName, null, superclass, interfaces);
        generator.accept(cw);
        return finish(cw, internalName);
    }

    @SneakyThrows(IOException.class)
    protected static byte[] finish(ClassWriter cw, String internalName) {
        cw.visitEnd();
        byte[] bytecode = cw.toByteArray();

        if (WRITE_CLASSES) {
            Path path = Paths.get(".fp2" + File.separatorChar + "gl" + File.separatorChar + internalName + ".class");
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.write(path, bytecode);
        }

        return bytecode;
    }

    protected static MethodVisitor beginMethod(ClassVisitor cv, int access, String name, String desc) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
        mv.visitCode();
        return mv;
    }

    protected static void finish(MethodVisitor mv) {
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    protected static void generateMethod(ClassVisitor cv, int access, String name, String desc, ToIntFunction<MethodVisitor> codeGenerator) {
        MethodVisitor mv = beginMethod(cv, access, name, desc);
        mv.visitInsn(codeGenerator.applyAsInt(mv));
        finish(mv);
    }

    protected static void generatePassthroughCtor(ClassVisitor cv, String superclass, Type... argumentTypes) {
        String desc = getMethodDescriptor(VOID_TYPE, argumentTypes);
        MethodVisitor mv = beginMethod(cv, ACC_PUBLIC, "<init>", desc);

        mv.visitVarInsn(ALOAD, 0);
        loadConsecutiveValuesFromLvt(mv, 1, argumentTypes);
        mv.visitMethodInsn(INVOKESPECIAL, superclass, "<init>", desc, false);
        mv.visitInsn(RETURN);

        finish(mv);
    }

    protected static void generatePassthroughCtorWithLocals(ClassVisitor cv, String internalName, String superclass, Map<String, Type> localTypes, Type... passthroughArgumentTypes) {
        MethodVisitor mv = beginMethod(cv, ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, Stream.concat(Arrays.stream(passthroughArgumentTypes), localTypes.values().stream()).toArray(Type[]::new)));

        mv.visitVarInsn(ALOAD, 0);
        int lvt = loadConsecutiveValuesFromLvt(mv, 1, passthroughArgumentTypes);
        mv.visitMethodInsn(INVOKESPECIAL, superclass, "<init>", getMethodDescriptor(VOID_TYPE, passthroughArgumentTypes), false);

        for (Map.Entry<String, Type> entry : localTypes.entrySet()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(entry.getValue().getOpcode(ILOAD), lvt);
            mv.visitFieldInsn(PUTFIELD, internalName, entry.getKey(), entry.getValue().getDescriptor());
            lvt += entry.getValue().getSize();
        }

        mv.visitInsn(RETURN);

        finish(mv);
    }

    protected static int loadConsecutiveValuesFromLvt(MethodVisitor mv, int lvt, Type... types) {
        for (Type type : types) {
            mv.visitVarInsn(type.getOpcode(ILOAD), lvt);
            lvt += type.getSize();
        }
        return lvt;
    }

    protected static void generateThrowNew(MethodVisitor mv, String exceptionType) {
        mv.visitTypeInsn(NEW, exceptionType);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, exceptionType, "<init>", getMethodDescriptor(VOID_TYPE), false);
        mv.visitInsn(ATHROW);
    }

    protected static void generateThrowNew(MethodVisitor mv, String exceptionType, String message) {
        mv.visitTypeInsn(NEW, exceptionType);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(INVOKESPECIAL, exceptionType, "<init>", getMethodDescriptor(VOID_TYPE, getType(String.class)), false);
        mv.visitInsn(ATHROW);
    }

    protected static void generateThrowNew(MethodVisitor mv, int skipOpcode, String exceptionType) {
        Label tailLbl = new Label();
        mv.visitJumpInsn(skipOpcode, tailLbl);
        generateThrowNew(mv, exceptionType);
        mv.visitLabel(tailLbl);
    }

    protected static void generateThrowNew(MethodVisitor mv, int skipOpcode, String exceptionType, String message) {
        Label tailLbl = new Label();
        mv.visitJumpInsn(skipOpcode, tailLbl);
        generateThrowNew(mv, exceptionType, message);
        mv.visitLabel(tailLbl);
    }
}
