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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat;

import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IPaddedLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.world.gen.layer.GenLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static net.daporkchop.fp2.core.util.math.MathUtil.mulAddShift;
import static net.daporkchop.lib.common.util.PValidation.checkArg;
import static net.daporkchop.lib.common.util.PValidation.checkState;

/**
 * @author DaPorkchop_
 */
public class CompatPaddedLayerWrapper extends AbstractFastLayer implements IPaddedLayer, ICompatLayer {
    protected final Cached<EmulatedParent> wrappedGenLayerCache;

    public CompatPaddedLayerWrapper(@NonNull GenLayer wrappedLayer) {
        super(((ATGenLayer1_12) wrappedLayer).getWorldGenSeed());
        this.wrappedGenLayerCache = Cached.threadLocal(() -> new EmulatedParent(wrappedLayer), ReferenceStrength.SOFT);
    }

    @Override
    public boolean shouldResetIntCacheAfterGet() {
        return true;
    }

    @Override
    public int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        try (EmulatedParent emulatedParent = this.wrappedGenLayerCache.get().begin(alloc)) {
            int[] buffer = emulatedParent.atLeast(3 * 3);
            this.child().getGrid(alloc, x - 1, z - 1, 3, 3, buffer);

            emulatedParent.setBuffer(x - 1, z - 1, 3, 3, buffer, 0);
            return emulatedParent.invokeWrappedLevel(x, z, 1, 1, x - 1, z - 1, 3, 3)[0];
        }
    }

    @Override
    public void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        try (EmulatedParent emulatedParent = this.wrappedGenLayerCache.get().begin(alloc)) {
            emulatedParent.setBuffer(x - 1, z - 1, sizeX + 2, sizeZ + 2, in, 0);
            int[] wrappedResult = emulatedParent.invokeWrappedLevel(x, z, sizeX, sizeZ, x - 1, z - 1, sizeX + 2, sizeZ + 2);

            //read source elements in vanilla ZX coordinate order and write them to the output array in fp2 XZ coordinate order
            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    out[i++] = wrappedResult[dx + dz * sizeX];
                }
            }
        }
    }

    @Override
    public void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int inSize = (((dist >> depth) + 1) * count) + 2;
        final int mask = (depth != 0) ? 1 : 0;

        try (EmulatedParent emulatedParent = this.wrappedGenLayerCache.get().begin(alloc)) {
            emulatedParent.setBuffer((x >> depth) - 1, (z >> depth) - 1, inSize, inSize, in, 0);

            for (int outIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++) {
                    final int baseX = mulAddShift(gridX, dist, x, depth);
                    final int baseZ = mulAddShift(gridZ, dist, z, depth);
                    final int offsetX = mulAddShift(gridX, dist, gridX & mask, depth);
                    final int offsetZ = mulAddShift(gridZ, dist, gridZ & mask, depth);

                    int[] wrappedResult = emulatedParent.invokeWrappedLevel(baseX, baseZ, size, size, baseX - 1, baseZ - 1, size + 2, size + 2);

                    //read source elements in vanilla ZX coordinate order and write them to the output array in fp2 XZ coordinate order
                    for (int dx = 0; dx < size; dx++) {
                        for (int dz = 0; dz < size; dz++, outIdx++) {
                            out[outIdx] = wrappedResult[dx + dz * size];
                        }
                    }
                }
            }
        }
    }

    @Override
    public void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int inSize = size + 2;

        try (EmulatedParent emulatedParent = this.wrappedGenLayerCache.get().begin(alloc)) {
            for (int outIdx = 0, inBase = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++, inBase += inSize * inSize) {
                    final int baseX = mulAddShift(gridX, dist, x, depth);
                    final int baseZ = mulAddShift(gridZ, dist, z, depth);

                    emulatedParent.setBuffer(baseX - 1, baseZ - 1, inSize, inSize, in, inBase);
                    int[] wrappedResult = emulatedParent.invokeWrappedLevel(baseX, baseZ, size, size, baseX - 1, baseZ - 1, size + 2, size + 2);

                    //read source elements in vanilla ZX coordinate order and write them to the output array in fp2 XZ coordinate order
                    for (int dx = 0; dx < size; dx++) {
                        for (int dz = 0; dz < size; dz++, outIdx++) {
                            out[outIdx] = wrappedResult[dx + dz * size];
                        }
                    }
                }
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class EmulatedParent extends GenLayer implements AutoCloseable {
        protected final GenLayer wrappedLayer;

        protected ArrayAllocator<int[]> currentAlloc;
        protected final List<int[]> currentArrays = new ArrayList<>();

        protected int expectedAreaX;
        protected int expectedAreaZ;
        protected int expectedAreaWidth;
        protected int expectedAreaHeight;
        protected boolean visited;

        protected int[] bufferData;
        protected int bufferOffset;
        protected int bufferAreaX;
        protected int bufferAreaZ;
        protected int bufferAreaWidth;
        protected int bufferAreaHeight;

        public EmulatedParent(@NonNull GenLayer wrappedLayer) {
            super(-1L);
            ((ATGenLayer1_12) this).setWorldGenSeed(((ATGenLayer1_12) ((ATGenLayer1_12) wrappedLayer).getParent()).getWorldGenSeed());

            this.wrappedLayer = BiomeHelper.cloneLayer(wrappedLayer, new GenLayer[]{this});
        }

        public EmulatedParent begin(@NonNull ArrayAllocator<int[]> alloc) {
            this.currentAlloc = alloc;
            return this;
        }

        public int[] atLeast(int length) {
            int[] arr = this.currentAlloc.atLeast(length);
            this.currentArrays.add(arr);
            return arr;
        }

        public EmulatedParent setBuffer(int areaX, int areaZ, int areaWidth, int areaHeight, int[] buffer, int offset) {
            this.bufferData = buffer;
            this.bufferOffset = offset;
            this.bufferAreaX = areaX;
            this.bufferAreaZ = areaZ;
            this.bufferAreaWidth = areaWidth;
            this.bufferAreaHeight = areaHeight;
            return this;
        }

        @Override
        public void close() {
            ArrayAllocator<int[]> alloc = this.currentAlloc;
            this.currentAlloc = null;

            this.bufferData = null;

            for (int[] currentArray : this.currentArrays) {
                alloc.release(currentArray);
            }
            this.currentArrays.clear();
        }

        public int[] invokeWrappedLevel(int areaX, int areaZ, int areaWidth, int areaHeight, int expectedAreaX, int expectedAreaZ, int expectedAreaWidth, int expectedAreaHeight) {
            this.expectedAreaX = expectedAreaX;
            this.expectedAreaZ = expectedAreaZ;
            this.expectedAreaWidth = expectedAreaWidth;
            this.expectedAreaHeight = expectedAreaHeight;
            this.visited = false;

            int[] result = this.wrappedLayer.getInts(areaX, areaZ, areaWidth, areaHeight);
            checkState(this.visited, "wrapped layer didn't access its parent!");
            return result;
        }

        @Override
        public int[] getInts(int areaX, int areaZ, int areaWidth, int areaHeight) {
            checkState(areaX == this.expectedAreaX && areaZ == this.expectedAreaZ && areaWidth == this.expectedAreaWidth && areaHeight == this.expectedAreaHeight,
                    "wrapped GenLayer didn't access the expected area from its parent layer!");

            checkState(!this.visited, "wrapped GenLayer already accessed its parent!");
            this.visited = true;

            checkArg(areaX >= this.bufferAreaX && areaZ >= this.bufferAreaZ
                    && areaX + areaWidth <= this.bufferAreaX + this.bufferAreaWidth && areaZ + areaHeight <= this.bufferAreaZ + this.bufferAreaHeight);

            int[] out = this.atLeast(areaWidth * areaHeight);
            //this.currentState.getInts(areaX, areaZ, areaWidth, areaHeight, out);

            //read source elements in fp2 XZ coordinate order and write them to the output array in vanilla ZX coordinate order
            for (int i = 0, dz = 0; dz < areaHeight; dz++) {
                for (int dx = 0; dx < areaWidth; dx++) {
                    out[i++] = this.bufferData[this.bufferOffset + (areaX - this.bufferAreaX + dx) * this.bufferAreaHeight + (areaZ - this.bufferAreaZ + dz)];
                }
            }
            return out;
        }
    }
}
