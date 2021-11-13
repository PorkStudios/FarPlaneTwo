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

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLVectorType;
import net.daporkchop.lib.common.math.PMath;

import java.util.Objects;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;

/**
 * @author DaPorkchop_
 */
public enum GLSLLayout {
    STD140 {
        private AlignmentSize alignmentSize(@NonNull GLSLType type) {
            long alignment;
            long size;

            if (type instanceof GLSLPrimitiveType) {
                alignment = size = ((GLSLPrimitiveType) type).size();
            } else if (type instanceof GLSLVectorType) {
                GLSLVectorType vec = (GLSLVectorType) type;
                alignment = size = vec.primitive().size() * (vec.components() == 3 ? 4L : vec.components());
            } else if (type instanceof GLSLMatrixType) {
                GLSLMatrixType mat = (GLSLMatrixType) type;

                //matCxR is equivalent to vecR[C]
                //array element alignment+size is always in multiples of vec4
                alignment = PMath.roundUp(mat.primitive().size() * mat.rows(), FLOAT_SIZE * 4);
                size = alignment * mat.columns();
            } else {
                throw new IllegalArgumentException(Objects.toString(type));
            }

            return new AlignmentSize(alignment, size);
        }

        @Override
        public <S> InterleavedStructLayout<S> layout(@NonNull StructInfo<S> structInfo) {
            int memberCount = structInfo.members.size();
            long[] memberOffsets = new long[memberCount];

            GLSLType[] types = structInfo.members.stream()
                    .map(member -> member.unpackedStage.glslType())
                    .toArray(GLSLType[]::new);

            AlignmentSize[] alignmentSizes = Stream.of(types)
                    .map(this::alignmentSize)
                    .toArray(AlignmentSize[]::new);

            long offset = 0L;
            for (int i = 0; i < memberCount; i++) {
                AlignmentSize alignmentSize = alignmentSizes[i];

                //pad to alignment
                offset = PMath.roundUp(offset, alignmentSize.alignment);
                memberOffsets[i] = offset;

                //offset by member size
                offset += alignmentSize.size;
            }

            //"struct alignment will be the alignment for the biggest structure member [...], rounded up to a multiple of the size of a vec4."
            //TODO: does that mean the member with the biggest alignment, or the biggest total size? logically i would assume it's the former, but the text seems to
            // imply the latter...
            long structAlignment = PMath.roundUp(
                    Stream.of(alignmentSizes)
                            .mapToLong(AlignmentSize::alignment)
                            .max().orElse(0L),
                    FLOAT_SIZE * 4);

            return InterleavedStructLayout.<S>builder()
                    .structInfo(structInfo)
                    .layoutName("std140")
                    .unpacked(true)
                    .memberOffsets(memberOffsets)
                    .stride(PMath.roundUp(offset, structAlignment))
                    .build();
        }
    };

    public abstract <S> InterleavedStructLayout<S> layout(@NonNull StructInfo<S> structInfo);

    /**
     * A simple tuple containing a struct member's alignment and size.
     *
     * @author DaPorkchop_
     */
    @Data
    private static class AlignmentSize {
        public final long alignment;
        public final long size;
    }
}
