/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.lib.common.util.PorkUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.List;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public class StructMember<S> {
    //what have i done
    //dear god this is horrible

    protected final Class<S> clazz;
    protected final String name;

    protected final Stage packedStage;
    protected final Stage unpackedStage;

    public StructMember(@NonNull Class<S> clazz, @NonNull String name, @NonNull Attribute attribute, @NonNull List<Field> fields) {
        this.clazz = clazz;
        this.name = name;

        Stage packedStage;
        switch (attribute.transform()) {
            case UNCHANGED:
                packedStage = new VectorInputStage(fields.toArray(new Field[0]));
                break;
            case INT_ARGB8_TO_BYTE_VECTOR_RGB:
            case INT_ARGB8_TO_BYTE_VECTOR_RGBA:
                checkArg(fields.size() == 1, "%s requires exactly one field, but got %s", attribute.transform(), fields);
                packedStage = new IntARGB8ToByteVectorInputStage(fields.get(0), attribute.transform() == Attribute.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGBA);
                break;
            case ARRAY_TO_MATRIX: {
                checkArg(fields.size() == 1, "%s requires exactly one field, but got %s", attribute.transform(), fields);

                Attribute.MatrixDimension matrixDimension = attribute.matrixDimension();
                @SuppressWarnings("deprecation")
                boolean _default = matrixDimension._default();
                checkArg(!_default, "matrixDimension must be set!");

                packedStage = new MatrixInputStage(fields.get(0), matrixDimension.columns(), matrixDimension.rows());
                break;
            }
            default:
                throw new UnsupportedOperationException(attribute.transform().toString());
        }

        Stage prevStage = packedStage;
        for (Attribute.Conversion conversion : attribute.convert()) {
            switch (conversion) {
                case TO_UNSIGNED:
                    if (packedStage == prevStage) {
                        packedStage = prevStage = new ToUnsignedConversionStage(prevStage);
                    } else {
                        prevStage = new ToUnsignedConversionStage(prevStage);
                    }
                    break;
                case TO_FLOAT:
                    prevStage = new ToFloatConversionStage(prevStage, false);
                    break;
                case TO_NORMALIZED_FLOAT:
                    prevStage = new ToFloatConversionStage(prevStage, true);
                    break;
                default:
                    throw new UnsupportedOperationException(conversion.toString());
            }
        }
        if (prevStage.componentType().integer()) { //we have to extend other integer types to int
            prevStage = new ToIntConversionStage(prevStage);
        }

        this.packedStage = packedStage;
        this.unpackedStage = prevStage;
    }

    public void storeStageOutput(@NonNull MethodVisitor mv, @NonNull Stage stage, int structLvtIndex, int outputBaseLvtIndex, int outputOffsetLvtIndex, long outputOffset) {
        stage.preLoadComponents(mv, structLvtIndex);

        for (int componentIndex = 0; componentIndex < stage.components(); componentIndex++) {
            mv.visitVarInsn(ALOAD, outputBaseLvtIndex);
            mv.visitVarInsn(LLOAD, outputOffsetLvtIndex);
            mv.visitLdcInsn(componentIndex * (long) stage.componentType().stride() + outputOffset);
            mv.visitInsn(LADD);

            stage.loadComponent(mv, structLvtIndex, componentIndex);
            stage.componentType().unsafePut(mv);
        }
    }

    public void copyStageOutput(@NonNull MethodVisitor mv, @NonNull Stage stage, int srcBaseLvtIndex, int srcOffsetLvtIndex, int dstBaseLvtIndex, int dstOffsetLvtIndex, long outputOffset) {
        for (int componentIndex = 0; componentIndex < stage.components(); componentIndex++) {
            mv.visitVarInsn(ALOAD, dstBaseLvtIndex);
            mv.visitVarInsn(LLOAD, dstOffsetLvtIndex);
            mv.visitLdcInsn(componentIndex * (long) stage.componentType().stride() + outputOffset);
            mv.visitInsn(LADD);

            mv.visitVarInsn(ALOAD, srcBaseLvtIndex);
            mv.visitVarInsn(LLOAD, srcOffsetLvtIndex);
            mv.visitLdcInsn(componentIndex * (long) stage.componentType().stride() + outputOffset);
            mv.visitInsn(LADD);

            stage.componentType().unsafeGet(mv);
            stage.componentType().unsafePut(mv);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected interface ComponentType {
        static ComponentType from(@NonNull Class<?> clazz) {
            checkArg(clazz.isPrimitive(), "not a primitive type: %s", clazz);

            if (clazz == byte.class) {
                return Int.BYTE;
            } else if (clazz == short.class) {
                return Int.SHORT;
            } else if (clazz == int.class) {
                return Int.INT;
            } else if (clazz == float.class) {
                return Floating.FLOAT;
            } else {
                throw new IllegalArgumentException("don't know how to handle " + clazz);
            }
        }

        int stride();

        boolean integer();

        boolean floatingPoint();

        GLSLPrimitiveType glslPrimitive();

        void arrayLoad(@NonNull MethodVisitor mv);

        void unsafePut(@NonNull MethodVisitor mv);

        void unsafeGet(@NonNull MethodVisitor mv);

        /**
         * @author DaPorkchop_
         */
        enum Int implements ComponentType {
            UNSIGNED_BYTE(BYTE_SIZE, 1.0f / 0xFF, null, GLSLPrimitiveType.INVALID) {
                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    BYTE.arrayLoad(mv);
                    BYTE.makeUnsigned(mv);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    BYTE.unsafePut(mv);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    BYTE.unsafeGet(mv);
                    BYTE.makeUnsigned(mv);
                }
            },
            BYTE(BYTE_SIZE, 1.0f / 0x80, UNSIGNED_BYTE, GLSLPrimitiveType.INVALID) {
                @Override
                public void makeUnsigned(@NonNull MethodVisitor mv) {
                    mv.visitLdcInsn(0xFF);
                    mv.visitInsn(IAND);
                }

                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    mv.visitInsn(BALOAD);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    mv.visitInsn(I2B);
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putByte", "(Ljava/lang/Object;JB)V", false);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getByte", "(Ljava/lang/Object;J)B", false);
                }
            },
            UNSIGNED_SHORT(SHORT_SIZE, 1.0f / 0xFFFF, null, GLSLPrimitiveType.INVALID) {
                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    SHORT.arrayLoad(mv);
                    SHORT.makeUnsigned(mv);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    SHORT.unsafePut(mv);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    SHORT.unsafeGet(mv);
                    SHORT.makeUnsigned(mv);
                }
            },
            SHORT(SHORT_SIZE, 1.0f / 0x8000, UNSIGNED_SHORT, GLSLPrimitiveType.INVALID) {
                @Override
                public void makeUnsigned(@NonNull MethodVisitor mv) {
                    mv.visitLdcInsn(0xFFFF);
                    mv.visitInsn(IAND);
                }

                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    mv.visitInsn(SALOAD);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    mv.visitInsn(I2S);
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putShort", "(Ljava/lang/Object;JS)V", false);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getShort", "(Ljava/lang/Object;J)S", false);
                }
            },
            UNSIGNED_INT(INT_SIZE, 1.0f / 0xFFFFFFFFL, null, GLSLPrimitiveType.UINT) {
                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    INT.arrayLoad(mv);
                    INT.makeUnsigned(mv);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    INT.unsafePut(mv);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    INT.unsafeGet(mv);
                    INT.makeUnsigned(mv);
                }
            },
            INT(INT_SIZE, 1.0f / 0x80000000L, UNSIGNED_INT, GLSLPrimitiveType.INT) {
                @Override
                public void makeUnsigned(@NonNull MethodVisitor mv) {
                    //no-op, we can't make this unsigned...
                }

                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    mv.visitInsn(IALOAD);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putInt", "(Ljava/lang/Object;JI)V", false);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getInt", "(Ljava/lang/Object;J)I", false);
                }
            };

            @Getter
            protected final int stride;
            protected final float normalizeFactor;
            protected final Int unsignedType;

            @Getter
            protected final GLSLPrimitiveType glslPrimitive;

            Int(int stride, float normalizeFactor, Int unsignedType, @NonNull GLSLPrimitiveType glslPrimitive) {
                this.stride = stride;
                this.normalizeFactor = normalizeFactor;
                this.unsignedType = PorkUtil.fallbackIfNull(unsignedType, this);
                this.glslPrimitive = glslPrimitive;
            }

            public void makeInt(@NonNull MethodVisitor mv) {
                //no-op
            }

            public void makeUnsigned(@NonNull MethodVisitor mv) {
                throw new UnsupportedOperationException(this.toString());
            }

            public void makeFloat(@NonNull MethodVisitor mv) {
                mv.visitInsn(I2F);
            }

            public void makeNormalizedFloat(@NonNull MethodVisitor mv) {
                mv.visitInsn(I2F);
                mv.visitLdcInsn(this.normalizeFactor);
                mv.visitInsn(FMUL);
            }

            public boolean unsigned() {
                return this.unsignedType == this;
            }

            @Override
            public boolean integer() {
                return true;
            }

            @Override
            public boolean floatingPoint() {
                return false;
            }
        }

        /**
         * @author DaPorkchop_
         */
        enum Floating implements ComponentType {
            FLOAT {
                @Override
                public int stride() {
                    return FLOAT_SIZE;
                }

                @Override
                public GLSLPrimitiveType glslPrimitive() {
                    return GLSLPrimitiveType.FLOAT;
                }

                @Override
                public void arrayLoad(@NonNull MethodVisitor mv) {
                    mv.visitInsn(FALOAD);
                }

                @Override
                public void unsafePut(@NonNull MethodVisitor mv) {
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putFloat", "(Ljava/lang/Object;JF)V", false);
                }

                @Override
                public void unsafeGet(@NonNull MethodVisitor mv) {
                    mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getFloat", "(Ljava/lang/Object;J)F", false);
                }
            };

            @Override
            public boolean integer() {
                return false;
            }

            @Override
            public boolean floatingPoint() {
                return true;
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected interface Stage {
        ComponentType componentType();

        int components();

        boolean isNormalizedFloat();

        GLSLType glslType();

        void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex);

        void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex);

        void cloneStruct(@NonNull MethodVisitor mv, int srcStructLvtIndex, int dstStructLvtIndex);
    }

    /**
     * @author DaPorkchop_
     */
    protected static abstract class InputStage implements Stage {
        @Override
        public boolean isNormalizedFloat() {
            return false;
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            //no-op
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class VectorInputStage extends InputStage {
        @Getter
        protected final ComponentType componentType;
        protected final Field[] fields;

        public VectorInputStage(@NonNull Field[] fields) {
            Class<?> type = fields[0].getType();
            for (Field field : fields) {
                checkArg(field.getType() == type, "mismatched type: %s (expected %s)", field, type);
            }

            this.componentType = ComponentType.from(type);
            this.fields = fields;
        }

        @Override
        public int components() {
            return this.fields.length;
        }

        @Override
        public GLSLType glslType() {
            return GLSLType.vec(this.componentType.glslPrimitive(), this.components());
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            Field field = this.fields[componentIndex];

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(field.getType()));
        }

        @Override
        public void cloneStruct(@NonNull MethodVisitor mv, int srcStructLvtIndex, int dstStructLvtIndex) {
            for (Field field : this.fields) {
                mv.visitVarInsn(ALOAD, dstStructLvtIndex);
                mv.visitVarInsn(ALOAD, srcStructLvtIndex);
                mv.visitFieldInsn(GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(field.getType()));
                mv.visitFieldInsn(PUTFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(field.getType()));
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class IntARGB8ToByteVectorInputStage extends InputStage {
        protected final Field field;
        protected final boolean alpha;

        public IntARGB8ToByteVectorInputStage(@NonNull Field field, boolean alpha) {
            checkArg(field.getType() == int.class, "not an int: %s", field);

            this.field = field;
            this.alpha = alpha;
        }

        @Override
        public ComponentType componentType() {
            return ComponentType.Int.UNSIGNED_BYTE;
        }

        @Override
        public int components() {
            return this.alpha ? 4 : 3;
        }

        @Override
        public GLSLType glslType() {
            return GLSLType.vec(GLSLPrimitiveType.UINT, this.components());
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            checkIndex(this.components(), componentIndex);

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), "I");

            mv.visitLdcInsn((((2 - componentIndex) & 3) << 3));
            mv.visitInsn(ISHR);
            mv.visitLdcInsn(0xFF);
            mv.visitInsn(IAND);
        }

        @Override
        public void cloneStruct(@NonNull MethodVisitor mv, int srcStructLvtIndex, int dstStructLvtIndex) {
            mv.visitVarInsn(ALOAD, dstStructLvtIndex);
            mv.visitVarInsn(ALOAD, srcStructLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), "I");
            mv.visitFieldInsn(PUTFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), "I");
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class MatrixInputStage extends InputStage {
        @Getter
        protected final ComponentType componentType;
        protected final Field field;

        protected final int columns;
        protected final int rows;

        public MatrixInputStage(@NonNull Field field, int columns, int rows) {
            checkArg(field.getType().isArray(), "not an array: %s", field);
            checkArg(columns >= 2 && columns <= 4 && rows >= 2 && rows <= 4, "cannot create %dx%d matrix", columns, rows);

            this.field = field;
            this.componentType = ComponentType.from(field.getType().getComponentType());
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public int components() {
            return this.columns * this.rows;
        }

        @Override
        public GLSLType glslType() {
            return GLSLType.mat(this.componentType.glslPrimitive(), this.columns, this.rows);
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            Label label = new Label();

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
            mv.visitInsn(ARRAYLENGTH);
            mv.visitLdcInsn(this.components());
            mv.visitJumpInsn(IF_ICMPEQ, label);

            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn(this.field + ": array length must be " + this.components());
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);

            mv.visitLabel(label);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            checkIndex(this.components(), componentIndex);

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
            mv.visitLdcInsn(componentIndex);
            this.componentType.arrayLoad(mv);
        }

        @Override
        public void cloneStruct(@NonNull MethodVisitor mv, int srcStructLvtIndex, int dstStructLvtIndex) {
            mv.visitVarInsn(ALOAD, dstStructLvtIndex);
            mv.visitVarInsn(ALOAD, srcStructLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(this.field.getType()), "clone", Type.getMethodDescriptor(Type.getType(this.field.getType())), false);
            mv.visitFieldInsn(PUTFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static abstract class ConversionStage implements Stage {
        @NonNull
        protected final Stage prev;

        @Override
        public ComponentType componentType() {
            return this.prev.componentType();
        }

        @Override
        public int components() {
            return this.prev.components();
        }

        @Override
        public boolean isNormalizedFloat() {
            return this.prev.isNormalizedFloat();
        }

        @Override
        public GLSLType glslType() {
            return this.prev.glslType().withPrimitive(this.componentType().glslPrimitive());
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            this.prev.preLoadComponents(mv, structLvtIndex);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            this.prev.loadComponent(mv, structLvtIndex, componentIndex);
        }

        @Override
        public void cloneStruct(@NonNull MethodVisitor mv, int srcStructLvtIndex, int dstStructLvtIndex) {
            this.prev.cloneStruct(mv, srcStructLvtIndex, dstStructLvtIndex);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToUnsignedConversionStage extends ConversionStage {
        @Getter
        protected final ComponentType.Int componentType;
        protected final ComponentType.Int prevComponentType;

        public ToUnsignedConversionStage(@NonNull Stage prev) {
            super(prev);

            this.prevComponentType = (ComponentType.Int) prev.componentType();
            this.componentType = this.prevComponentType.unsignedType;
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            super.loadComponent(mv, structLvtIndex, componentIndex);
            this.prevComponentType.makeUnsigned(mv);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToFloatConversionStage extends ConversionStage {
        protected final ComponentType.Int prevComponentType;
        protected final boolean normalized;

        public ToFloatConversionStage(@NonNull Stage prev, boolean normalized) {
            super(prev);

            checkArg(!prev.componentType().floatingPoint(), "already a floating-point type: %s", prev);
            this.prevComponentType = (ComponentType.Int) prev.componentType();
            this.normalized = normalized;
        }

        @Override
        public ComponentType componentType() {
            return ComponentType.Floating.FLOAT;
        }

        @Override
        public boolean isNormalizedFloat() {
            return this.normalized || super.isNormalizedFloat();
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            super.loadComponent(mv, structLvtIndex, componentIndex);

            if (this.normalized) {
                this.prevComponentType.makeNormalizedFloat(mv);
            } else {
                this.prevComponentType.makeFloat(mv);
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToIntConversionStage extends ConversionStage {
        @Getter
        protected final ComponentType.Int componentType;
        protected final ComponentType.Int prevComponentType;

        public ToIntConversionStage(@NonNull Stage prev) {
            super(prev);

            this.prevComponentType = (ComponentType.Int) prev.componentType();
            this.componentType = this.prevComponentType.unsignedType == this.prevComponentType
                    ? ComponentType.Int.UNSIGNED_INT
                    : ComponentType.Int.INT;
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            super.loadComponent(mv, structLvtIndex, componentIndex);
            this.prevComponentType.makeInt(mv);
        }
    }
}
