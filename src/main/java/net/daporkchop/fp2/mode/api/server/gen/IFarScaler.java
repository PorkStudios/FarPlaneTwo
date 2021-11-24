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

package net.daporkchop.fp2.mode.api.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;

import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface IFarScaler<POS extends IFarPos, T extends IFarTile> {
    /**
     * Gets the positions of all the low-detail tiles whose contents are affected by the content of the given high-detail tile.
     * <p>
     * The returned {@link Stream} must be sequential!
     *
     * @param srcPos the position of the high-detail tile
     * @return the positions of all the low-detail tiles whose contents are affected by the content of the given high-detail tile
     */
    Stream<POS> outputs(@NonNull POS srcPos);

    /**
     * Gets the positions of all the high-detail tiles needed to create the given low-detail tile.
     * <p>
     * The returned {@link Stream} must be sequential!
     *
     * @param dstPos the position of the low-detail tile to generate
     * @return the positions of all the high-detail tiles needed to create the given low-detail tile
     */
    Stream<POS> inputs(@NonNull POS dstPos);

    /**
     * Merges the content of the given high-detail tiles into the given low-detail data tile.
     *
     * @param srcs an array containing the high-detail tiles. Tiles are in the same order as provided by the {@link Stream} returned by
     *             {@link #inputs(IFarPos)}. Any of the tiles may be {@code null}, in which case they should be treated by the implementation
     *             as if they were merely empty.
     * @param dst  the low-detail tile to merge the content into
     * @return the extra data to be saved with the tile
     */
    long scale(@NonNull T[] srcs, @NonNull T dst);
}
