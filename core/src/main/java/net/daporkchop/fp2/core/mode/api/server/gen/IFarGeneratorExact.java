/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarServerResourceCreationEvent;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Type of {@link IFarGenerator} which generates tile data based on block data in the world.
 * <p>
 * Exact generators only operate on tiles at level 0.
 *
 * @author DaPorkchop_
 */
public interface IFarGeneratorExact<POS extends IFarPos, T extends IFarTile> extends IFarGenerator<POS, T> {
    /**
     * Gets an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time as the tile at the given position to potentially achieve better performance.
     * <p>
     * If an empty {@link Optional} is returned, no batching will be done and the tile will be generated individually.
     * <p>
     * Otherwise, the following restrictions apply to the positions which may be returned:
     * <ul>
     *     <li>the input position must be present in the list</li>
     *     <li>all of the positions must be valid (i.e. within the provider's {@link IFarCoordLimits coordinate limits})</li>
     *     <li>the positions must be chosen such that generation can only fail with a {@link GenerationNotAllowedException} if the input position would have failed. Conversely,
     *     if the input position would have been able to be generated successfully, generation for the whole batch <strong>must</strong> succeed. As thi is entirely dependent
     *     on the world's internal data representation, it is recommended to use {@link FBlockWorld#guaranteedDataAvailableVolume} to determine which data is guaranteed
     *     to be available if the data necessary for generating the input position is.</li>
     * </ul>
     *
     * @param world the {@link FBlockWorld} instance providing access to block data in the world
     * @param pos   the position of the tile to generate
     * @return an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time
     */
    default Optional<? extends Collection<POS>> batchGenerationGroup(@NonNull FBlockWorld world, @NonNull POS pos) {
        return Optional.empty(); //don't do batch generation by default
    }

    /**
     * Gets an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time as the tiles at the given positions to potentially achieve better performance.
     * <p>
     * If an empty {@link Optional} is returned, no additional batching will be done.
     * <p>
     * Otherwise, the following restrictions apply to the positions which may be returned:
     * <ul>
     *     <li>the input position must be present in the list</li>
     *     <li>all of the positions must be valid (i.e. within the provider's {@link IFarCoordLimits coordinate limits})</li>
     *     <li>the positions must be chosen such that generation can only fail with a {@link GenerationNotAllowedException} if the input position would have failed. Conversely,
     *     if the input position would have been able to be generated successfully, generation for the whole batch <strong>must</strong> succeed. As thi is entirely dependent
     *     on the world's internal data representation, it is recommended to use {@link FBlockWorld#guaranteedDataAvailableVolume} to determine which data is guaranteed
     *     to be available if the data necessary for generating the input position is.</li>
     * </ul>
     *
     * @param world     the {@link FBlockWorld} instance providing access to block data in the world
     * @param positions the position of the tile to generate
     * @return an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time
     */
    default Optional<? extends Collection<POS>> batchGenerationGroup(@NonNull FBlockWorld world, @NonNull Collection<POS> positions) {
        Set<POS> set = null;
        for (POS pos : positions) {
            Optional<? extends Collection<POS>> optionalBatchGroup = this.batchGenerationGroup(world, pos);
            if (optionalBatchGroup.isPresent()) {
                if (set == null) { //create set if it doesn't exist
                    //clone the input collection to ensure that all of the original input positions will be included
                    set = this.provider().mode().directPosAccess().clonePositionsAsSet(positions);
                }

                //add all positions to set
                set.addAll(optionalBatchGroup.get());
            }
        }

        return Optional.ofNullable(set);
    }

    /**
     * Generates the terrain in the given tile.
     *
     * @param world the {@link FBlockWorld} instance providing access to block data in the world
     * @param pos   the position of the tile to generate
     * @param tile  the tile to generate
     * @throws GenerationNotAllowedException if the generator attempts to access terrain which is not generated, and the given {@link FBlockWorld} does not allow generation
     */
    void generate(@NonNull FBlockWorld world, @NonNull POS pos, @NonNull T tile) throws GenerationNotAllowedException;

    /**
     * Generates the terrain in the given tiles.
     *
     * @param world     the {@link FBlockWorld} instance providing access to block data in the world
     * @param positions the positions of the tiles to generate
     * @param tiles     the tiles to generate
     * @throws GenerationNotAllowedException if the generator attempts to access terrain which is not generated, and the given {@link FBlockWorld} does not allow generation
     */
    default void generate(@NonNull FBlockWorld world, @NonNull POS[] positions, @NonNull T[] tiles) throws GenerationNotAllowedException {
        checkArg(positions.length == tiles.length, "positions (%d) and tiles (%d) must have same number of elements!", positions.length, tiles.length);

        for (int i = 0; i < positions.length; i++) {
            this.generate(world, positions[i], tiles[i]);
        }
    }

    /**
     * Fired to create a new {@link IFarGeneratorExact}.
     *
     * @author DaPorkchop_
     */
    interface CreationEvent<POS extends IFarPos, T extends IFarTile> extends IFarServerResourceCreationEvent<POS, T, IFarGeneratorExact<POS, T>> {
        /**
         * @return the {@link IFarTileProvider} which the {@link IFarGeneratorExact} will be created for
         */
        IFarTileProvider<POS, T> provider();
    }
}
