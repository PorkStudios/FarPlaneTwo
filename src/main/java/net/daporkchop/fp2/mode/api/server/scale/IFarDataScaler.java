/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.mode.api.server.scale;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPieceData;

import java.util.stream.Stream;

/**
 * Merges the content of multiple high-detail data pieces into a single lower-detail data piece.
 *
 * @author DaPorkchop_
 */
public interface IFarDataScaler<POS extends IFarPos, D extends IFarPieceData> extends IFarScaler<POS> {
    /**
     * Merges the content of the given high-detail data pieces into the given low-detail data piece.
     *
     * @param srcs an array containing the high-detail pieces. Pieces are in the same order as provided by the {@link Stream} returned by
     *             {@link #inputs(IFarPos)}. Any of the pieces may be {@code null}, in which case they should be treated by the implementation
     *             as if they were merely empty.
     * @param dst  the low-detail data piece to merge the content into
     */
    void scale(@NonNull D[] srcs, @NonNull D dst);
}
