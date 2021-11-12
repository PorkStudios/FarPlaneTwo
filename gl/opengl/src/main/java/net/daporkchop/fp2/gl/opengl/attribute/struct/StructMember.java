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
import net.daporkchop.fp2.gl.attribute.Attrib;
import net.daporkchop.lib.common.util.PorkUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public class StructMember<T> {
    //what have i done
    //dear god this is horrible

    protected final Class<T> clazz;

    protected final Stage packedStage;
    protected final Stage unpackedStage;

    public StructMember(@NonNull Class<T> clazz, @NonNull Attrib attrib, @NonNull List<Field> fields) {
        this.clazz = clazz;

        switch (attrib.transform()) {
            case UNCHANGED:
                if (fields.size() == 1) {
                    this.packedStage = new ScalarInputStage(fields.get(0));
                } else {
                    this.packedStage = new VectorInputStage(fields.toArray(new Field[0]));
                }
                break;
            case INT_ARGB8_TO_BYTE_VECTOR_RGB:
            case INT_ARGB8_TO_BYTE_VECTOR_RGBA:
                checkArg(fields.size() == 1, "%s requires exactly one field, but got %d", attrib.transform(), fields.size());
                this.packedStage = new IntARGB8ToByteVectorInputStage(fields.get(0), attrib.transform() == Attrib.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGBA);
                break;
            case ARRAY_TO_MAT4x4:
                checkArg(fields.size() == 1, "%s requires exactly one field, but got %d", attrib.transform(), fields.size());
                this.packedStage = new MatrixInputStage(fields.get(0), 4);
                break;
            default:
                throw new UnsupportedOperationException(attrib.transform().toString());
        }

        Stage prevStage = this.packedStage;
        for (Attrib.Conversion conversion : attrib.convert()) {
            switch (conversion) {
                case TO_UNSIGNED:
                    prevStage = new ToUnsignedConversionStage(prevStage);
                    break;
                case TO_FLOAT:
                    prevStage = new ToFloatConversionStage(prevStage);
                    break;
                case TO_NORMALIZED_FLOAT:
                    prevStage = new ToNormalizedFloatConversionStage(prevStage);
                    break;
                default:
                    throw new UnsupportedOperationException(conversion.toString());
            }
        }
        if (prevStage.componentType().integer()) { //we have to extend other integer types to int
            prevStage = new ToIntConversionStage(prevStage);
        }
        this.unpackedStage = prevStage;
    }

    public void storeStageOutput(@NonNull MethodVisitor mv, @NonNull Stage stage, int structLvtIndex, int outputBaseLvtIndex, int outputOffsetLvtIndex) {
        stage.preLoadComponents(mv, structLvtIndex);

        for (int componentIndex = 0; componentIndex < stage.components(); componentIndex++) {
            mv.visitVarInsn(ALOAD, outputBaseLvtIndex);
            mv.visitVarInsn(LLOAD, outputOffsetLvtIndex);

            stage.loadComponent(mv, structLvtIndex, componentIndex);
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

        boolean integer();

        boolean floatingPoint();

        void arrayLoad(@NonNull MethodVisitor mv);

        void unsafePut(@NonNull MethodVisitor mv);

        void unsafeGet(@NonNull MethodVisitor mv);

        /**
         * @author DaPorkchop_
         */
        enum Int implements ComponentType {
            UNSIGNED_BYTE(1.0f / 0xFF, null) {
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
            BYTE(1.0f / 0x80, UNSIGNED_BYTE) {
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
            UNSIGNED_SHORT(1.0f / 0xFFFF, null) {
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
            SHORT(1.0f / 0x8000, UNSIGNED_SHORT) {
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
            UNSIGNED_INT(1.0f / 0xFFFFFFFFL, null) {
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
            INT(1.0f / 0x80000000L, UNSIGNED_INT) {
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

            protected final float normalizeFactor;
            protected final Int unsignedType;

            Int(float normalizeFactor, Int unsignedType) {
                this.normalizeFactor = normalizeFactor;
                this.unsignedType = PorkUtil.fallbackIfNull(unsignedType, this);
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

        void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex);

        void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex);
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ScalarInputStage implements Stage {
        @Getter
        protected final ComponentType componentType;
        protected final Field field;

        public ScalarInputStage(@NonNull Field field) {
            this.componentType = ComponentType.from(field.getType());
            this.field = field;
        }

        @Override
        public int components() {
            return 1;
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            //no-op
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            checkIndex(1, componentIndex);

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class VectorInputStage implements Stage {
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
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            //no-op
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            Field field = this.fields[componentIndex];

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), Type.getDescriptor(field.getType()));
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class IntARGB8ToByteVectorInputStage implements Stage {
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
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            //no-op
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
    }

    /**
     * @author DaPorkchop_
     */
    protected static class MatrixInputStage implements Stage {
        @Getter
        protected final ComponentType componentType;
        protected final Field field;

        @Getter
        protected final int components;

        public MatrixInputStage(@NonNull Field field, int size) {
            checkArg(field.getType().isArray(), "not an array: %s", field);

            this.field = field;
            this.componentType = ComponentType.from(field.getType().getComponentType());
            this.components = multiplyExact(positive(size, "size"), size);
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            Label label = new Label();

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
            mv.visitInsn(ARRAYLENGTH);
            mv.visitLdcInsn(this.components);
            mv.visitJumpInsn(IF_ICMPEQ, label);

            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn(this.field + ": array length must be " + this.components);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);

            mv.visitLabel(label);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            checkIndex(this.components, componentIndex);

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(this.field.getDeclaringClass()), this.field.getName(), Type.getDescriptor(this.field.getType()));
            mv.visitLdcInsn(componentIndex);
            this.componentType.arrayLoad(mv);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToUnsignedConversionStage implements Stage {
        protected final Stage prev;

        @Getter
        protected final ComponentType.Int componentType;
        protected final ComponentType.Int prevComponentType;

        public ToUnsignedConversionStage(@NonNull Stage prev) {
            this.prev = prev;

            this.prevComponentType = (ComponentType.Int) prev.componentType();
            this.componentType = this.prevComponentType.unsignedType;
        }

        @Override
        public int components() {
            return this.prev.components();
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            this.prev.preLoadComponents(mv, structLvtIndex);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            this.prev.loadComponent(mv, structLvtIndex, componentIndex);
            this.prevComponentType.makeUnsigned(mv);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToFloatConversionStage implements Stage {
        protected final Stage prev;

        protected final ComponentType.Int prevComponentType;

        public ToFloatConversionStage(@NonNull Stage prev) {
            this.prev = prev;

            checkArg(!prev.componentType().floatingPoint(), "already a floating-point type: %s", prev);
            this.prevComponentType = (ComponentType.Int) prev.componentType();
        }

        @Override
        public ComponentType componentType() {
            return ComponentType.Floating.FLOAT;
        }

        @Override
        public int components() {
            return this.prev.components();
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            this.prev.preLoadComponents(mv, structLvtIndex);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            this.prev.loadComponent(mv, structLvtIndex, componentIndex);
            this.prevComponentType.makeFloat(mv);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToNormalizedFloatConversionStage implements Stage {
        protected final Stage prev;

        protected final ComponentType.Int prevComponentType;

        public ToNormalizedFloatConversionStage(@NonNull Stage prev) {
            this.prev = prev;

            checkArg(!prev.componentType().floatingPoint(), "already a floating-point type: %s", prev);
            this.prevComponentType = (ComponentType.Int) prev.componentType();
        }

        @Override
        public ComponentType componentType() {
            return ComponentType.Floating.FLOAT;
        }

        @Override
        public int components() {
            return this.prev.components();
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            this.prev.preLoadComponents(mv, structLvtIndex);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            this.prev.loadComponent(mv, structLvtIndex, componentIndex);
            this.prevComponentType.makeNormalizedFloat(mv);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class ToIntConversionStage implements Stage {
        protected final Stage prev;

        @Getter
        protected final ComponentType.Int componentType;
        protected final ComponentType.Int prevComponentType;

        public ToIntConversionStage(@NonNull Stage prev) {
            this.prev = prev;

            this.prevComponentType = (ComponentType.Int) prev.componentType();
            this.componentType = this.prevComponentType.unsignedType == this.prevComponentType
                    ? ComponentType.Int.UNSIGNED_INT
                    : ComponentType.Int.INT;
        }

        @Override
        public int components() {
            return this.prev.components();
        }

        @Override
        public void preLoadComponents(@NonNull MethodVisitor mv, int structLvtIndex) {
            this.prev.preLoadComponents(mv, structLvtIndex);
        }

        @Override
        public void loadComponent(@NonNull MethodVisitor mv, int structLvtIndex, int componentIndex) {
            this.prev.loadComponent(mv, structLvtIndex, componentIndex);
            this.prevComponentType.makeInt(mv);
        }
    }
}
