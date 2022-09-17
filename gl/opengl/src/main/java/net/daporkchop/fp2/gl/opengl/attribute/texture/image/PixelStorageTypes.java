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

package net.daporkchop.fp2.gl.opengl.attribute.texture.image;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@SuppressWarnings("UnstableApiUsage")
@UtilityClass
public class PixelStorageTypes {
    public static final ImmutableList<PixelStorageType> BYTE = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_BYTE, PixelFormatChannelType.INTEGER, BYTE_TYPE, BYTE_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> UNSIGNED_BYTE = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_UNSIGNED_BYTE, PixelFormatChannelType.UNSIGNED_INTEGER, BYTE_TYPE, BYTE_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> SHORT = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_SHORT, PixelFormatChannelType.INTEGER, SHORT_TYPE, SHORT_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> UNSIGNED_SHORT = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_UNSIGNED_SHORT, PixelFormatChannelType.UNSIGNED_INTEGER, CHAR_TYPE, CHAR_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> INT = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_INT, PixelFormatChannelType.INTEGER, INT_TYPE, INT_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> UNSIGNED_INT = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_UNSIGNED_INT, PixelFormatChannelType.UNSIGNED_INTEGER, INT_TYPE, INT_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> FLOAT = IntStream.rangeClosed(1, 4)
            .mapToObj(components -> new AbstractPrimitiveStorageType(GL_FLOAT, PixelFormatChannelType.FLOATING_POINT, FLOAT_TYPE, FLOAT_SIZE, components))
            .collect(ImmutableList.toImmutableList());

    public static final ImmutableList<PixelStorageType> ALL = Stream.of(BYTE, UNSIGNED_BYTE, SHORT, UNSIGNED_SHORT, INT, UNSIGNED_INT, FLOAT)
            .flatMap(List::stream)
            .collect(ImmutableList.toImmutableList());

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static class AbstractPrimitiveStorageType implements PixelStorageType {
        private final int glType;
        private final PixelFormatChannelType genericType;
        private final Type asmType;
        private final int size;

        private final int components;

        @Getter(lazy = true)
        private final ImmutableList<Integer> bitDepths = this.bitDepths0();

        protected ImmutableList<Integer> bitDepths0() {
            return ImmutableList.copyOf(PArrays.filled(this.components, Integer.class, this.size << 3));
        }

        @Override
        public void load(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
            int baseLvtIndex = lvtIndexAllocatorIn;
            lvtIndexAllocatorIn += getType(Object.class).getSize();
            int offsetLvtIndex = lvtIndexAllocatorIn;
            lvtIndexAllocatorIn += LONG_TYPE.getSize();

            mv.visitVarInsn(LSTORE, offsetLvtIndex);
            mv.visitVarInsn(ASTORE, baseLvtIndex);

            callback.withComponents(lvtIndexAllocatorIn, componentIndex -> {
                checkIndex(this.components, componentIndex);

                mv.visitVarInsn(ALOAD, baseLvtIndex);
                mv.visitVarInsn(LLOAD, offsetLvtIndex);

                if (componentIndex != 0) { //add component offset if necessary
                    mv.visitLdcInsn((long) componentIndex * this.size);
                    mv.visitInsn(LADD);
                }

                String name = this.asmType.getClassName();
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class),
                        "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1),
                        getMethodDescriptor(this.asmType, getType(Object.class), LONG_TYPE), false);

                if (this.genericType == PixelFormatChannelType.UNSIGNED_INTEGER && this.asmType != INT_TYPE) {
                    mv.visitLdcInsn((1 << (this.size << 3)) - 1);
                    mv.visitInsn(IAND);
                }
            });
        }

        @Override
        public void store(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull StoreCallback callback) {
            int baseLvtIndex = lvtIndexAllocatorIn;
            lvtIndexAllocatorIn += getType(Object.class).getSize();
            int offsetLvtIndex = lvtIndexAllocatorIn;
            lvtIndexAllocatorIn += LONG_TYPE.getSize();

            mv.visitVarInsn(LSTORE, offsetLvtIndex);
            mv.visitVarInsn(ASTORE, baseLvtIndex);

            callback.withComponents(lvtIndexAllocatorIn, componentIndex -> {
                checkIndex(this.components, componentIndex);

                mv.visitVarInsn(ALOAD, baseLvtIndex);
                mv.visitInsn(DUP_X1);
                mv.visitInsn(POP);
                mv.visitVarInsn(LLOAD, offsetLvtIndex);

                if (componentIndex != 0) { //add component offset if necessary
                    mv.visitLdcInsn((long) componentIndex * this.size);
                    mv.visitInsn(LADD);
                }

                mv.visitInsn(DUP2_X1);
                mv.visitInsn(POP2);

                //TODO: see if this is really necessary
                if (this.asmType == BYTE_TYPE) {
                    mv.visitInsn(I2B);
                } else if (this.asmType == SHORT_TYPE) {
                    mv.visitInsn(I2S);
                } else if (this.asmType == CHAR_TYPE) {
                    mv.visitInsn(I2C);
                }

                String name = this.asmType.getClassName();
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class),
                        "put" + Character.toUpperCase(name.charAt(0)) + name.substring(1),
                        getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, this.asmType), false);

                if (this.genericType == PixelFormatChannelType.UNSIGNED_INTEGER && this.asmType != INT_TYPE) {
                    mv.visitLdcInsn((1 << (this.size << 3)) - 1);
                    mv.visitInsn(IAND);
                }
            });
        }
    }
}
