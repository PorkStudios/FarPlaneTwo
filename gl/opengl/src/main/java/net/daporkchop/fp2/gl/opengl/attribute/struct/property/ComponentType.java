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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum ComponentType {
    UNSIGNED_BYTE(BYTE_SIZE, true, false, byte.class, GLSLPrimitiveType.UINT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            BYTE.arrayLoad(mv);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            BYTE.unsafeGet(mv);
            mv.visitLdcInsn(0xFF);
            mv.visitInsn(IAND);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            BYTE.unsafePut(mv);
        }
    },
    BYTE(BYTE_SIZE, true, true, byte.class, GLSLPrimitiveType.INT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(BALOAD);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getByte", "(Ljava/lang/Object;J)B", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2B);
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putByte", "(Ljava/lang/Object;JB)V", false);
        }
    },
    UNSIGNED_SHORT(SHORT_SIZE, true, false, short.class, GLSLPrimitiveType.UINT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            SHORT.arrayLoad(mv);
            mv.visitLdcInsn(0xFFFF);
            mv.visitInsn(IAND);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            SHORT.unsafeGet(mv);
            mv.visitLdcInsn(0xFFFF);
            mv.visitInsn(IAND);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            SHORT.unsafePut(mv);
        }
    },
    SHORT(SHORT_SIZE, true, true, short.class, GLSLPrimitiveType.INT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(SALOAD);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getShort", "(Ljava/lang/Object;J)S", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2S);
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putShort", "(Ljava/lang/Object;JS)V", false);
        }
    },
    UNSIGNED_INT(INT_SIZE, true, false, int.class, GLSLPrimitiveType.UINT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            INT.arrayLoad(mv);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            INT.unsafeGet(mv);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            INT.unsafePut(mv);
        }
    },
    INT(INT_SIZE, true, true, int.class, GLSLPrimitiveType.INT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(IALOAD);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getInt", "(Ljava/lang/Object;J)I", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putInt", "(Ljava/lang/Object;JI)V", false);
        }
    },
    FLOAT(FLOAT_SIZE, false, true, float.class, GLSLPrimitiveType.FLOAT) {
        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(FALOAD);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getFloat", "(Ljava/lang/Object;J)F", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putFloat", "(Ljava/lang/Object;JF)V", false);
        }
    };

    public static ComponentType from(@NonNull Class<?> clazz) {
        checkArg(clazz.isPrimitive(), "not a primitive type: %s", clazz);

        for (ComponentType type : values()) {
            if (type.signed() && type.javaPrimitive() == clazz) {
                return type;
            }
        }
        throw new IllegalArgumentException("don't know how to handle " + clazz);
    }

    private final int size;
    private final boolean integer;
    private final boolean signed;

    @NonNull
    private final Class<?> javaPrimitive;
    @NonNull
    private final GLSLPrimitiveType glslPrimitive;

    public abstract void arrayLoad(@NonNull MethodVisitor mv);

    public abstract void unsafeGet(@NonNull MethodVisitor mv);

    public abstract void unsafePut(@NonNull MethodVisitor mv);
}
