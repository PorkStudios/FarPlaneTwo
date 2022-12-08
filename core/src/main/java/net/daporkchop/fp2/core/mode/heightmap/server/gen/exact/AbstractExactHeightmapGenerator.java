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
 */

package net.daporkchop.fp2.core.mode.heightmap.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

import java.util.Arrays;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactHeightmapGenerator extends AbstractFarGenerator<HeightmapPos, HeightmapTile> implements IFarGeneratorExact<HeightmapPos, HeightmapTile> {
    public AbstractExactHeightmapGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider<HeightmapPos, HeightmapTile> provider) {
        super(world, provider);
    }

    protected void computeElevations(@NonNull FBlockLevel world, @NonNull int[] elevations, int blockX, int blockZ) {
        //TODO: this is absolutely disgusting!
        world.getNextTypeTransitions(Direction.NEGATIVE_Y, blockX, Integer.MAX_VALUE, blockZ, Integer.MAX_VALUE * 2L,
                Arrays.asList(
                        TypeTransitionFilter.builder()
                                .fromTypes(blockTypeFlag(BLOCK_TYPE_INVISIBLE))
                                .toTypes(blockTypeFlag(BLOCK_TYPE_TRANSPARENT))
                                .disableAfterHitCount(elevations.length - 1).abortAfterHitCount(-1)
                                .build(),
                        TypeTransitionFilter.builder()
                                .fromTypes(allBlockTypes() & ~blockTypeFlag(BLOCK_TYPE_OPAQUE))
                                .toTypes(blockTypeFlag(BLOCK_TYPE_OPAQUE))
                                .disableAfterHitCount(-1).abortAfterHitCount(1)
                                .build()
                ),
                new TypeTransitionSingleOutput.BandArrays(
                        null, 0, 0,
                        null, 0, 0,
                        elevations, 0, 1,
                        null, 0, 0,
                        null, 0, 0,
                        elevations.length));
    }

    @Override
    public void generate(@NonNull FBlockLevel world, @NonNull HeightmapPos posIn, @NonNull HeightmapTile tile) throws GenerationNotAllowedException {
        int tileX = posIn.x();
        int tileZ = posIn.z();

        HeightmapData data = new HeightmapData();
        int[] elevations = new int[MAX_LAYERS];

        for (int x = 0; x < HT_VOXELS; x++) {
            for (int z = 0; z < HT_VOXELS; z++) {
                Arrays.fill(elevations, Integer.MIN_VALUE);

                int blockX = tileX * HT_VOXELS + x;
                int blockZ = tileZ * HT_VOXELS + z;
                this.computeElevations(world, elevations, blockX, blockZ);

                for (int layer = 0; layer < MAX_LAYERS; layer++) {
                    int elevation = elevations[layer];
                    if (elevation != Integer.MIN_VALUE) {
                        data.state = world.getState(blockX, elevation, blockZ);
                        data.biome = world.getBiome(blockX, elevation, blockZ);
                        data.height_int = elevation + 1;
                        data.light = world.getLight(blockX, elevation + 1, blockZ) & 0xFF;

                        if (layer == WATER_LAYER) {
                            data.height_frac = HEIGHT_FRAC_LIQUID;
                            data.secondaryConnection = WATER_LAYER;
                        } else {
                            data.height_frac = 0;
                            data.secondaryConnection = DEFAULT_LAYER;
                        }

                        tile.setLayer(x, z, layer, data);
                    }
                }
            }
        }
    }
}
