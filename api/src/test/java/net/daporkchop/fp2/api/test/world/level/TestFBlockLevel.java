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

package net.daporkchop.fp2.api.test.world.level;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.registry.FExtendedBiomeRegistryData;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.stream.IntStream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestFBlockLevel {
    //
    // default implementation of getNextTypeTransitions()
    //

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NEGATIVE_X", "NEGATIVE_Y", "NEGATIVE_Z"})
    public void testGetNextTypeTransitions_sampleCount_nearDataLimits(Direction direction) {
        //make sure that we take exactly the right number of samples when close to the level's data limits

        TypeTransitionSingleOutput output = TypeTransitionSingleOutput.BandArrays.createWithCount(64);

        int fx = abs(direction.x());
        int fy = abs(direction.y());
        int fz = abs(direction.z());

        try (FBlockLevel level = this.dummyLevelOpaque(new IntAxisAlignedBB(-16, -16, -16, 16, 16, 16))) {
            //upper edge
            assertEquals(0, level.getNextTypeTransitions(direction, 20 * fx, 20 * fy, 20 * fz, 3L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, 20 * fx, 20 * fy, 20 * fz, 4L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, 20 * fx, 20 * fy, 20 * fz, 5L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, 16 * fx, 16 * fy, 16 * fz, 5L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, 16 * fx, 16 * fy, 16 * fz, 1L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, 16 * fx, 16 * fy, 16 * fz, 0L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, 15 * fx, 15 * fy, 15 * fz, 5L, outputEverythingFilterList(), output, , ));

            //in the top and out the bottom
            assertEquals(2, level.getNextTypeTransitions(direction, 20 * fx, 20 * fy, 20 * fz, Long.MAX_VALUE, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, 15 * fx, 15 * fy, 15 * fz, Long.MAX_VALUE, outputEverythingFilterList(), output, , ));

            //lower edge
            assertEquals(0, level.getNextTypeTransitions(direction, -10 * fx, -10 * fy, -10 * fz, 5L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, -10 * fx, -10 * fy, -10 * fz, 6L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, -10 * fx, -10 * fy, -10 * fz, 7L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, -15 * fx, -15 * fy, -15 * fz, 1L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, -15 * fx, -15 * fy, -15 * fz, 2L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(direction, -16 * fx, -16 * fy, -16 * fz, 1L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, -16 * fx, -16 * fy, -16 * fz, 0L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(direction, -17 * fx, -17 * fy, -17 * fz, Long.MAX_VALUE, outputEverythingFilterList(), output, , ));
        }
    }

    @Test
    public void testGetNextTypeTransitions_overflow_vertical() {
        //make sure we take exactly the right number of samples when the coordinates are very big and would cause overflows

        TypeTransitionSingleOutput output = TypeTransitionSingleOutput.BandArrays.createWithCount(64);

        try (FBlockLevel level = this.dummyLevelCheckerboard(new IntAxisAlignedBB(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -10, -100, 100, 10))) {
            //jumping down to lower coordinates, jump distance just within the int limit
            assertEquals(0, level.getNextTypeTransitions(Direction.NEGATIVE_Y, -101, Integer.MAX_VALUE, 0, Integer.MAX_VALUE - 101L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.NEGATIVE_Y, -101, Integer.MAX_VALUE, 0, Integer.MAX_VALUE - 100L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(Direction.NEGATIVE_Y, -101, Integer.MAX_VALUE, 0, Integer.MAX_VALUE - 99L, outputEverythingFilterList(), output, , ));
            assertEquals(2, level.getNextTypeTransitions(Direction.NEGATIVE_Y, -101, Integer.MAX_VALUE, 0, Integer.MAX_VALUE - 98L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.NEGATIVE_Y, -101, Integer.MAX_VALUE, 11, Integer.MAX_VALUE - 98L, outputEverythingFilterList(), output, , ));
        }

        try (FBlockLevel level = this.dummyLevelCheckerboard(new IntAxisAlignedBB(100, -100, -10, Integer.MAX_VALUE, Integer.MAX_VALUE - 1, 10))) {
            //jumping up to higher coordinates, jump distance just within the int limit
            assertEquals(0, level.getNextTypeTransitions(Direction.POSITIVE_Y, 101, Integer.MIN_VALUE, 0, Integer.MAX_VALUE - 101L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.POSITIVE_Y, 101, Integer.MIN_VALUE, 0, Integer.MAX_VALUE - 100L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(Direction.POSITIVE_Y, 101, Integer.MIN_VALUE, 0, Integer.MAX_VALUE - 99L, outputEverythingFilterList(), output, , ));
            assertEquals(2, level.getNextTypeTransitions(Direction.POSITIVE_Y, 101, Integer.MIN_VALUE, 0, Integer.MAX_VALUE - 98L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.POSITIVE_Y, 101, Integer.MIN_VALUE, 11, Integer.MAX_VALUE - 98L, outputEverythingFilterList(), output, , ));
        }
    }

    @Test
    public void testGetNextTypeTransitions_overflow_horizontal() {
        //make sure we take exactly the right number of samples when the coordinates are very big and would cause overflows

        TypeTransitionSingleOutput output = TypeTransitionSingleOutput.BandArrays.createWithCount(64);

        try (FBlockLevel level = this.dummyLevelCheckerboard(new IntAxisAlignedBB(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -10, -100, 100, 10))) {
            //jumping down to lower coordinates, jump distance just beyond the int limit
            assertEquals(0, level.getNextTypeTransitions(Direction.NEGATIVE_X, Integer.MAX_VALUE, 0, 0, Integer.MAX_VALUE + 99L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.NEGATIVE_X, Integer.MAX_VALUE, 0, 0, Integer.MAX_VALUE + 99L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(Direction.NEGATIVE_X, Integer.MAX_VALUE, 0, 0, Integer.MAX_VALUE + 101L, outputEverythingFilterList(), output, , ));
            assertEquals(2, level.getNextTypeTransitions(Direction.NEGATIVE_X, Integer.MAX_VALUE, 0, 0, Integer.MAX_VALUE + 102L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.NEGATIVE_X, Integer.MAX_VALUE, 0, 11, Integer.MAX_VALUE + 102L, outputEverythingFilterList(), output, , ));
        }

        try (FBlockLevel level = this.dummyLevelCheckerboard(new IntAxisAlignedBB(100, -100, -10, Integer.MAX_VALUE, Integer.MAX_VALUE - 1, 10))) {
            //jumping up to higher coordinates, jump distance just within the int limit
            assertEquals(0, level.getNextTypeTransitions(Direction.POSITIVE_X, Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE + 99L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.POSITIVE_X, Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE + 99L, outputEverythingFilterList(), output, , ));
            assertEquals(1, level.getNextTypeTransitions(Direction.POSITIVE_X, Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE + 101L, outputEverythingFilterList(), output, , ));
            assertEquals(2, level.getNextTypeTransitions(Direction.POSITIVE_X, Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE + 102L, outputEverythingFilterList(), output, , ));
            assertEquals(0, level.getNextTypeTransitions(Direction.POSITIVE_X, Integer.MIN_VALUE, 0, 11, Integer.MAX_VALUE + 102L, outputEverythingFilterList(), output, , ));
        }
    }

    protected FBlockLevel dummyLevelOpaque(IntAxisAlignedBB bounds) {
        return new AbstractDummyFBlockLevel(bounds, this.alwaysOpaque());
    }

    protected FBlockLevel dummyLevelCheckerboard(IntAxisAlignedBB bounds) {
        return new AbstractDummyFBlockLevel(bounds, this.checkerboard());
    }

    @FunctionalInterface
    protected interface IntIntIntToIntFunction {
        int apply(int x, int y, int z);
    }

    protected IntIntIntToIntFunction alwaysOpaque() {
        return (x, y, z) -> BLOCK_TYPE_OPAQUE;
    }

    protected IntIntIntToIntFunction checkerboard() {
        //noinspection ConstantConditions
        assert BLOCK_TYPE_OPAQUE == BLOCK_TYPE_TRANSPARENT + 1; //check this just in case i change the constants in the future

        return (x, y, z) -> ((x ^ y ^ z) & 1) + BLOCK_TYPE_TRANSPARENT;
    }

    @RequiredArgsConstructor
    @Getter
    protected static class AbstractDummyFBlockLevel implements FBlockLevel {
        @NonNull
        private final IntAxisAlignedBB dataLimits;
        @NonNull
        private final IntIntIntToIntFunction blockFunction;

        @Override
        public void close() {
            //no-op
        }

        @Override
        public FGameRegistry registry() {
            return DummyTypeRegistry.INSTANCE;
        }

        @Override
        public boolean generationAllowed() {
            return false;
        }

        @Override
        public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            return this.dataLimits.intersects(minX, minY, minZ, maxX, maxY, maxZ);
        }

        @Override
        public boolean containsAnyData(@NonNull IntAxisAlignedBB bb) {
            return this.dataLimits.intersects(bb);
        }

        @Override
        public int getState(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
            return this.dataLimits.contains(x, y, z) ? this.blockFunction.apply(x, y, z) : BLOCK_TYPE_INVISIBLE;
        }

        @Override
        public int getBiome(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
            return 0;
        }

        @Override
        public byte getLight(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
            return 0;
        }
    }

    protected static final class DummyTypeRegistry implements FGameRegistry {
        public static final DummyTypeRegistry INSTANCE = new DummyTypeRegistry();

        @Override
        public byte[] registryToken() {
            return new byte[0];
        }

        @Override
        public IntStream biomes() {
            return IntStream.empty();
        }

        @Override
        public int biome2id(@NonNull Object biome) throws UnsupportedOperationException, ClassCastException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object id2biome(int biome) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FExtendedBiomeRegistryData extendedBiomeRegistryData() {
            throw new AbstractMethodError();
        }

        @Override
        public IntStream states() {
            return IntStream.range(0, BLOCK_TYPES);
        }

        @Override
        public int state2id(@NonNull Object state) throws UnsupportedOperationException, ClassCastException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object id2state(int state) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int state2id(@NonNull Object block, int meta) throws UnsupportedOperationException, ClassCastException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FExtendedStateRegistryData extendedStateRegistryData() {
            return ExtendedStateRegistryData.INSTANCE;
        }

        private static final class ExtendedStateRegistryData implements FExtendedStateRegistryData {
            private static final ExtendedStateRegistryData INSTANCE = new ExtendedStateRegistryData();

            @Override
            public FGameRegistry registry() {
                return DummyTypeRegistry.INSTANCE;
            }

            @Override
            public int type(int state) throws IndexOutOfBoundsException {
                return checkIndex(BLOCK_TYPES, state);
            }

            @Override
            public int stateInfo(int state) throws IndexOutOfBoundsException {
                return 0;
            }
        }
    }
}
