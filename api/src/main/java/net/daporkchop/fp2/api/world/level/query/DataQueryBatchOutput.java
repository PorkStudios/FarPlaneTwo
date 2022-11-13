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

package net.daporkchop.fp2.api.world.level.query;

import lombok.Data;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Describes target for a query's result to be written to.
 * <p>
 * A query shape consists of a sequence of per-voxel data values, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). Each value is broken up into multiple
 * "bands", each of which contain a separate category of information. Bands may be enabled or disabled individually, allowing users to query only the data they're interested
 * in without wasting processing time reading unneeded values or allocating throwaway buffers.
 *
 * @author DaPorkchop_
 */
public interface DataQueryBatchOutput {
    /**
     * Ensures that this output's state is valid, throwing an exception if not.
     * <p>
     * If this method is not called and the output's state is invalid, the behavior of all other methods is undefined.
     * <p>
     * It is recommended to call this once per method body before using an output instance, as it could allow the JVM to optimize the code more aggressively.
     *
     * @throws RuntimeException if the output's state is invalid
     */
    void validate() throws RuntimeException;

    /**
     * @return a bitfield indicating which bands are enabled for this output
     * @see BlockLevelConstants#isDataBandEnabled(int, int)
     */
    int enabledBands();

    /**
     * @return the number of output slots which will be read by this query
     */
    int count();

    /**
     * Sets the state value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#DATA_BAND_ORDINAL_STATES "states" data band} enabled, the value will be silently discarded.
     *
     * @param index the output index
     * @param state the new state value
     */
    void setState(int index, int state);

    /**
     * Sets the biome value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#DATA_BAND_ORDINAL_BIOMES "biome" data band} enabled, the value will be silently discarded.
     *
     * @param index the output index
     * @param biome the new biome value
     */
    void setBiome(int index, int biome);

    /**
     * Sets the light value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#DATA_BAND_ORDINAL_LIGHT "light" data band} enabled, the value will be silently discarded.
     *
     * @param index the output index
     * @param light the new light value
     */
    void setLight(int index, byte light);

    /**
     * Sets the values in multiple bands at the given output index.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * this.setState(index, state);
     * this.setBiome(index, biome);
     * this.setLight(index, light);
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param index the output index
     * @param state the new state value
     * @param biome the new biome value
     * @param light the new light value
     */
    default void setStateBiomesLight(int index, int state, int biome, byte light) {
        this.setState(index, state);
        this.setBiome(index, biome);
        this.setLight(index, light);
    }

    /**
     * A simple {@link DataQueryBatchOutput} consisting of a separate array for each data band.
     * <p>
     * Each data band array is described by the following:
     * <ul>
     *     <li>the array to which output is to be written, or {@code null} if the data band is to be disabled</li>
     *     <li>the offset at which to begin writing values to the array. Should be {@code 0} to begin writing at the beginning of the array</li>
     *     <li>the stride between values written to the array. Should be {@code 1} for tightly-packed output</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    @Data
    final class BandArraysDataQueryBatchOutput implements DataQueryBatchOutput {
        private final int[] statesArray;
        private final int statesOffset;
        private final int statesStride;

        private final int[] biomesArray;
        private final int biomesOffset;
        private final int biomesStride;

        private final byte[] lightArray;
        private final int lightOffset;
        private final int lightStride;

        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure count is valid
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                if (this.statesArray != null) {
                    checkRangeLen(this.statesArray.length, this.statesOffset, multiplyExact(positive(this.statesStride, "statesStride"), this.count) - this.statesOffset);
                }
                if (this.biomesArray != null) {
                    checkRangeLen(this.biomesArray.length, this.biomesOffset, multiplyExact(positive(this.biomesStride, "biomesStride"), this.count) - this.biomesOffset);
                }
                if (this.lightArray != null) {
                    checkRangeLen(this.lightArray.length, this.lightOffset, multiplyExact(positive(this.lightStride, "lightStride"), this.count) - this.lightOffset);
                }
            }
        }

        @Override
        public int enabledBands() {
            int bands = 0;
            if (this.statesArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.DATA_BAND_ORDINAL_STATES);
            }
            if (this.biomesArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.DATA_BAND_ORDINAL_BIOMES);
            }
            if (this.lightArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.DATA_BAND_ORDINAL_LIGHT);
            }
            return bands;
        }

        @Override
        public void setState(int index, int state) {
            checkIndex(this.count, index);
            if (this.statesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.statesArray[this.statesOffset + this.statesStride * index] = state;
            }
        }

        @Override
        public void setBiome(int index, int biome) {
            checkIndex(this.count, index);
            if (this.biomesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.biomesArray[this.biomesOffset + this.biomesStride * index] = biome;
            }
        }

        @Override
        public void setLight(int index, byte light) {
            checkIndex(this.count, index);
            if (this.lightArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.lightArray[this.lightOffset + this.lightStride * index] = light;
            }
        }

        @Override
        public void setStateBiomesLight(int index, int state, int biome, byte light) {
            checkIndex(this.count, index);
            if (this.statesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.statesArray[this.statesOffset + this.statesStride * index] = state;
            }
            if (this.biomesArray != null) {
                this.biomesArray[this.biomesOffset + this.biomesStride * index] = biome;
            }
            if (this.lightArray != null) {
                this.lightArray[this.lightOffset + this.lightStride * index] = light;
            }
        }
    }
}
