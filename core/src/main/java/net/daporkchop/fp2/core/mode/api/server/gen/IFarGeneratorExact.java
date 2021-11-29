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

package net.daporkchop.fp2.core.mode.api.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.lib.math.vector.Vec2i;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.stream.Stream;

/**
 * Type of {@link IFarGenerator} which generates tile data based on block data in the world.
 * <p>
 * Exact generators only operate on tiles at level 0.
 *
 * @author DaPorkchop_
 */
public interface IFarGeneratorExact<POS extends IFarPos, T extends IFarTile> extends IFarGenerator {
    /**
     * Gets the positions of all of the columns that need to be loaded in order for this generator to generate the tile at the given position.
     *
     * @param pos the position of the tile to generate
     * @return the positions of all of the columns that need to be loaded in order for this generator to generate the tile at the given position
     */
    Stream<Vec2i> neededColumns(@NonNull POS pos);

    /**
     * Gets the positions of all of the cubes that need to be loaded in order for this generator to generate the tile at the given position.
     * <p>
     * Note that this method is only guaranteed to be called in cubic chunks worlds.
     *
     * @param world the {@link FBlockWorld} providing access to block/height data in the world
     * @param pos   the position of the tile to generate
     * @return the positions of all of the cubes that need to be loaded in order for this generator to generate the tile at the given position
     */
    Stream<Vec3i> neededCubes(@NonNull FBlockWorld world, @NonNull POS pos);

    /**
     * Generates the terrain in the given tile.
     *
     * @param world the {@link FBlockWorld} providing access to block/height data in the world
     * @param pos   the position of the tile to generate
     * @param tile  the tile to generate
     */
    void generate(@NonNull FBlockWorld world, @NonNull POS pos, @NonNull T tile);

    /**
     * Factory method for creating instances of {@link IFarGeneratorExact}.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface Factory<POS extends IFarPos, T extends IFarTile> {
        /**
         * Creates a new {@link IFarGeneratorExact} in the given world.
         *
         * @param world the world
         * @return the new {@link IFarGeneratorExact}, or {@code null} if no generator could be created for the given world
         */
        IFarGeneratorExact<POS, T> forWorld(@NonNull IFarWorldServer world);
    }
}
