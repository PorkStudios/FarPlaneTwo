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

package net.daporkchop.fp2.strategy.common.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;

import java.util.stream.Stream;

/**
 * Type of {@link IFarGenerator} which generates piece data based on block data in the world.
 * <p>
 * Exact generators only operate on pieces at level 0.
 *
 * @author DaPorkchop_
 */
public interface IFarGeneratorExact<POS extends IFarPos, P extends IFarPiece<POS>> extends IFarGenerator {
    @Override
    void init(@NonNull WorldServer world);

    /**
     * Gets the positions of all of the columns that need to be loaded in order for this generator to generate the piece at the given position.
     *
     * @param pos the position of the piece to generate
     * @return the positions of all of the columns that need to be loaded in order for this generator to generate the piece at the given position
     */
    Stream<ChunkPos> neededColumns(@NonNull POS pos);

    /**
     * Gets the positions of all of the cubes that need to be loaded in order for this generator to generate the piece at the given position.
     * <p>
     * Note that this method is only guaranteed to be called in cubic chunks worlds.
     *
     * @param world the {@link IBlockHeightAccess} providing access to block/height data in the world
     * @param pos   the position of the piece to generate
     * @return the positions of all of the cubes that need to be loaded in order for this generator to generate the piece at the given position
     */
    Stream<Vec3i> neededCubes(@NonNull IBlockHeightAccess world, @NonNull POS pos);

    /**
     * Generates a rough estimate of the terrain in the given piece.
     *
     * @param world the {@link IBlockHeightAccess} providing access to block/height data in the world
     * @param piece the piece to generate
     */
    void generate(@NonNull IBlockHeightAccess world, @NonNull P piece);
}
