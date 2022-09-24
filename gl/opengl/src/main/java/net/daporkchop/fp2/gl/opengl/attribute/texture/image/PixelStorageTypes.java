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
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class PixelStorageTypes {
    protected final OpenGL gl;

    protected final List<PixelStorageType> all;

    protected final Map<Integer, List<PixelStorageType>> byComponents;

    public PixelStorageTypes(@NonNull OpenGL gl) {
        this.gl = gl;

        ImmutableList.Builder<PixelStorageType> builder = ImmutableList.builder();

        //regular primitive types
        for (int components = 1; components <= 4; components++) {
            builder.add(new SimplePrimitiveStorageType(GL_BYTE, PixelFormatChannelType.INTEGER, BYTE_TYPE, BYTE_SIZE, components));
            builder.add(new SimplePrimitiveStorageType(GL_UNSIGNED_BYTE, PixelFormatChannelType.UNSIGNED_INTEGER, BYTE_TYPE, BYTE_SIZE, components));
            builder.add(new SimplePrimitiveStorageType(GL_SHORT, PixelFormatChannelType.INTEGER, SHORT_TYPE, SHORT_SIZE, components));
            builder.add(new SimplePrimitiveStorageType(GL_UNSIGNED_SHORT, PixelFormatChannelType.UNSIGNED_INTEGER, CHAR_TYPE, CHAR_SIZE, components));
            builder.add(new SimplePrimitiveStorageType(GL_INT, PixelFormatChannelType.INTEGER, INT_TYPE, INT_SIZE, components));
            builder.add(new SimplePrimitiveStorageType(GL_UNSIGNED_INT, PixelFormatChannelType.UNSIGNED_INTEGER, INT_TYPE, INT_SIZE, components));
            builder.add(new SimplePrimitiveStorageType(GL_FLOAT, PixelFormatChannelType.FLOATING_POINT, FLOAT_TYPE, FLOAT_SIZE, components));
        }

        this.all = builder.build();

        //noinspection UnstableApiUsage
        this.byComponents = ImmutableMap.copyOf(this.all.stream().collect(Collectors.groupingBy(
                PixelStorageType::components,
                ImmutableList.toImmutableList())));
    }

    public Stream<PixelStorageType> all() {
        return this.all.stream();
    }

    public PixelStorageType getOptimalStorageTypeFor(@NonNull PixelFormatBuilderImpl builder, @NonNull PixelStorageFormat storageFormat) {
        checkArg(builder.type() == storageFormat.type(), "builder requested type %s, but storage format has %s", builder.type(), storageFormat.type());

        return this.byComponents.getOrDefault(storageFormat.channels().size(), Collections.emptyList())
                .stream()
                .filter(storageType -> {
                    PixelFormatChannelType genericType = storageType.genericType();

                    switch (storageFormat.type()) {
                        case FLOATING_POINT: //the storage format outputs floating-point values
                            switch (builder.range()) { //depending on the range, we can accept normalized fixed-point values computed from integers
                                case ZERO_TO_ONE:
                                    if (genericType == PixelFormatChannelType.INTEGER) { //signed integers cannot be normalized to range [0, 1]
                                        return false;
                                    }
                                    break;
                                case NEGATIVE_ONE_TO_ONE:
                                    if (genericType == PixelFormatChannelType.UNSIGNED_INTEGER) { //unsigned integers cannot be normalized to range [-1, 1]
                                        return false;
                                    }
                                    break;
                                case INFINITY:
                                    if (genericType
                                        != PixelFormatChannelType.FLOATING_POINT) { //integers will always be normalized when converted to a floating-point storage format
                                        return false;
                                    }
                                    break;
                                default:
                                    throw new IllegalArgumentException(Objects.toString(builder.range()));
                            }
                            break;
                        case INTEGER:
                        case UNSIGNED_INTEGER:
                            if (genericType != storageFormat.type()) { //only consider using integer types of the same sign
                                return false;
                            }
                            break;
                        default:
                            throw new IllegalArgumentException(Objects.toString(storageFormat.type()));
                    }

                    //make sure all the channels have a sufficient bit depth
                    Map<PixelFormatChannel, Integer> builderBitDepths = builder.channelsToMinimumBitDepths();
                    List<PixelFormatChannel> formatChannels = storageFormat.channels();
                    List<Integer> typeBitDepths = storageType.bitDepths();

                    for (int component = 0; component < formatChannels.size(); component++) {
                        PixelFormatChannel channel = formatChannels.get(component);
                        int typeBitDepth = typeBitDepths.get(component);
                        Integer builderBitDepth = builderBitDepths.get(channel);

                        if (builderBitDepth == null) { //it doesn't matter what the type's bit depth is, because the user said they don't care
                            continue;
                        }

                        if (builderBitDepth > typeBitDepth) { //the user has requested a bit depth greater than what the current format provides
                            return false;
                        } else { //the format's bit depth is sufficiently large to meet the user's requirements
                            //noinspection UnnecessaryContinue
                            continue;
                        }
                    }

                    return true;
                })
                .findFirst().orElseThrow(() -> new IllegalArgumentException("unable to determine storage type for " + builder));
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static class SimplePrimitiveStorageType implements PixelStorageType {
        private final int glType;
        private final PixelFormatChannelType genericType;
        private final Type asmType;
        private final int size;

        private final int components;

        private String toString;

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

        @Override
        public String toString() {
            if (this.toString == null) { //compute
                this.toString = OpenGL.class.desiredAssertionStatus()
                        ? OpenGLConstants.getNameIfPossible(this.glType).orElseGet(() -> String.valueOf(this.glType).intern())
                        : String.valueOf(this.glType).intern();
            }
            return this.toString;
        }
    }
}
