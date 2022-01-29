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
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLVectorType;
import net.daporkchop.lib.common.math.PMath;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;

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
        private MemberLayout alignmentSize(@NonNull GLSLType type) {
            long alignment;
            long size;
            long[] componentOffsets;

            if (type instanceof GLSLPrimitiveType) {
                alignment = size = ((GLSLPrimitiveType) type).size();
                componentOffsets = new long[1];
            } else if (type instanceof GLSLVectorType) {
                GLSLVectorType vec = (GLSLVectorType) type;
                alignment = size = vec.primitive().size() * (vec.components() == 3 ? 4L : vec.components());

                componentOffsets = IntStream.range(0, vec.components())
                        .mapToLong(i -> i * (long) vec.primitive().size())
                        .toArray();
            } else if (type instanceof GLSLMatrixType) {
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

            return new MemberLayout(alignment, size, componentOffsets);
        }

        @Override
        public <S> InterleavedStructLayout<S> layout(@NonNull StructInfo<S> structInfo) {
            int memberCount = structInfo.members.size();
            long[] memberOffsets = new long[memberCount];
            long[][] memberComponentOffsets = new long[memberCount][];

            GLSLType[] types = structInfo.members.stream()
                    .map(member -> member.unpackedStage.glslType())
                    .toArray(GLSLType[]::new);

            MemberLayout[] memberLayouts = Stream.of(types)
                    .map(this::alignmentSize)
                    .toArray(MemberLayout[]::new);

            long offset = 0L;
            for (int i = 0; i < memberCount; i++) {
                MemberLayout memberLayout = memberLayouts[i];

                //pad to alignment
                offset = PMath.roundUp(offset, memberLayout.alignment);
                memberOffsets[i] = offset;
                memberComponentOffsets[i] = memberLayout.componentOffsets;

                //offset by member size
                offset += memberLayout.size;
            }

            //If the member is a structure, the base alignment of the structure is N, where N is the largest base alignment value of any of
            // its members, and rounded up to the base alignment of a vec4.
            long structAlignment = PMath.roundUp(
                    Stream.of(memberLayouts)
                            .mapToLong(MemberLayout::alignment)
                            .max().orElse(0L),
                    FLOAT_SIZE * 4);
            long stride = PMath.roundUp(offset, structAlignment);

            return InterleavedStructLayout.<S>builder()
                    .structInfo(structInfo)
                    .layoutName(this.name().toLowerCase(Locale.ROOT).intern())
                    .unpacked(true)
                    .memberOffsets(memberOffsets)
                    .memberComponentOffsets(memberComponentOffsets)
                    .stride(stride)
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
    public abstract <S> InterleavedStructLayout<S> layout(@NonNull StructInfo<S> structInfo);

    /**
     * A simple tuple containing a struct member's alignment and size.
     *
     * @author DaPorkchop_
     */
    @Data
    private static class MemberLayout {
        public final long alignment;
        public final long size;

        @NonNull
        public final long[] componentOffsets;
    }
}
