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

package net.daporkchop.fp2.gl.opengl.attribute;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeInterpretation;
import net.daporkchop.fp2.gl.attribute.AttributeType;
import net.daporkchop.lib.common.misc.string.PStrings;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class AttributeGenerator {
    protected final LoadingCache<Key, Class<?>> CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(CacheLoader.from(AttributeGenerator::generate));

    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public <A extends AttributeImpl> A get(@NonNull AttributeBuilderImpl builder) {
        Class<A> clazz = uncheckedCast(CACHE.getUnchecked(new Key(builder.type, builder.interpretation, builder.components)));
        Constructor<A> constructor = clazz.getDeclaredConstructor(AttributeBuilderImpl.class);
        constructor.setAccessible(true);
        return constructor.newInstance(builder);
    }

    private Class<?> generate(@NonNull Key key) {
        AttributeType type = key.type;
        AttributeInterpretation interpretation = key.interpretation;
        int components = key.components;

        int interpretationSize;
        switch (interpretation) {
            case INTEGER:
                interpretationSize = INT_SIZE;
                break;
            case FLOAT:
            case NORMALIZED_FLOAT:
                interpretationSize = FLOAT_SIZE;
                break;
            default:
                throw new IllegalArgumentException(interpretation.toString());
        }

        Class<?> baseClass = AttributeImpl.class;
        String baseClassName = Type.getInternalName(baseClass);
        String className = baseClassName + '$' + type + '_' + interpretation + '_' + components;

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        writer.visit(V1_8, ACC_FINAL, className, null, baseClassName, new String[]{
                Type.getInternalName(Attribute.class) + '$' + (type instanceof AttributeType.Integer ? "Int" : null) + components
        });

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", "(Lnet/daporkchop/fp2/gl/opengl/attribute/AttributeBuilderImpl;)V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", "(Lnet/daporkchop/fp2/gl/opengl/attribute/AttributeBuilderImpl;)V", false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //int components()
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "components", "()I", null, null);
            mv.visitCode();

            mv.visitLdcInsn(components);
            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //int packedSize()
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "packedSize", "()I", null, null);
            mv.visitCode();

            mv.visitLdcInsn(type.size(components));
            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //int unpackedSize()
            int componentCount;
            switch (components) {
                case 1:
                case 2:
                case 4:
                    componentCount = components;
                    break;
                case 3:
                    componentCount = 4;
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(components));
            }

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "unpackedSize", "()I", null, null);
            mv.visitCode();

            mv.visitLdcInsn(interpretationSize * componentCount);
            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        if (type instanceof AttributeType.Integer) {
            StringBuilder builder = new StringBuilder();
            builder.append("(Ljava/lang/Object;J");
            PStrings.appendMany(builder, 'I', components);
            builder.append(")V");

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "set", builder.toString(), null, null);
            mv.visitCode();

            for (int i = 0; i < components; i++) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(LLOAD, 2);
                mv.visitLdcInsn(i * (long) type.size());
                mv.visitInsn(LADD);
                mv.visitVarInsn(ILOAD, 4 + i);

                switch ((AttributeType.Integer) type) {
                    case BYTE:
                    case UNSIGNED_BYTE:
                        mv.visitInsn(I2B);
                        mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putByte", "(Ljava/lang/Object;JB)V", false);
                        break;
                    case SHORT:
                    case UNSIGNED_SHORT:
                        mv.visitInsn(I2S);
                        mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putShort", "(Ljava/lang/Object;JS)V", false);
                        break;
                    case INT:
                    case UNSIGNED_INT:
                        mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putInt", "(Ljava/lang/Object;JI)V", false);
                        break;
                    default:
                        throw new IllegalArgumentException(type.toString());
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void unpack(Object srcBase, long srcOffset, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "unpack", "(Ljava/lang/Object;JLjava/lang/Object;J)V", null, null);
            mv.visitCode();

            for (int i = 0; i < components; i++) {
                //dst
                mv.visitVarInsn(ALOAD, 4);
                mv.visitVarInsn(LLOAD, 5);
                mv.visitLdcInsn(i * (long) interpretationSize);
                mv.visitInsn(LADD);

                //src
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(LLOAD, 2);
                mv.visitLdcInsn(i * (long) type.size());
                mv.visitInsn(LADD);

                //load values
                if (type instanceof AttributeType.Integer) {
                    switch ((AttributeType.Integer) type) {
                        case BYTE:
                        case UNSIGNED_BYTE:
                            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getByte", "(Ljava/lang/Object;J)B", false);
                            break;
                        case SHORT:
                        case UNSIGNED_SHORT:
                            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getShort", "(Ljava/lang/Object;J)S", false);
                            break;
                        case INT:
                        case UNSIGNED_INT:
                            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getInt", "(Ljava/lang/Object;J)I", false);
                            break;
                    }
                } else if (type instanceof AttributeType.Float) {
                    switch ((AttributeType.Float) type) {
                        case FLOAT:
                            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "getFloat", "(Ljava/lang/Object;J)F", false);
                            break;
                        case DOUBLE:
                            throw new UnsupportedOperationException(type.toString());
                    }
                }

                //type conversion
                switch (interpretation) {
                    case INTEGER:
                        if (type instanceof AttributeType.Float) {
                            switch ((AttributeType.Float) type) {
                                case FLOAT:
                                    mv.visitInsn(F2I);
                                    break;
                                case DOUBLE:
                                    mv.visitInsn(D2I);
                                    break;
                            }
                        }
                        break;
                    case FLOAT:
                        if (type instanceof AttributeType.Integer) {
                            mv.visitInsn(I2F);
                        }
                        break;
                    case NORMALIZED_FLOAT:
                        if (type instanceof AttributeType.Integer) {
                            mv.visitInsn(I2F);

                            double factor;
                            switch ((AttributeType.Integer) type) {
                                case BYTE:
                                    factor = -Byte.MIN_VALUE;
                                    break;
                                case UNSIGNED_BYTE:
                                    factor = (1 << Byte.SIZE) - 1;
                                    break;
                                case SHORT:
                                    factor = -Short.MIN_VALUE;
                                    break;
                                case UNSIGNED_SHORT:
                                    factor = (1 << Short.SIZE) - 1;
                                    break;
                                case INT:
                                    factor = -((long) Integer.MIN_VALUE);
                                    break;
                                case UNSIGNED_INT:
                                    factor = (1L << (long) Integer.SIZE) - 1L;
                                    break;
                                default:
                                    throw new IllegalArgumentException(type.toString());
                            }

                            mv.visitLdcInsn((float) (1.0d / factor));
                            mv.visitInsn(FMUL);
                        }
                        break;
                }

                //store values
                switch (interpretation) {
                    case INTEGER:
                        mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putInt", "(Ljava/lang/Object;JI)V", false);
                        break;
                    case FLOAT:
                    case NORMALIZED_FLOAT:
                        mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "putFloat", "(Ljava/lang/Object;JF)V", false);
                        break;
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return ClassloadingUtils.defineHiddenClass(baseClass.getClassLoader(), writer.toByteArray());
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static final class Key {
        @NonNull
        protected final AttributeType type;
        @NonNull
        protected final AttributeInterpretation interpretation;
        protected final int components;
    }
}
