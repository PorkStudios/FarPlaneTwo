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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import org.objectweb.asm.MethodVisitor;

import java.util.IdentityHashMap;
import java.util.Map;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum ComponentType {
    UNSIGNED_BYTE(BYTE_SIZE, true, false, 1 << Byte.SIZE, byte.class, GLSLPrimitiveType.UINT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ILOAD, lvtIndex);
            this.truncateInteger(mv);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            this.truncateInteger(mv);
            mv.visitVarInsn(ISTORE, lvtIndex);
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            BYTE.arrayLoad(mv);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            BYTE.arrayStore(mv);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            BYTE.unsafeGet(mv);
            this.truncateInteger(mv);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            BYTE.unsafePut(mv);
        }

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            mv.visitLdcInsn(0xFF);
            mv.visitInsn(IAND);
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            this.truncateInteger(mv);
            mv.visitInsn(I2F);
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(F2I);
            this.truncateInteger(mv);
        }
    },
    BYTE(BYTE_SIZE, true, true, -Byte.MIN_VALUE, byte.class, GLSLPrimitiveType.INT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ILOAD, lvtIndex);
            this.truncateInteger(mv);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            this.truncateInteger(mv);
            mv.visitVarInsn(ISTORE, lvtIndex);
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(BALOAD);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            mv.visitInsn(BASTORE);
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

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2B);
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            this.truncateInteger(mv);
            mv.visitInsn(I2F);
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(F2I);
            this.truncateInteger(mv);
        }
    },
    UNSIGNED_SHORT(SHORT_SIZE, true, false, 1 << Short.SIZE, short.class, GLSLPrimitiveType.UINT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ILOAD, lvtIndex);
            this.truncateInteger(mv);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            this.truncateInteger(mv);
            mv.visitVarInsn(ISTORE, lvtIndex);
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(CALOAD);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            mv.visitInsn(CASTORE);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getChar", "(Ljava/lang/Object;J)C", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2C);
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putChar", "(Ljava/lang/Object;JC)V", false);
        }

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2C);
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            this.truncateInteger(mv);
            mv.visitInsn(I2F);
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(F2I);
            this.truncateInteger(mv);
        }
    },
    SHORT(SHORT_SIZE, true, true, -Short.MIN_VALUE, short.class, GLSLPrimitiveType.INT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ILOAD, lvtIndex);
            this.truncateInteger(mv);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            this.truncateInteger(mv);
            mv.visitVarInsn(ISTORE, lvtIndex);
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(SALOAD);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            mv.visitInsn(SASTORE);
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

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2S);
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            this.truncateInteger(mv);
            mv.visitInsn(I2F);
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(F2I);
            this.truncateInteger(mv);
        }
    },
    UNSIGNED_INT(INT_SIZE, true, false, (float) (1L << (long) Integer.SIZE), int.class, GLSLPrimitiveType.UINT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ILOAD, lvtIndex);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ISTORE, lvtIndex);
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            INT.arrayLoad(mv);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            INT.arrayStore(mv);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            INT.unsafeGet(mv);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            INT.unsafePut(mv);
        }

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            //no-op
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            //unsigned integers need special handling because java doesn't support them...
            mv.visitInsn(I2L);
            mv.visitLdcInsn(0xFFFFFFFFL);
            mv.visitInsn(LAND);
            mv.visitInsn(L2F);
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(F2L);
            mv.visitLdcInsn(0xFFFFFFFFL);
            mv.visitInsn(LAND);
            mv.visitInsn(L2I);
        }
    },
    INT(INT_SIZE, true, true, -((float) Integer.MIN_VALUE), int.class, GLSLPrimitiveType.INT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ILOAD, lvtIndex);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(ISTORE, lvtIndex);
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(IALOAD);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            mv.visitInsn(IASTORE);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getInt", "(Ljava/lang/Object;J)I", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putInt", "(Ljava/lang/Object;JI)V", false);
        }

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            //no-op
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(I2F);
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            mv.visitInsn(F2I);
        }
    },
    FLOAT(FLOAT_SIZE, false, true, Float.NaN, float.class, GLSLPrimitiveType.FLOAT) {
        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(FLOAD, lvtIndex);
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndex) {
            mv.visitVarInsn(FSTORE, lvtIndex);
        }

        @Override
        public float inverseNormalizationFactor() {
            throw new UnsupportedOperationException("normalize float");
        }

        @Override
        public void arrayLoad(@NonNull MethodVisitor mv) {
            mv.visitInsn(FALOAD);
        }

        @Override
        public void arrayStore(@NonNull MethodVisitor mv) {
            mv.visitInsn(FASTORE);
        }

        @Override
        public void unsafeGet(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getFloat", "(Ljava/lang/Object;J)F", false);
        }

        @Override
        public void unsafePut(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putFloat", "(Ljava/lang/Object;JF)V", false);
        }

        @Override
        public void truncateInteger(@NonNull MethodVisitor mv) {
            throw new UnsupportedOperationException("truncate float");
        }

        @Override
        public void convertToFloat(@NonNull MethodVisitor mv) {
            throw new UnsupportedOperationException("convert float -> float");
        }

        @Override
        public void convertFromFloat(@NonNull MethodVisitor mv) {
            throw new UnsupportedOperationException("convert float -> float");
        }
    };

    private static final Map<Class<?>, ComponentType> PRIMITIVE_CLASSES_TO_COMPONENT_TYPES = new IdentityHashMap<>();

    static {
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(byte.class, BYTE);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(char.class, UNSIGNED_SHORT);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(short.class, SHORT);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(int.class, INT);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(float.class, FLOAT);
    }

    public static ComponentType from(@NonNull Class<?> clazz) {
        checkArg(clazz.isPrimitive(), "not a primitive type: %s", clazz);

        ComponentType componentType = PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.get(clazz);
        if (componentType != null) {
            return componentType;
        }

        throw new IllegalArgumentException("don't know how to handle " + clazz);
    }

    private final int size;
    private final boolean integer;
    private final boolean signed;

    private final float inverseNormalizationFactor;

    @NonNull
    private final Class<?> javaPrimitive;
    @NonNull
    private final GLSLPrimitiveType glslPrimitive;

    public float normalizationFactor() {
        return 1.0f / this.inverseNormalizationFactor();
    }

    public ComponentType toUnsigned() {
        assert this.integer() : "not an integer type!";
        assert this.signed() : "not a signed type!";

        return values()[this.ordinal() & ~1];
    }

    public abstract void load(@NonNull MethodVisitor mv, int lvtIndex);

    public abstract void store(@NonNull MethodVisitor mv, int lvtIndex);

    public abstract void arrayLoad(@NonNull MethodVisitor mv);

    public abstract void arrayStore(@NonNull MethodVisitor mv);

    public abstract void unsafeGet(@NonNull MethodVisitor mv);

    public abstract void unsafePut(@NonNull MethodVisitor mv);

    public abstract void truncateInteger(@NonNull MethodVisitor mv);

    public abstract void convertToFloat(@NonNull MethodVisitor mv);

    public abstract void convertFromFloat(@NonNull MethodVisitor mv);
}
