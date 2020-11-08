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

import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface IFarScaler<POS extends IFarPos> {
    /**
     * Gets the positions of all the low-detail pieces whose contents are affected by the content of the given high-detail piece.
     * <p>
     * The returned {@link Stream} must be sequential!
     *
     * @param srcPos the position of the high-detail piece
     * @return the positions of all the low-detail pieces whose contents are affected by the content of the given high-detail piece
     */
    Stream<POS> outputs(@NonNull POS srcPos);

    /**
     * Gets the positions of all the high-detail pieces needed to create the given low-detail piece.
     * <p>
     * The returned {@link Stream} must be sequential!
     *
     * @param dstPos the position of the low-detail piece to generate
     * @return the positions of all the high-detail pieces needed to create the given low-detail piece
     */
    Stream<POS> inputs(@NonNull POS dstPos);
}
