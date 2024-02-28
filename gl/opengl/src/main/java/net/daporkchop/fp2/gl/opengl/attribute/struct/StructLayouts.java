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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.LayoutComponentStorage;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.TextureStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.AttributeType;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.util.PArrays;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class StructLayouts {
    private InterleavedStructLayout.Member interleaved(@NonNull MutableLong offset, long alignment, @NonNull AttributeType property, boolean unpacked) {
        return property.with(new AttributeType.TypedCallback<InterleavedStructLayout.Member>() {
            @Override
            public InterleavedStructLayout.Member withComponents(@NonNull AttributeType.Components componentsType) {
                long[] componentOffsets = new long[componentsType.components()];
                for (int i = 0, col = 0; col < componentsType.cols(); col++) {
                    for (int row = 0; row < componentsType.rows(); row++, i++) {
                        componentOffsets[i] = offset.getAndAdd(unpacked
                                ? componentsType.componentInterpretation().outputType().size()
                                : componentsType.logicalStorageType().size());
                    }
                    offset.roundUp(alignment);
                }
                return new InterleavedStructLayout.RegularMember(0L, componentOffsets,
                        PArrays.filled(componentsType.components(), LayoutComponentStorage.class,
                                unpacked ? LayoutComponentStorage.unpacked(componentsType) : LayoutComponentStorage.unchanged(componentsType)));
            }

            @Override
            public InterleavedStructLayout.Member withElements(@NonNull AttributeType.Elements elementsType) {
                return this.withNested(IntStream.range(0, elementsType.elements()).mapToObj(i -> elementsType.componentType()));
            }

            @Override
            public InterleavedStructLayout.Member withFields(@NonNull AttributeType.Fields fieldsType) {
                return this.withNested(IntStream.range(0, fieldsType.fields()).mapToObj(fieldsType::fieldProperty));
            }

            private InterleavedStructLayout.Member withNested(@NonNull Stream<AttributeType> nestedProperty) {
                return new InterleavedStructLayout.NestedMember(nestedProperty
                        .map(property -> interleaved(offset, alignment, property, unpacked))
                        .toArray(InterleavedStructLayout.Member[]::new));
            }
        });
    }

    public <S> InterleavedStructLayout vertexAttributesInterleaved(@NonNull OpenGL gl, @NonNull StructInfo<S> structInfo, boolean unpacked) {
        MutableLong offset = new MutableLong();
        InterleavedStructLayout.Member member = interleaved(offset, gl.vertexAttributeAlignment(), structInfo.property(), unpacked);

        return InterleavedStructLayout.builder()
                .structInfo(structInfo)
                .layoutName("vertex_attribute_interleaved")
                .member(member)
                .stride(offset.value)
                .build();
    }

    public <S> TextureStructLayout texture(@NonNull OpenGL gl, @NonNull StructInfo<S> structInfo) {
        boolean unpacked = structInfo.property().with(new AttributeType.TypedCallback<Boolean>() {
            @Override
            public Boolean withComponents(@NonNull AttributeType.Components componentsType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean withElements(@NonNull AttributeType.Elements elementsType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean withFields(@NonNull AttributeType.Fields fieldsType) {
                checkArg(fieldsType.fields() == 1, "expected exactly one field, but found %d! %s", fieldsType.fields(), structInfo);

                return fieldsType.fieldProperty(0).with(new AttributeType.TypedCallback<Boolean>() {
                    @Override
                    public Boolean withComponents(@NonNull AttributeType.Components componentsType) {
                        ComponentInterpretation interpretation = componentsType.componentInterpretation();

                        if (componentsType.logicalStorageType().integer()
                            && !interpretation.outputType().integer()
                            && interpretation.outputType().signed()
                            && interpretation.normalized()) {
                            //input type is a signed integer, which will be converted to a signed normalized float
                            return !GLExtension.GL_EXT_texture_snorm.supported(gl.env()); //we'll unpack the texture values manually if GL_*_SNORM textures aren't available
                        }

                        return false;
                    }

                    @Override
                    public Boolean withElements(@NonNull AttributeType.Elements elementsType) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Boolean withFields(@NonNull AttributeType.Fields fieldsType) {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        });

        MutableLong stride = new MutableLong();
        TextureStructLayout.Member member = structInfo.property().with(new AttributeType.TypedCallback<TextureStructLayout.Member>() {
            @Override
            public TextureStructLayout.Member withComponents(@NonNull AttributeType.Components componentsType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TextureStructLayout.Member withElements(@NonNull AttributeType.Elements elementsType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TextureStructLayout.Member withFields(@NonNull AttributeType.Fields fieldsType) {
                checkArg(fieldsType.fields() == 1, "expected exactly one field, but found %d! %s", fieldsType.fields(), structInfo);

                return fieldsType.fieldProperty(0).with(new AttributeType.TypedCallback<TextureStructLayout.Member>() {
                    @Override
                    public TextureStructLayout.Member withComponents(@NonNull AttributeType.Components componentsType) {
                        long[] componentOffsets = new long[componentsType.components()];
                        for (int i = 0; i < componentsType.components(); i++) {
                            componentOffsets[i] = stride.getAndAdd(componentsType.logicalStorageType().size());
                        }
                        return new TextureStructLayout.RegularMember(0L, componentOffsets,
                                PArrays.filled(componentsType.components(), LayoutComponentStorage.class, LayoutComponentStorage.unchanged(componentsType)));
                    }

                    @Override
                    public TextureStructLayout.Member withElements(@NonNull AttributeType.Elements elementsType) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public TextureStructLayout.Member withFields(@NonNull AttributeType.Fields fieldsType) {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        });

        return TextureStructLayout.builder()
                .structInfo(structInfo)
                .layoutName("texture")
                .stride(stride.value)
                .member(member)
                .build();
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    private static final class MutableLong {
        private long value;

        public MutableLong roundUp(long to) {
            this.value = PMath.roundUp(this.value, to);
            return this;
        }

        public long getAndAdd(long d) {
            long old = this.value;
            this.value += d;
            return old;
        }
    }
}
