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
import net.daporkchop.fp2.core.engine.api.server.IFarServerResourceCreationEvent;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Extracts height and color information from a world for use by a rendering mode.
 * <p>
 * Once initialized, instances of this class are expected to be safely usable by multiple concurrent threads.
 *
 * @author DaPorkchop_
 */
public interface IFarGeneratorRough extends IFarGenerator {
    /**
     * Gets an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time as the tile at the given position to potentially achieve better performance.
     * <p>
     * If an empty {@link Optional} is returned, no batching will be done and the tile will be generated individually.
     *
     * @param pos the position of the tile to generate
     * @return an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time
     */
    default Optional<? extends Collection<TilePos>> batchGenerationGroup(@NonNull TilePos pos) {
        return Optional.empty(); //don't do batch generation by default
    }

    /**
     * Gets an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time as the tiles at the given positions to potentially achieve better performance.
     * <p>
     * If an empty {@link Optional} is returned, no additional batching will be done.
     *
     * @param positions the position of the tile to generate
     * @return an {@link Optional} {@link Collection} containing all the tile positions which may be generated at the same time
     */
    default Optional<? extends Collection<TilePos>> batchGenerationGroup(@NonNull Collection<TilePos> positions) {
        Set<TilePos> set = null;
        for (TilePos pos : positions) {
            Optional<? extends Collection<TilePos>> optionalBatchGroup = this.batchGenerationGroup(pos);
            if (optionalBatchGroup.isPresent()) {
                if (set == null) { //create set if it doesn't exist
                    //clone the input collection to ensure that all of the original input positions will be included
                    set = DirectTilePosAccess.clonePositionsAsHashSet(positions);
                }

                //add all positions to set
                set.addAll(optionalBatchGroup.get());
            }
        }

        return Optional.ofNullable(set);
    }

    /**
     * Checks whether or not rough generation at the given position is possible.
     * <p>
     * The position must be valid (within the world's coordinate limits)!
     *
     * @param pos the position to check
     * @return whether or not rough generation is possible
     */
    boolean canGenerate(@NonNull TilePos pos);

    /**
     * Generates a rough estimate of the terrain in the given tile.
     *
     * @param pos  the position of the tile to generate
     * @param tile the tile to generate
     */
    void generate(@NonNull TilePos pos, @NonNull Tile tile);

    /**
     * Generates a rough estimate of the terrain in the given tiles.
     *
     * @param positions the positions of the tiles to generate
     * @param tiles     the tiles to generate
     */
    default void generate(@NonNull TilePos[] positions, @NonNull Tile[] tiles) {
        checkArg(positions.length == tiles.length, "positions (%d) and tiles (%d) must have same number of elements!", positions.length, tiles.length);

        for (int i = 0; i < positions.length; i++) {
            this.generate(positions[i], tiles[i]);
        }
    }

    /**
     * Fired to create a new {@link IFarGeneratorRough}.
     *
     * @author DaPorkchop_
     */
    interface CreationEvent extends IFarServerResourceCreationEvent<IFarGeneratorRough> {
        /**
         * @return the {@link IFarTileProvider} which the {@link IFarGeneratorRough} will be created for
         */
        IFarTileProvider provider();
    }
}
