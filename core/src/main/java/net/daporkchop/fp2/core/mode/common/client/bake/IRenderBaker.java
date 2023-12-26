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

package net.daporkchop.fp2.core.mode.common.client.bake;

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;

import java.util.stream.Stream;

/**
 * Converts uncompressed tile contents to renderable data which can be stored in an {@link IBakeOutputStorage}.
 *
 * @author DaPorkchop_
 */
public interface IRenderBaker<B extends IBakeOutput> {
    /**
     * Gets the positions of the tiles which use the tile at the given position as a bake input.
     *
     * @param pos the position of the tile
     * @return Gets the positions of the tiles which use the tile at the given position as a bake input
     */
    Stream<TilePos> bakeOutputs(@NonNull TilePos pos);

    /**
     * Gets the positions of the tiles required in order to bake the tile at the given position.
     *
     * @param pos the position of the tile to bake
     * @return the positions of the tiles required in order to bake the tile at the given position
     */
    Stream<TilePos> bakeInputs(@NonNull TilePos pos);

    /**
     * Bakes the tile data at the given position.
     *
     * @param pos    the position of the tile to bake
     * @param srcs   the uncompressed source tiles. Tiles are provided in the same order as they were contained in the stream returned by {@link #bakeInputs(TilePos)}, and will
     *               be {@code null} if not loaded
     * @param output the {@link IBakeOutput} to write to
     */
    void bake(@NonNull TilePos pos, @NonNull Tile[] srcs, @NonNull B output);
}
