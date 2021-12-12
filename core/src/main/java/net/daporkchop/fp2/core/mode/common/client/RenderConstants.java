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

package net.daporkchop.fp2.core.mode.common.client;

import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;

/**
 * Constant values used throughout the render code.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class RenderConstants {
    public final int RENDER_PASS_COUNT = 3; //the total number of render passes

    /**
     * Emits the indices for drawing a quad.
     *
     * @param indices        the {@link ByteBuf} to write the indices to
     * @param oppositeCorner the index of the vertex in the corner opposite the provoking vertex
     * @param c0             the index of one of the edge vertices
     * @param c1             the index of the other edge vertex
     * @param provoking      the index of the provoking vertex
     */
    public static void emitQuad(ByteBuf indices, int oppositeCorner, int c0, int c1, int provoking) {
        indices.writeShortLE(c1).writeShortLE(oppositeCorner).writeShortLE(c0).writeShortLE(provoking);
    }
}
