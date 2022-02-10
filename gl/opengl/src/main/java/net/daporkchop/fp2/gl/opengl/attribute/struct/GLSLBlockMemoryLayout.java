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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLVectorType;
import net.daporkchop.lib.common.math.PMath;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Memory layout types supported for GLSL interface blocks.
 *
 * @author DaPorkchop_
 * @see <a href="https://www.khronos.org/opengl/wiki/Interface_Block_(GLSL)#Memory_layout>OpenGL Wiki</a>
 */
public enum GLSLBlockMemoryLayout {
    /**
     * The {@code std140} layout.
     *
     * @see <a href="https://www.khronos.org/registry/OpenGL/specs/gl/glspec45.core.pdf#page=159>OpenGL 4.5, Section 7.6.2.2, page 137</a>
     */
    STD140 {
        private MemberLayout memberLayout(@NonNull StructProperty property) {
            return property.with(new StructProperty.TypedPropertyCallback<MemberLayout>() {
                @Override
                public MemberLayout.Basic withComponents(@NonNull StructProperty.Components componentsProperty) {
                    long alignment;
                    long size;
                    long[] componentOffsets;

                    GLSLBasicType type = componentsProperty.glslType().ensureValid();
                    if (type instanceof GLSLPrimitiveType) {
                        //1. If the member is a scalar consuming N basic machine units, the base align-
                        //   ment is N.

                        alignment = size = ((GLSLPrimitiveType) type).size();
                        componentOffsets = new long[1];
                    } else if (type instanceof GLSLVectorType) {
                        //2. If the member is a two- or four-component vector with components consum-
                        //   ing N basic machine units, the base alignment is 2N or 4N, respectively.

                        //3. If the member is a three-component vector with components consuming N
                        //   basic machine units, the base alignment is 4N.

                        GLSLVectorType vec = (GLSLVectorType) type;
                        alignment = size = vec.primitive().size() * (vec.components() == 3 ? 4L : vec.components());

                        componentOffsets = IntStream.range(0, vec.components())
                                .mapToLong(i -> i * (long) vec.primitive().size())
                                .toArray();
                    } else if (type instanceof GLSLMatrixType) {
                        //5. If the member is a column-major matrix with C columns and R rows, the
                        //   matrix is stored identically to an array of C column vectors with R compo-
                        //   nents each, according to rule (4).

                        //(not implemented)
                        //7. If the member is a row-major matrix with C columns and R rows, the matrix
                        //   is stored identically to an array of R row vectors with C components each,
                        //   according to rule (4).

                        GLSLMatrixType mat = (GLSLMatrixType) type;

                        //matCxR is equivalent to vecR[C]
                        //array element alignment+size is always in multiples of vec4
                        alignment = PMath.roundUp(mat.primitive().size() * mat.rows(), FLOAT_SIZE * 4);
                        size = alignment * mat.columns();

                        componentOffsets = new long[mat.columns() * mat.rows()];
                        long offset = 0L;
                        for (int i = 0, column = 0; column < mat.columns(); column++) {
                            for (int row = 0; row < mat.rows(); row++, i++, offset += mat.primitive().size()) {
                                componentOffsets[i] = offset;
                            }
                            offset = PMath.roundUp(offset, FLOAT_SIZE * 4);
                        }
                    } else {
                        throw new IllegalArgumentException(Objects.toString(type));
                    }

                    return new MemberLayout.Basic(alignment, size, componentOffsets);
                }

                @Override
                public MemberLayout.Array withElements(@NonNull StructProperty.Elements elementsProperty) {
                    //all array elements will have the same type, so we can just query the 0th element

                    return elementsProperty.element(0).with(new StructProperty.TypedPropertyCallback<MemberLayout.Array>() {
                        @Override
                        public MemberLayout.Array withComponents(@NonNull StructProperty.Components componentsProperty) {
                            //4. If the member is an array of scalars or vectors, the base alignment and array
                            //   stride are set to match the base alignment of a single array element, according
                            //   to rules (1), (2), and (3), and rounded up to the base alignment of a vec4. The
                            //   array may have padding at the end; the base offset of the member following
                            //   the array is rounded up to the next multiple of the base alignment.

                            //6. If the member is an array of S column-major matrices with C columns and
                            //   R rows, the matrix is stored identically to a row of S × C column vectors
                            //   with R components each, according to rule (4).

                            //(not implemented)
                            //8. If the member is an array of S row-major matrices with C columns and R
                            //   rows, the matrix is stored identically to a row of S × R row vectors with C
                            //   components each, according to rule (4).

                            @SuppressWarnings("UnqualifiedMethodAccess")
                            MemberLayout elementLayout = memberLayout(componentsProperty);

                            if (componentsProperty.cols() == 1) { //not a matrix, the array elements are all vectors or scalars
                                //round alignment and stride up to multiple of vec4
                                elementLayout = new MemberLayout.Basic(
                                        PMath.roundUp(elementLayout.alignment(), 4 * FLOAT_SIZE),
                                        PMath.roundUp(elementLayout.size(), 4 * FLOAT_SIZE),
                                        ((MemberLayout.Basic) elementLayout).componentOffsets);
                            } else { //this is a matrix type
                                //matrices are always interpreted as R vectors with C components each, even if they're not used in an array, so we can use
                                // the existing elementLayout with no additional changes.
                            }

                            return new MemberLayout.Array(elementLayout, elementsProperty.elements());
                        }

                        @Override
                        public MemberLayout.Array withElements(@NonNull StructProperty.Elements elementsProperty) {
                            //we can't support this without a hard dependency on ARB_arrays_of_arrays
                            throw new UnsupportedOperationException("arrays of arrays are not supported!");
                        }

                        @Override
                        public MemberLayout.Array withFields(@NonNull StructProperty.Fields fieldsProperty) {
                            //10. If the member is an array of S structures, the S elements of the array are laid
                            //    out in order, according to rule (9).

                            @SuppressWarnings("UnqualifiedMethodAccess")
                            MemberLayout elementLayout = memberLayout(fieldsProperty);

                            return new MemberLayout.Array(elementLayout, elementsProperty.elements());
                        }
                    });
                }

                @Override
                public MemberLayout.Struct withFields(@NonNull StructProperty.Fields fieldsProperty) {
                    //9. If the member is a structure, the base alignment of the structure is N , where
                    //   N is the largest base alignment value of any of its members, and rounded
                    //   up to the base alignment of a vec4. The individual members of this sub-
                    //   structure are then assigned offsets by applying this set of rules recursively,
                    //   where the base offset of the first member of the sub-structure is equal to the
                    //   aligned offset of the structure. The structure may have padding at the end;
                    //   the base offset of the member following the sub-structure is rounded up to
                    //   the next multiple of the base alignment of the structure.

                    @SuppressWarnings("UnqualifiedMethodAccess")
                    MemberLayout[] fieldLayouts = IntStream.range(0, fieldsProperty.fields()).mapToObj(fieldsProperty::fieldProperty)
                            .map(field -> memberLayout(field))
                            .toArray(MemberLayout[]::new);

                    long offset = 0L;
                    for (MemberLayout fieldLayout : fieldLayouts) {
                        //pad to member alignment, offset by member size
                        offset = PMath.roundUp(offset, fieldLayout.alignment()) + fieldLayout.size();
                    }

                    //If the member is a structure, the base alignment of the structure is N, where N is the largest base alignment value of any of
                    // its members, and rounded up to the base alignment of a vec4.
                    long structAlignment = PMath.roundUp(
                            Stream.of(fieldLayouts)
                                    .mapToLong(MemberLayout::alignment)
                                    .max().orElse(0L),
                            FLOAT_SIZE * 4);

                    //The structure may have padding at the end; the base offset of the member following the sub-structure is rounded up to
                    // the next multiple of the base alignment of the structure.
                    long size = PMath.roundUp(offset, structAlignment);

                    return new MemberLayout.Struct(structAlignment, size, fieldLayouts);
                }
            });
        }

        private InterleavedStructLayout.Member toInterleaved(long baseOffset, @NonNull MemberLayout layout) {
            return layout.with(new MemberLayout.LayoutCallback<InterleavedStructLayout.Member>() {
                @Override
                public InterleavedStructLayout.Member withBasic(@NonNull MemberLayout.Basic basic) {
                    return new InterleavedStructLayout.RegularMember(baseOffset, basic.componentOffsets);
                }

                @Override
                public InterleavedStructLayout.Member withArray(@NonNull MemberLayout.Array array) {
                    InterleavedStructLayout.Member[] children = new InterleavedStructLayout.Member[array.length];

                    long offset = 0L;
                    for (int i = 0; i < array.length; i++) {
                        //pad to member alignment
                        offset = PMath.roundUp(offset, array.elementLayout.alignment());

                        //noinspection UnqualifiedMethodAccess
                        children[i] = toInterleaved(baseOffset + offset, array.elementLayout);

                        //advance by member size
                        offset += array.elementLayout.size();
                    }

                    return new InterleavedStructLayout.NestedMember(children);
                }

                @Override
                public InterleavedStructLayout.Member withStruct(@NonNull MemberLayout.Struct struct) {
                    InterleavedStructLayout.Member[] children = new InterleavedStructLayout.Member[struct.fieldLayouts.length];

                    long offset = 0L;
                    for (int i = 0; i < children.length; i++) {
                        MemberLayout fieldLayout = struct.fieldLayouts[i];

                        //pad to member alignment
                        offset = PMath.roundUp(offset, fieldLayout.alignment());

                        //noinspection UnqualifiedMethodAccess
                        children[i] = toInterleaved(baseOffset + offset, fieldLayout);

                        //advance by member size
                        offset += fieldLayout.size();
                    }

                    return new InterleavedStructLayout.NestedMember(children);
                }
            });
        }

        @Override
        public <S> InterleavedStructLayout layout(@NonNull StructInfo<S> structInfo) {
            MemberLayout layout = this.memberLayout(structInfo.unpackedProperty());

            return InterleavedStructLayout.builder()
                    .structInfo(structInfo)
                    .layoutName(this.name().toLowerCase(Locale.ROOT).intern())
                    .unpacked(true)
                    .member(this.toInterleaved(0L, layout))
                    .stride(layout.size())
                    .build();
        }
    };

    /**
     * Creates an {@link InterleavedStructLayout} for the given {@link StructInfo} compatible with this memory layout.
     *
     * @param structInfo the struct info
     * @param <S>        the struct type
     * @return the {@link InterleavedStructLayout}
     */
    public abstract <S> InterleavedStructLayout layout(@NonNull StructInfo<S> structInfo);

    /**
     * A simple tuple containing a struct member's alignment and size.
     *
     * @author DaPorkchop_
     */
    @Data
    private static abstract class MemberLayout {
        private final long alignment;
        private final long size;

        protected abstract <T> T with(@NonNull LayoutCallback<T> callback);

        /**
         * @author DaPorkchop_
         */
        private interface LayoutCallback<T> {
            T withBasic(@NonNull Basic basic);

            T withArray(@NonNull Array array);

            T withStruct(@NonNull Struct struct);
        }

        /**
         * @author DaPorkchop_
         */
        private static class Basic extends MemberLayout {
            private final long[] componentOffsets;

            public Basic(long alignment, long size, @NonNull long[] componentOffsets) {
                super(alignment, size);

                this.componentOffsets = componentOffsets;
            }

            @Override
            protected <T> T with(@NonNull LayoutCallback<T> callback) {
                return callback.withBasic(this);
            }
        }

        /**
         * @author DaPorkchop_
         */
        private static class Array extends MemberLayout {
            private final MemberLayout elementLayout;
            private final int length;

            public Array(@NonNull MemberLayout elementLayout, int length) {
                super(elementLayout.alignment(), elementLayout.size() * length);

                this.elementLayout = elementLayout;
                this.length = positive(length, "length");
            }

            @Override
            protected <T> T with(@NonNull LayoutCallback<T> callback) {
                return callback.withArray(this);
            }
        }

        /**
         * @author DaPorkchop_
         */
        private static class Struct extends MemberLayout {
            private final MemberLayout[] fieldLayouts;

            public Struct(long alignment, long size, @NonNull MemberLayout[] fieldLayouts) {
                super(alignment, size);

                this.fieldLayouts = fieldLayouts;
            }

            @Override
            protected <T> T with(@NonNull LayoutCallback<T> callback) {
                return callback.withStruct(this);
            }
        }
    }
}
