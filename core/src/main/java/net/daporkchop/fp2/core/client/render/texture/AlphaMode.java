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

package net.daporkchop.fp2.core.client.render.texture;

import lombok.NonNull;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public enum AlphaMode {
    NONE {
        @Override
        public void combine(@NonNull int[] src0, int src0Offset, @NonNull int[] src1, int src1Offset, @NonNull int[] dst, int dstOffset, int len) {
            checkRangeLen(src0.length, src0Offset, len);
            checkRangeLen(src1.length, src1Offset, len);
            checkRangeLen(dst.length, dstOffset, len);

            System.arraycopy(src1, src1Offset, dst, dstOffset, len);
        }
    },
    CUTOUT {
        @Override
        public void combine(@NonNull int[] src0, int src0Offset, @NonNull int[] src1, int src1Offset, @NonNull int[] dst, int dstOffset, int len) {
            checkRangeLen(src0.length, src0Offset, len);
            checkRangeLen(src1.length, src1Offset, len);
            checkRangeLen(dst.length, dstOffset, len);

            for (int i = 0; i < len; i++) {
                int colorSrc0 = src0[src0Offset + i];
                int colorSrc1 = src1[src1Offset + i];

                //dst = src1.a >= 0.5 ? src1 : src0
                int mask = colorSrc1 >> 31;
                dst[dstOffset + i] = (colorSrc0 & ~mask) | (colorSrc1 & mask);
            }
        }
    },
    BLEND_ONE_MINUS_SOURCE_ALPHA {
        int div255_1x8(int i) {
            return ((i + 1) + ((i + 1) >>> 8)) >>> 8;
        }

        int div255_2x8_16(int i) {
            return ((i + 0x10001) + ((i + 0x10001) >>> 8)) >>> 8;
        }

        @Override
        public void combine(@NonNull int[] src0, int src0Offset, @NonNull int[] src1, int src1Offset, @NonNull int[] dst, int dstOffset, int len) {
            checkRangeLen(src0.length, src0Offset, len);
            checkRangeLen(src1.length, src1Offset, len);
            checkRangeLen(dst.length, dstOffset, len);

            for (int i = 0; i < len; i++) {
                int colorSrc0 = src0[src0Offset + i];
                int colorSrc1 = src1[src1Offset + i];
                int alpha = colorSrc1 >>> 24;

                //dst = src0 * (1.0 - src1.a) + src1 * src1.a
                dst[dstOffset + i] =
                        (this.div255_2x8_16(((colorSrc0 >>> 8) & 0x00FF00FF) * (0xFF - alpha) + ((colorSrc1 >>> 8) & 0x00FF00FF) * alpha) << 8)
                        | this.div255_2x8_16((colorSrc0 & 0x00FF00FF) * (0xFF - alpha) + (colorSrc1 & 0x00FF00FF) * alpha);
            }
        }
    };

    /**
     * Combines the ARGB8 colors in the given source arrays and writes them to the given destination buffer.
     *
     * @param src0       the first source array, containing the current colors
     * @param src0Offset the offset in the first source array to begin reading from
     * @param src1       the second source array, containing the incoming colors
     * @param src1Offset the offset in the second source array to begin reading from
     * @param dst        the destination array
     * @param dstOffset  the offset in the destination array to begin writing to
     * @param len        the number of elements to process
     */
    public abstract void combine(@NonNull int[] src0, int src0Offset, @NonNull int[] src1, int src1Offset, @NonNull int[] dst, int dstOffset, int len);
}
