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
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Describes a target for a single {@link FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput)} type transition search}'s
 * output to be written to.
 * <p>
 * A type transition query output consists of a sequence of data values, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). Each value is broken up
 * into multiple "bands", each of which contain a separate category of information. Bands may be enabled or disabled individually, allowing users to query only the
 * data they're interested in without wasting processing time reading unneeded values or allocating throwaway buffers.
 *
 * @author DaPorkchop_
 * @see TypeTransitionBatchOutput
 */
public interface TypeTransitionSingleOutput {
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
     * @see BlockLevelConstants#isTypeTransitionQueryOutputBandEnabled(int, int)
     */
    int enabledBands();

    /**
     * @return the maximum number of type transitions which can be stored in this output
     */
    int count();

    /**
     * Sets the type transition value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS "type transitions" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param index          the output index
     * @param typeTransition the new type transition value
     */
    void setTypeTransition(int index, byte typeTransition);

    /**
     * Sets the X coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X "X coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param index the output index
     * @param x     the new x coordinate value
     */
    void setX(int index, int x);

    /**
     * Sets the Y coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y "Y coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param index the output index
     * @param y     the new y coordinate value
     */
    void setY(int index, int y);

    /**
     * Sets the Z coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z "Z coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param index the output index
     * @param z     the new z coordinate value
     */
    void setZ(int index, int z);

    /**
     * Sets the Z coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA "skipped NoData" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param index         the output index
     * @param skippedNoData the new "skipped NoData" value
     */
    void setSkippedNoData(int index, boolean skippedNoData);

    /**
     * Sets the values in multiple bands at the given output index.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * this.setTypeTransition(index, typeTransition);
     * this.setX(index, x);
     * this.setY(index, y);
     * this.setZ(index, z);
     * this.setSkippedNoData(index, skippedNoData);
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param index          the output index
     * @param typeTransition the new type transition descriptor value
     * @param x              the new x coordinate value
     * @param y              the new y coordinate value
     * @param z              the new z coordinate value
     * @param skippedNoData  the new "skipped NoData" value
     */
    default void setAll(int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
        this.setTypeTransition(index, typeTransition);
        this.setX(index, x);
        this.setY(index, y);
        this.setZ(index, z);
        this.setSkippedNoData(index, skippedNoData);
    }

    /**
     * A simple {@link TypeTransitionSingleOutput} consisting of a separate array for each type transition query output band.
     * <p>
     * Each type transition query output band array is described by the following:
     * <ul>
     *     <li>the array to which output is to be written, or {@code null} if the type transition query output band is to be disabled</li>
     *     <li>the offset at which to begin writing values to the array. Should be {@code 0} to begin writing at the beginning of the array</li>
     *     <li>the stride between values written to the array. Should be {@code 1} for tightly-packed output</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    @Data
    final class BandArrays implements TypeTransitionSingleOutput {
        public static BandArrays createWithCount(@NotNegative int count) {
            return createWithCount(count, allTypeTransitionQueryOutputBands());
        }

        public static BandArrays createWithCount(@NotNegative int count, int enabledBands) {
            return new BandArrays(
                    isTypeTransitionQueryOutputBandEnabled(enabledBands, TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS) ? new byte[count] : null, 0, 1,
                    isTypeTransitionQueryOutputBandEnabled(enabledBands, TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y) ? new int[count] : null, 0, 1,
                    isTypeTransitionQueryOutputBandEnabled(enabledBands, TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z) ? new int[count] : null, 0, 1,
                    isTypeTransitionQueryOutputBandEnabled(enabledBands, TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X) ? new int[count] : null, 0, 1,
                    isTypeTransitionQueryOutputBandEnabled(enabledBands, TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA) ? new boolean[count] : null, 0, 1,
                    count);
        }
        
        private final byte[] typeTransitionsArray;
        private final int typeTransitionsOffset;
        private final int typeTransitionsStride;

        private final int[] xCoordinatesArray;
        private final int xCoordinatesOffset;
        private final int xCoordinatesStride;

        private final int[] yCoordinatesArray;
        private final int yCoordinatesOffset;
        private final int yCoordinatesStride;

        private final int[] zCoordinatesArray;
        private final int zCoordinatesOffset;
        private final int zCoordinatesStride;

        private final boolean[] skippedNoDataArray;
        private final int skippedNoDataOffset;
        private final int skippedNoDataStride;

        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure count is valid
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                if (this.typeTransitionsArray != null) {
                    checkRangeLen(this.typeTransitionsArray.length, this.typeTransitionsOffset,
                            multiplyExact(positive(this.typeTransitionsStride, "typeTransitionsStride"), this.count) - this.typeTransitionsOffset);
                }
                if (this.xCoordinatesArray != null) {
                    checkRangeLen(this.xCoordinatesArray.length, this.xCoordinatesOffset,
                            multiplyExact(positive(this.xCoordinatesStride, "xCoordinatesStride"), this.count) - this.xCoordinatesOffset);
                }
                if (this.yCoordinatesArray != null) {
                    checkRangeLen(this.yCoordinatesArray.length, this.yCoordinatesOffset,
                            multiplyExact(positive(this.yCoordinatesStride, "yCoordinatesStride"), this.count) - this.yCoordinatesOffset);
                }
                if (this.zCoordinatesArray != null) {
                    checkRangeLen(this.zCoordinatesArray.length, this.zCoordinatesOffset,
                            multiplyExact(positive(this.zCoordinatesStride, "zCoordinatesStride"), this.count) - this.zCoordinatesOffset);
                }
                if (this.skippedNoDataArray != null) {
                    checkRangeLen(this.skippedNoDataArray.length, this.skippedNoDataOffset,
                            multiplyExact(positive(this.skippedNoDataStride, "skippedNoDataStride"), this.count) - this.skippedNoDataOffset);
                }
            }
        }

        @Override
        public int enabledBands() {
            int bands = 0;
            if (this.typeTransitionsArray != null) {
                bands |= typeTransitionQueryOutputBandFlag(TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS);
            }
            if (this.xCoordinatesArray != null) {
                bands |= typeTransitionQueryOutputBandFlag(TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X);
            }
            if (this.yCoordinatesArray != null) {
                bands |= typeTransitionQueryOutputBandFlag(TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y);
            }
            if (this.zCoordinatesArray != null) {
                bands |= typeTransitionQueryOutputBandFlag(TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z);
            }
            if (this.skippedNoDataArray != null) {
                bands |= typeTransitionQueryOutputBandFlag(TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA);
            }
            return bands;
        }

        @Override
        public void setTypeTransition(int index, byte typeTransition) {
            checkIndex(this.count, index);
            if (this.typeTransitionsArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsStride * index] = typeTransition;
            }
        }

        @Override
        public void setX(int index, int x) {
            checkIndex(this.count, index);
            if (this.xCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesStride * index] = x;
            }
        }

        @Override
        public void setY(int index, int y) {
            checkIndex(this.count, index);
            if (this.yCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesStride * index] = y;
            }
        }

        @Override
        public void setZ(int index, int z) {
            checkIndex(this.count, index);
            if (this.zCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesStride * index] = z;
            }
        }

        @Override
        public void setSkippedNoData(int index, boolean skippedNoData) {
            checkIndex(this.count, index);
            if (this.skippedNoDataArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataStride * index] = skippedNoData;
            }
        }

        @Override
        public void setAll(int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
            checkIndex(this.count, index);
            if (this.typeTransitionsArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsStride * index] = typeTransition;
            }
            if (this.xCoordinatesArray != null) {
                this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesStride * index] = x;
            }
            if (this.yCoordinatesArray != null) {
                this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesStride * index] = y;
            }
            if (this.zCoordinatesArray != null) {
                this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesStride * index] = z;
            }
            if (this.skippedNoDataArray != null) {
                this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataStride * index] = skippedNoData;
            }
        }
    }
}
