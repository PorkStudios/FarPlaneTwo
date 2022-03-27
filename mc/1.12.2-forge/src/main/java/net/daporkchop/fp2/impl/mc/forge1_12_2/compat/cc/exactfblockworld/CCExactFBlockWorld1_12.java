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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.exactfblockworld;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class CCExactFBlockWorld1_12 implements FBlockWorld {
    public static final int CUBE_SHIFT = 4;
    public static final int CUBE_SIZE = 1 << CUBE_SHIFT;
    public static final int CUBE_MASK = CUBE_SIZE - 1;

    @NonNull
    protected final CCExactFBlockWorldHolder1_12 holder;
    protected final boolean generationAllowed;

    @Override
    public void close() {
        //no-op, all resources are owned by CCExactFBlockWorldHolder1_12
    }

    @Override
    public FGameRegistry registry() {
        return GameRegistry1_12_2.get();
    }

    @Override
    public IntAxisAlignedBB dataLimits() {
        return this.holder.farWorld.fp2_IFarWorld_coordLimits();
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public int getState(int x, int y, int z) throws GenerationNotAllowedException {
        //delegate to bulk getter because it'll delegate to PrefetchedCubesCCFBlockWorld1_12, which can access neighboring chunks if Block#getActualState accesses a
        //  state which goes over a cube/column border
        int[] buf = new int[1];
        this.getData(buf, 0, 1, null, 0, 0, null, 0, 0, x, y, z, 1, 1, 1, 1, 1, 1);
        return buf[0];
    }

    @Override
    public int getBiome(int x, int y, int z) throws GenerationNotAllowedException {
        //delegate to bulk getter because it'll delegate to PrefetchedCubesCCFBlockWorld1_12, which can access neighboring chunks if Block#getActualState accesses a
        //  state which goes over a cube/column border
        int[] buf = new int[1];
        this.getData(null, 0, 0, buf, 0, 1, null, 0, 0, x, y, z, 1, 1, 1, 1, 1, 1);
        return buf[0];
    }

    @Override
    public byte getLight(int x, int y, int z) throws GenerationNotAllowedException {
        //delegate to bulk getter because it'll delegate to PrefetchedCubesCCFBlockWorld1_12, which can access neighboring chunks if Block#getActualState accesses a
        //  state which goes over a cube/column border
        byte[] buf = new byte[1];
        this.getData(null, 0, 0, null, 0, 0, buf, 0, 1, x, y, z, 1, 1, 1, 1, 1, 1);
        return buf[0];
    }

    @Override
    public void getData(int[] states, int statesOff, int statesStride, int[] biomes, int biomesOff, int biomesStride, byte[] light, int lightOff, int lightStride, int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) throws GenerationNotAllowedException {
        BlockWorldConstants.validateArgsForGetData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);

        //prefetch all affected cubes
        Consumer<IntConsumer> cubeXSupplier = this.cubeCoordSupplier(x, sizeX, strideX);
        Consumer<IntConsumer> cubeYSupplier = this.cubeCoordSupplier(y, sizeY, strideY);
        Consumer<IntConsumer> cubeZSupplier = this.cubeCoordSupplier(z, sizeZ, strideZ);

        List<CubePos> cubePositions = new ArrayList<>();
        cubeXSupplier.accept(cubeX -> cubeYSupplier.accept(cubeY -> cubeZSupplier.accept(cubeZ -> cubePositions.add(new CubePos(cubeX, cubeY, cubeZ)))));

        //delegate to PrefetchedCubesCCFBlockWorld1_12
        PrefetchedCubesCCFBlockWorld1_12.prefetchCubes(this.holder, this.generationAllowed, cubePositions)
                .getData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    protected Consumer<IntConsumer> cubeCoordSupplier(int base, int size, int stride) {
        if (stride >= Coords.cubeToMinBlock(1)) {
            return callback -> {
                for (int i = 0, block = base; i < size; i++, block += stride) {
                    callback.accept(Coords.blockToCube(block));
                }
            };
        } else {
            return callback -> {
                for (int cube = Coords.blockToCube(base), limit = Coords.blockToCube(base + size * stride - 1); cube <= limit; cube++) {
                    callback.accept(cube);
                }
            };
        }
    }

    @Override
    public void getData(int[] states, int statesOff, int statesStride, int[] biomes, int biomesOff, int biomesStride, byte[] light, int lightOff, int lightStride, @NonNull int[] xs, int xOff, int xStride, @NonNull int[] ys, int yOff, int yStride, @NonNull int[] zs, int zOff, int zStride, int count) throws GenerationNotAllowedException {
        BlockWorldConstants.validateArgsForGetData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);

        //find unique cube positions intersected by this query
        try (NDimensionalIntSet cubePositions = Datastructures.INSTANCE.nDimensionalIntSet().dimensions(3).build()) {
            for (int i = 0, xIndex = xOff, yIndex = yOff, zIndex = zStride; i < count; i++, xIndex += xStride, yIndex += yStride, zIndex += zStride) {
                cubePositions.add(Coords.blockToCube(xs[xIndex]), Coords.blockToCube(ys[yIndex]), Coords.blockToCube(zs[zIndex]));
            }

            //delegate to PrefetchedCubesCCFBlockWorld1_12
            PrefetchedCubesCCFBlockWorld1_12.prefetchCubes(this.holder, this.generationAllowed, cubePositions)
                    .getData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
        }
    }
}
