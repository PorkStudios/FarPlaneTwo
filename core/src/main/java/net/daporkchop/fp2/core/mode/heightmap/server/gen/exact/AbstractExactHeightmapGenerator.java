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

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.BatchDataQuery;
import net.daporkchop.fp2.api.world.level.query.BatchTypeTransitionQuery;
import net.daporkchop.fp2.api.world.level.query.DataQueryBatchOutput;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionBatchOutput;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactHeightmapGenerator extends AbstractFarGenerator<HeightmapPos, HeightmapTile> implements IFarGeneratorExact<HeightmapPos, HeightmapTile> {
    private static final List<TypeTransitionFilter> TYPE_TRANSITION_FILTERS = ImmutableList.of(
            TypeTransitionFilter.builder()
                    .fromTypes(blockTypeFlag(BLOCK_TYPE_INVISIBLE))
                    .toTypes(blockTypeFlag(BLOCK_TYPE_TRANSPARENT))
                    .disableAfterHitCount(MAX_LAYERS - 1).abortAfterHitCount(-1)
                    .build(),
            TypeTransitionFilter.builder()
                    .fromTypes(allBlockTypes() & ~blockTypeFlag(BLOCK_TYPE_OPAQUE))
                    .toTypes(blockTypeFlag(BLOCK_TYPE_OPAQUE))
                    .disableAfterHitCount(-1).abortAfterHitCount(1)
                    .build());

    public AbstractExactHeightmapGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider<HeightmapPos, HeightmapTile> provider) {
        super(world, provider);
    }

    @Override
    public void generate(@NonNull FBlockLevel world, @NonNull HeightmapPos posIn, @NonNull HeightmapTile tile) throws GenerationNotAllowedException {
        int baseX = posIn.x() << HT_SHIFT;
        int baseZ = posIn.z() << HT_SHIFT;

        ArrayAllocator<int[]> intAlloc = ALLOC_INT.get();
        ArrayAllocator<byte[]> byteAlloc = ALLOC_BYTE.get();

        int[] lengths = intAlloc.atLeast(HT_VOXELS * HT_VOXELS);
        int[] elevations = intAlloc.atLeast(MAX_LAYERS * HT_VOXELS * HT_VOXELS);
        byte[] typeTransitions = byteAlloc.atLeast(MAX_LAYERS * HT_VOXELS * HT_VOXELS);

        //query type transitions
        TypeTransitionBatchOutput typeTransitionBatchOutput = new TypeTransitionBatchOutput.BandArrays(
                lengths, 0, 1,
                typeTransitions, 0, MAX_LAYERS, 1,
                null, 0, 0, 0,
                elevations, 0, MAX_LAYERS, 1,
                null, 0, 0, 0,
                null, 0, 0, 0,
                HT_VOXELS * HT_VOXELS, MAX_LAYERS);

        world.query(BatchTypeTransitionQuery.of(
                Direction.NEGATIVE_Y, Integer.MAX_VALUE * 2L, TYPE_TRANSITION_FILTERS,
                new PointsQueryShape.OriginSizeStride(baseX, Integer.MAX_VALUE, baseZ, HT_VOXELS, 1, HT_VOXELS, 1, 1, 1),
                typeTransitionBatchOutput));

        //determine block data at the relevant positions
        final int BLOCK_SAMPLE_OFFSET_INDEX_LO = 0;
        final int BLOCK_SAMPLE_OFFSET_INDEX_HI = 1;
        final int BLOCK_SAMPLE_STRIDE_PER_LAYER = 2;
        int[] states = intAlloc.atLeast(HT_VOXELS * HT_VOXELS * MAX_LAYERS * BLOCK_SAMPLE_STRIDE_PER_LAYER);
        int[] biomes = intAlloc.atLeast(HT_VOXELS * HT_VOXELS * MAX_LAYERS * BLOCK_SAMPLE_STRIDE_PER_LAYER);
        byte[] lights = byteAlloc.atLeast(HT_VOXELS * HT_VOXELS * MAX_LAYERS * BLOCK_SAMPLE_STRIDE_PER_LAYER);

        {
            //allocate arrays for coordinates and fill them with a bunch of positions: we want to get data for the blocks at all the queried transition points, as well as
            // one block above.
            int samplePoints = 0;
            int[] xs = intAlloc.atLeast(HT_VOXELS * HT_VOXELS * MAX_LAYERS * BLOCK_SAMPLE_STRIDE_PER_LAYER);
            int[] ys = intAlloc.atLeast(HT_VOXELS * HT_VOXELS * MAX_LAYERS * BLOCK_SAMPLE_STRIDE_PER_LAYER);
            int[] zs = intAlloc.atLeast(HT_VOXELS * HT_VOXELS * MAX_LAYERS * BLOCK_SAMPLE_STRIDE_PER_LAYER);
            for (int slot = 0, x = 0; x < HT_VOXELS; x++) {
                for (int z = 0; z < HT_VOXELS; z++, slot++) {
                    int blockX = baseX + x;
                    int blockZ = baseZ + z;
                    for (int layer = 0, slotLength = typeTransitionBatchOutput.getLength(slot); layer < slotLength; layer++, samplePoints += BLOCK_SAMPLE_STRIDE_PER_LAYER) {
                        int elevation = typeTransitionBatchOutput.getY(slot, layer);
                        xs[samplePoints + BLOCK_SAMPLE_OFFSET_INDEX_LO] = blockX;
                        xs[samplePoints + BLOCK_SAMPLE_OFFSET_INDEX_HI] = blockX;
                        ys[samplePoints + BLOCK_SAMPLE_OFFSET_INDEX_LO] = elevation;
                        ys[samplePoints + BLOCK_SAMPLE_OFFSET_INDEX_HI] = elevation + 1;
                        zs[samplePoints + BLOCK_SAMPLE_OFFSET_INDEX_LO] = blockZ;
                        zs[samplePoints + BLOCK_SAMPLE_OFFSET_INDEX_HI] = blockZ;
                    }
                }
            }

            try {
                world.query(BatchDataQuery.of(
                        new PointsQueryShape.Multi(xs, 0, 1, ys, 0, 1, zs, 0, 1, samplePoints),
                        new DataQueryBatchOutput.BandArrays(states, 0, 1, biomes, 0, 1, lights, 0, 1, samplePoints)));
            } catch (GenerationNotAllowedException e) {
                //the query should never fail with GenerationNotAllowedException, because all the positions we're querying are already known to exist. if it does fail,
                // it's almost certainly a bug and we'll throw a different type of exception to avoid safely retrying.
                throw new IllegalStateException(e);
            }

            intAlloc.release(xs);
            intAlloc.release(ys);
            intAlloc.release(zs);
        }

        HeightmapData data = new HeightmapData();
        FExtendedStateRegistryData extendedStateRegistryData = world.registry().extendedStateRegistryData();

        for (int slot = 0, blockSampleIndex = 0, x = 0; x < HT_VOXELS; x++) {
            for (int z = 0; z < HT_VOXELS; z++, slot++) {
                assert DEFAULT_LAYER == 0 && WATER_LAYER == 1; //we assume this is the case

                boolean foundLiquid = false;
                int remainingLayersAllocator = 2;

                for (int layer = 0, slotLength = typeTransitionBatchOutput.getLength(slot); layer < slotLength; layer++, blockSampleIndex += BLOCK_SAMPLE_STRIDE_PER_LAYER) {
                    int elevation = typeTransitionBatchOutput.getY(slot, layer);

                    data.state = states[blockSampleIndex + BLOCK_SAMPLE_OFFSET_INDEX_LO];
                    data.biome = biomes[blockSampleIndex + BLOCK_SAMPLE_OFFSET_INDEX_LO];
                    data.height_int = elevation + 1;
                    data.light = lights[blockSampleIndex + BLOCK_SAMPLE_OFFSET_INDEX_HI] & 0xFF;

                    byte transition = typeTransitionBatchOutput.getTypeTransition(slot, layer);
                    int tileLayer;
                    switch (getTypeTransitionToType(transition)) {
                        case BLOCK_TYPE_TRANSPARENT:
                            if (hasAnyStateFlags(extendedStateRegistryData.stateInfo(data.state), STATE_FLAG_LIQUID)) {
                                if (!foundLiquid) { //the first liquid we encounter goes on the water layer
                                    foundLiquid = true;
                                    tileLayer = WATER_LAYER;
                                } else if ((tileLayer = remainingLayersAllocator++) >= MAX_LAYERS) {
                                    continue;
                                }

                                data.secondaryConnection = tileLayer;
                                data.height_frac = HEIGHT_FRAC_LIQUID;
                            } else {
                                if ((tileLayer = remainingLayersAllocator++) >= MAX_LAYERS) {
                                    continue;
                                }

                                data.secondaryConnection = DEFAULT_LAYER;
                                data.height_frac = 0;
                            }
                            break;
                        case BLOCK_TYPE_OPAQUE:
                            assert layer + 1 == slotLength : "opaque block isn't the last block in the array?";
                            tileLayer = DEFAULT_LAYER;
                            data.secondaryConnection = DEFAULT_LAYER;
                            data.height_frac = 0;
                            break;
                        default:
                            throw new IllegalStateException("unexpected type transition " + getTypeTransitionToType(transition));
                    }

                    tile.setLayer(x, z, tileLayer, data);
                }
            }
        }

        byteAlloc.release(lights);
        intAlloc.release(biomes);
        intAlloc.release(states);
        byteAlloc.release(typeTransitions);
        intAlloc.release(lengths);
        intAlloc.release(elevations);
    }
}
