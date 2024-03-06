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

package net.daporkchop.fp2.gl.codegen.struct.attribute;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.IdentityHashMap;

import static net.daporkchop.lib.common.util.PValidation.checkArg;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum JavaPrimitiveType {
    UNSIGNED_BYTE(Byte.BYTES, true, false, 1 << Byte.SIZE, byte.class, BYTE_TYPE),
    BYTE(Byte.BYTES, true, true, -((int) Byte.MIN_VALUE), byte.class, BYTE_TYPE),
    UNSIGNED_SHORT(Character.SIZE, true, false, 1 << Character.SIZE, char.class, CHAR_TYPE),
    SHORT(Short.BYTES, true, true, -((int) Short.MIN_VALUE), short.class, SHORT_TYPE),
    UNSIGNED_INT(Integer.BYTES, true, false, 1L << Integer.SIZE, int.class, INT_TYPE),
    INT(Integer.BYTES, true, true, -((long) Integer.MIN_VALUE), int.class, INT_TYPE),
    FLOAT(Float.BYTES, false, true, Float.NaN, float.class, FLOAT_TYPE),
    ;

    private static final IdentityHashMap<Class<?>, JavaPrimitiveType> PRIMITIVE_CLASSES_TO_COMPONENT_TYPES = new IdentityHashMap<>();

    static {
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(byte.class, BYTE);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(char.class, UNSIGNED_SHORT);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(short.class, SHORT);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(int.class, INT);
        PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.put(float.class, FLOAT);
    }

    public static JavaPrimitiveType from(@NonNull Class<?> clazz) {
        checkArg(clazz.isPrimitive(), "not a primitive type: %s", clazz);

        JavaPrimitiveType componentType = PRIMITIVE_CLASSES_TO_COMPONENT_TYPES.get(clazz);
        if (componentType != null) {
            return componentType;
        }

        throw new IllegalArgumentException("don't know how to handle " + clazz);
    }

    public static JavaPrimitiveType from(ShaderPrimitiveType type) {
        switch (type) {
            case UINT:
                return UNSIGNED_INT;
            case INT:
                return INT;
            case FLOAT:
                return FLOAT;
        }

        throw new IllegalArgumentException("don't know how to handle " + type);
    }

    private final int size;
    private final boolean integer;
    private final boolean signed;

    private final float inverseNormalizationFactor;

    private final Class<?> javaType;
    private final Type asmType;

    @Getter(AccessLevel.NONE)
    private transient final String unsafeGetName;
    @Getter(AccessLevel.NONE)
    private transient final String unsafePutName;
    @Getter(AccessLevel.NONE)
    private transient final String unsafeGetDescriptor;
    @Getter(AccessLevel.NONE)
    private transient final String unsafePutDescriptor;

    JavaPrimitiveType(int size, boolean integer, boolean signed, float inverseNormalizationFactor, Class<?> javaType, Type asmType) {
        this.size = size;
        this.integer = integer;
        this.signed = signed;
        this.inverseNormalizationFactor = inverseNormalizationFactor;
        this.javaType = javaType;
        this.asmType = asmType;

        String className = this.asmType.getClassName();
        className = Character.toUpperCase(className.charAt(0)) + className.substring(1);
        this.unsafeGetName = ("get" + className).intern();
        this.unsafePutName = ("put" + className).intern();
        this.unsafeGetDescriptor = getMethodDescriptor(this.asmType, getType(Object.class), LONG_TYPE).intern();
        this.unsafePutDescriptor = getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, this.asmType).intern();
    }

    public final float inverseNormalizationFactor() {
        if (!this.integer) {
            throw new UnsupportedOperationException("normalize float");
        } else {
            return this.inverseNormalizationFactor;
        }
    }

    public final float normalizationFactor() {
        return 1.0f / this.inverseNormalizationFactor();
    }

    public final JavaPrimitiveType toUnsigned() {
        checkArg(this.integer, "not an integer type");
        checkArg(this.signed, "not a signed type");
        return values()[this.ordinal() & ~1];
    }

    public final void load(MethodVisitor mv, int lvtIndex) {
        mv.visitVarInsn(this.asmType.getOpcode(ILOAD), lvtIndex);
        if (this.integer) {
            this.truncateInteger(mv);
        }
    }

    public final void store(MethodVisitor mv, int lvtIndex) {
        if (this.integer) {
            this.truncateInteger(mv);
        }
        mv.visitVarInsn(this.asmType.getOpcode(ISTORE), lvtIndex);
    }

    public final void arrayLoad(MethodVisitor mv) {
        mv.visitInsn(this.asmType.getOpcode(IALOAD));
    }

    public final void arrayStore(MethodVisitor mv) {
        mv.visitInsn(this.asmType.getOpcode(IASTORE));
    }

    public final void unsafeGet(MethodVisitor mv) {
        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), this.unsafeGetName, this.unsafeGetDescriptor, PUnsafe.class.isInterface());
    }

    public final void unsafePut(MethodVisitor mv) {
        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), this.unsafePutName, this.unsafePutDescriptor, PUnsafe.class.isInterface());
    }

    public final void truncateInteger(MethodVisitor mv) {
        if (!this.integer) {
            throw new UnsupportedOperationException("truncate float");
        }

        int opcode;
        switch (this) {
            case UNSIGNED_BYTE:
                mv.visitIntInsn(SIPUSH, 0xFF);
                opcode = IAND;
                break;
            case BYTE:
                opcode = I2B;
                break;
            case UNSIGNED_SHORT:
                opcode = I2C;
                break;
            case SHORT:
                opcode = I2S;
                break;
            case UNSIGNED_INT:
            case INT:
                return;
            default:
                throw new IllegalStateException(this.name());
        }
        mv.visitInsn(opcode);
    }

    public final void convertToFloat(MethodVisitor mv) {
        if (!this.integer) {
            throw new UnsupportedOperationException("convert float -> float");
        } else if (this == UNSIGNED_INT) { //unsigned integers need special handling because java doesn't support them...
            mv.visitInsn(I2L);
            mv.visitLdcInsn(0xFFFFFFFFL);
            mv.visitInsn(LAND);
            mv.visitInsn(L2F);
        } else {
            this.truncateInteger(mv);
            mv.visitInsn(I2F);
        }
    }

    public final void convertFromFloat(MethodVisitor mv) {
        if (!this.integer) {
            throw new UnsupportedOperationException("convert float -> float");
        } else if (this == UNSIGNED_INT) { //unsigned integers need special handling because java doesn't support them...
            mv.visitInsn(F2L);
            mv.visitLdcInsn(0xFFFFFFFFL);
            mv.visitInsn(LAND);
            mv.visitInsn(L2I);
        } else {
            mv.visitInsn(F2I);
            this.truncateInteger(mv);
        }
    }
}
