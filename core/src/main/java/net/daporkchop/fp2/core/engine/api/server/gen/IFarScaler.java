/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.api.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface IFarScaler extends IFarGenerator {
    /**
     * Gets the positions of all the low-detail tiles whose contents are affected by the content of the given high-detail tile.
     *
     * @param srcPos the position of the high-detail tile
     * @return the positions of all the low-detail tiles whose contents are affected by the content of the given high-detail tile
     */
    List<TilePos> outputs(@NonNull TilePos srcPos);

    /**
     * Gets the positions of all the low-detail tiles whose contents are affected by the content of the given high-detail tiles.
     * <p>
     * Bulk equivalent to {@link #outputs(TilePos)}.
     *
     * @param srcPositions the positions of the high-detail tiles
     * @return the positions of all the low-detail tiles whose contents are affected by the content of the given high-detail tiles
     * @see #outputs(TilePos)
     */
    default Collection<TilePos> uniqueOutputs(@NonNull Iterable<TilePos> srcPositions) {
        Set<TilePos> set = DirectTilePosAccess.newPositionHashSet();

        //get all positions and add them to the set (this discards duplicates)
        srcPositions.forEach(pos -> set.addAll(this.outputs(pos)));

        return set;
    }

    /**
     * Gets the positions of all the high-detail tiles needed to create the given low-detail tile.
     *
     * @param dstPos the position of the low-detail tile to generate
     * @return the positions of all the high-detail tiles needed to create the given low-detail tile
     */
    List<TilePos> inputs(@NonNull TilePos dstPos);

    /**
     * Gets the positions of all the high-detail tiles needed to create the given low-detail tiles.
     *
     * @param dstPositions the positions of the low-detail tiles to generate
     * @return the unique positions of all the high-detail tiles needed to create the given low-detail tiles
     * @see #inputs(TilePos)
     */
    default Collection<TilePos> uniqueInputs(@NonNull Iterable<TilePos> dstPositions) {
        Set<TilePos> set = DirectTilePosAccess.newPositionHashSet();

        //get all positions and add them to the set (this discards duplicates)
        dstPositions.forEach(pos -> set.addAll(this.inputs(pos)));
        return set;
    }

    /**
     * Merges the content of the given high-detail tiles into the given low-detail data tile.
     *
     * @param srcs an array containing the high-detail tiles. Tiles are in the same order as provided by the {@link Stream} returned by
     *             {@link #inputs(TilePos)}. Any of the tiles may be {@code null}, in which case they should be treated by the implementation
     *             as if they were merely empty.
     * @param dst  the low-detail tile to merge the content into
     * @return the extra data to be saved with the tile
     */
    long scale(@NonNull Tile[] srcs, @NonNull Tile dst);
}
