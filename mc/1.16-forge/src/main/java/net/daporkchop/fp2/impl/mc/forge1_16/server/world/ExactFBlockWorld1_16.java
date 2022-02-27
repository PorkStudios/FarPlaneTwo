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

package net.daporkchop.fp2.impl.mc.forge1_16.server.world;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ServerWorld;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class ExactFBlockWorld1_16 implements FBlockWorld {
    public static final int CHUNK_SHIFT = 4;
    public static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;
    public static final int CHUNK_MASK = CHUNK_SIZE - 1;

    @NonNull
    protected final ServerWorld world;
    @NonNull
    protected final FGameRegistry registry;

    @Override
    public void close() {
        //no-op
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return true; //TODO: this means exact generation will always be used!!!
    }

    @Override
    public int getState(int x, int y, int z) {
        //delegate to bulk getter because it'll delegate to the server thread using fp2's WorkerManager, avoiding a potential deadlock when shutting down
        int[] buf = new int[1];
        this.getStates(buf, 0, 1, x, y, z, 1, 1, 1, 1, 1, 1);
        return buf[0];
    }

    @Override
    public int getBiome(int x, int y, int z) {
        //delegate to bulk getter because it'll delegate to the server thread using fp2's WorkerManager, avoiding a potential deadlock when shutting down
        int[] buf = new int[1];
        this.getBiomes(buf, 0, 1, x, y, z, 1, 1, 1, 1, 1, 1);
        return buf[0];
    }

    @Override
    public byte getLight(int x, int y, int z) {
        //delegate to bulk getter because it'll delegate to the server thread using fp2's WorkerManager, avoiding a potential deadlock when shutting down
        byte[] buf = new byte[1];
        this.getLights(buf, 0, 1, x, y, z, 1, 1, 1, 1, 1, 1);
        return buf[0];
    }

    @Override
    public void getData(int[] states, int statesOff, int statesStride,
                        int[] biomes, int biomesOff, int biomesStride,
                        byte[] light, int lightOff, int lightStride,
                        int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        int count = positive(sizeX, "sizeX") * positive(sizeY, "sizeY") * positive(sizeZ, "sizeZ");
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        positive(strideX, "strideX");
        positive(strideY, "strideY");
        positive(strideZ, "strideZ");

        //prefetch all affected sections
        Consumer<IntConsumer> chunkXSupplier = this.chunkCoordSupplier(x, sizeX, strideX);
        Consumer<IntConsumer> sectionYSupplier = this.chunkCoordSupplier(y, sizeY, strideY);
        Consumer<IntConsumer> chunkZSupplier = this.chunkCoordSupplier(z, sizeZ, strideZ);

        LongList sectionPositions = new LongArrayList();
        chunkXSupplier.accept(chunkX -> sectionYSupplier.accept(sectionY -> chunkZSupplier.accept(chunkZ -> sectionPositions.add(SectionPos.asLong(chunkX, sectionY, chunkZ)))));

        //delegate to PrefetchedChunksFBlockWorld1_16
        PrefetchedChunksFBlockWorld1_16.prefetchSections(this.world, this.registry, sectionPositions)
                .join()
                .getData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    protected Consumer<IntConsumer> chunkCoordSupplier(int base, int size, int stride) {
        if (stride >= CHUNK_SIZE) {
            return callback -> {
                for (int i = 0, block = base; i < size; i++, block += stride) {
                    callback.accept(block >> CHUNK_SHIFT);
                }
            };
        } else {
            return callback -> {
                for (int chunk = base >> CHUNK_SHIFT, limit = (base + size * stride - 1) >> CHUNK_SHIFT; chunk <= limit; chunk++) {
                    callback.accept(chunk);
                }
            };
        }
    }

    @Override
    public void getData(int[] states, int statesOff, int statesStride,
                        int[] biomes, int biomesOff, int biomesStride,
                        byte[] light, int lightOff, int lightStride,
                        @NonNull int[] xs, int xOff, int xStride, @NonNull int[] ys, int yOff, int yStride, @NonNull int[] zs, int zOff, int zStride, int count) {
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        checkRangeLen(xs.length, xOff, positive(xStride, "xStride") * count);
        checkRangeLen(ys.length, yOff, positive(yStride, "yStride") * count);
        checkRangeLen(zs.length, zOff, positive(zStride, "zStride") * count);

        //find unique section positions intersected by this query
        try (NDimensionalIntSet sectionPositions = Datastructures.INSTANCE.nDimensionalIntSet().dimensions(3).build()) {
            for (int i = 0, xIndex = xOff, yIndex = yOff, zIndex = zStride; i < count; i++, xIndex += xStride, yIndex += yStride, zIndex += zStride) {
                sectionPositions.add(xs[xIndex] >> CHUNK_SHIFT, ys[yIndex] >> CHUNK_SHIFT, zs[zIndex] >> CHUNK_SHIFT);
            }

            //delegate to PrefetchedChunksFBlockWorld1_16
            PrefetchedChunksFBlockWorld1_16.prefetchSections(this.world, this.registry, sectionPositions)
                    .join()
                    .getData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
        }
    }
}
