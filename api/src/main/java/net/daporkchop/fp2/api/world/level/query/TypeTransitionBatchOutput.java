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
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;

import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Describes a target for multiple {@link FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput)}  type transition searches}'
 * output to be written to.
 * <p>
 * A type transition query output consists of a sequence of data values, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). Each value is broken up
 * into multiple "bands", each of which contain a separate category of information. Bands may be enabled or disabled individually, allowing users to query only the
 * data they're interested in without wasting processing time reading unneeded values or allocating throwaway buffers.
 *
 * @author DaPorkchop_
 * @see TypeTransitionSingleOutput
 */
public interface TypeTransitionBatchOutput {
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
     * @return the number of query slots which are available in this storage, each of which with {@link #count()} elements
     */
    int slots();

    /**
     * @return the maximum number of type transitions which can be stored in this output per slot
     */
    int count();

    /**
     * Sets the length of the slot with the given index.
     *
     * @param slot   the output slot
     * @param length the new length for the slot
     */
    void setLength(int slot, int length);

    /**
     * Gets the length of the slot with the given index.
     *
     * @param slot   the output slot
     * @return the length for the slot
     */
    int getLength(int slot);

    /**
     * Sets the type transition value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS "type transitions" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot           the output slot
     * @param index          the output index
     * @param typeTransition the new type transition value
     */
    void setTypeTransition(int slot, int index, byte typeTransition);

    /**
     * Gets the type transition value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS "type transitions" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot           the output slot
     * @param index          the output index
     * @return the type transition value
     */
    byte getTypeTransition(int slot, int index);

    /**
     * Sets the X coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X "X coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot  the output slot
     * @param index the output index
     * @param x     the new x coordinate value
     */
    void setX(int slot, int index, int x);

    /**
     * Gets the X coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X "X coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot  the output slot
     * @param index the output index
     * @return     the x coordinate value
     */
    int getX(int slot, int index);

    /**
     * Sets the Y coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y "Y coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot  the output slot
     * @param index the output index
     * @param y     the new y coordinate value
     */
    void setY(int slot, int index, int y);

    /**
     * Gets the Y coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y "Y coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot  the output slot
     * @param index the output index
     * @return     the y coordinate value
     */
    int getY(int slot, int index);

    /**
     * Sets the Z coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z "Z coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot  the output slot
     * @param index the output index
     * @param z     the new z coordinate value
     */
    void setZ(int slot, int index, int z);

    /**
     * Gets the Z coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z "Z coordinates" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot  the output slot
     * @param index the output index
     * @return     the z coordinate value
     */
    int getZ(int slot, int index);

    /**
     * Sets the Z coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA "skipped NoData" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot          the output slot
     * @param index         the output index
     * @param skippedNoData the new "skipped NoData" value
     */
    void setSkippedNoData(int slot, int index, boolean skippedNoData);

    /**
     * Gets the Z coordinate value at the given output index.
     * <p>
     * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA "skipped NoData" type transition query output band}
     * enabled, the value will be silently discarded.
     *
     * @param slot          the output slot
     * @param index         the output index
     * @return the "skipped NoData" value
     */
    boolean getSkippedNoData(int slot, int index);

    /**
     * Sets the values in multiple bands at the given output index.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * this.setTypeTransition(slot, index, typeTransition);
     * this.setX(slot, index, x);
     * this.setY(slot, index, y);
     * this.setZ(slot, index, z);
     * this.setSkippedNoData(slot, index, skippedNoData);
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param slot           the output slot
     * @param index          the output index
     * @param typeTransition the new type transition descriptor value
     * @param x              the new x coordinate value
     * @param y              the new y coordinate value
     * @param z              the new z coordinate value
     * @param skippedNoData  the new "skipped NoData" value
     */
    default void setAll(int slot, int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
        this.setTypeTransition(slot, index, typeTransition);
        this.setX(slot, index, x);
        this.setY(slot, index, y);
        this.setZ(slot, index, z);
        this.setSkippedNoData(slot, index, skippedNoData);
    }

    /**
     * Gets a {@link TypeTransitionSingleOutput} instance which can access a single slot in this {@link TypeTransitionBatchOutput} instance.
     *
     * @param slot the slot index
     * @return a {@link TypeTransitionSingleOutput}
     */
    default TypeTransitionSingleOutput slot(int slot) {
        checkIndex(this.slots(), slot);

        return new TypeTransitionSingleOutput() {
            @Override
            public void validate() throws RuntimeException {
                TypeTransitionBatchOutput.this.validate();
            }

            @Override
            public int enabledBands() {
                return TypeTransitionBatchOutput.this.enabledBands();
            }

            @Override
            public int count() {
                return TypeTransitionBatchOutput.this.count();
            }

            @Override
            public void setTypeTransition(int index, byte typeTransition) {
                TypeTransitionBatchOutput.this.setTypeTransition(slot, index, typeTransition);
            }

            @Override
            public byte getTypeTransition(int index) {
                return TypeTransitionBatchOutput.this.getTypeTransition(slot, index);
            }

            @Override
            public void setX(int index, int x) {
                TypeTransitionBatchOutput.this.setX(slot, index, x);
            }

            @Override
            public int getX(int index) {
                return TypeTransitionBatchOutput.this.getX(slot, index);
            }

            @Override
            public void setY(int index, int y) {
                TypeTransitionBatchOutput.this.setY(slot, index, y);
            }

            @Override
            public int getY(int index) {
                return TypeTransitionBatchOutput.this.getY(slot, index);
            }

            @Override
            public void setZ(int index, int z) {
                TypeTransitionBatchOutput.this.setZ(slot, index, z);
            }

            @Override
            public int getZ(int index) {
                return TypeTransitionBatchOutput.this.getZ(slot, index);
            }

            @Override
            public void setSkippedNoData(int index, boolean skippedNoData) {
                TypeTransitionBatchOutput.this.setSkippedNoData(slot, index, skippedNoData);
            }

            @Override
            public boolean getSkippedNoData(int index) {
                return TypeTransitionBatchOutput.this.getSkippedNoData(slot, index);
            }

            @Override
            public void setAll(int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
                TypeTransitionBatchOutput.this.setAll(slot, index, typeTransition, x, y, z, skippedNoData);
            }
        };
    }

    /**
     * A simple {@link TypeTransitionBatchOutput} consisting of a separate array for each type transition query output band.
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
    final class BandArrays implements TypeTransitionBatchOutput {
        @NonNull
        private final int[] lengthsArray;
        private final int lengthsOffset;
        private final int lengthsStride;

        private final byte[] typeTransitionsArray;
        private final int typeTransitionsOffset;
        private final int typeTransitionsSlotStride;
        private final int typeTransitionsIndexStride;

        private final int[] xCoordinatesArray;
        private final int xCoordinatesOffset;
        private final int xCoordinatesSlotStride;
        private final int xCoordinatesIndexStride;

        private final int[] yCoordinatesArray;
        private final int yCoordinatesOffset;
        private final int yCoordinatesSlotStride;
        private final int yCoordinatesIndexStride;

        private final int[] zCoordinatesArray;
        private final int zCoordinatesOffset;
        private final int zCoordinatesSlotStride;
        private final int zCoordinatesIndexStride;

        private final boolean[] skippedNoDataArray;
        private final int skippedNoDataOffset;
        private final int skippedNoDataSlotStride;
        private final int skippedNoDataIndexStride;

        private final int slots;
        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure slots and count are valid
            notNegative(this.slots, "slots");
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                checkRangeLen(this.lengthsArray.length, this.lengthsOffset,
                        multiplyExact(positive(this.lengthsStride, "lengthsStride"), this.count) - this.lengthsOffset);

                if (this.typeTransitionsArray != null) {
                    int allSlotsLength = multiplyExact(positive(this.typeTransitionsSlotStride, "typeTransitionsSlotStride"), this.slots);
                    int indexSize = multiplyExact(positive(this.typeTransitionsIndexStride, "typeTransitionsIndexStride"), this.count);
                    int lastIndexPos = addExact(this.typeTransitionsSlotStride * (this.slots - 1), this.typeTransitionsIndexStride * this.count);
                    checkRangeLen(this.typeTransitionsArray.length, this.typeTransitionsOffset, lastIndexPos);
                }
                if (this.xCoordinatesArray != null) {
                    int allSlotsLength = multiplyExact(positive(this.xCoordinatesSlotStride, "xCoordinatesSlotStride"), this.slots);
                    int indexSize = multiplyExact(positive(this.xCoordinatesIndexStride, "xCoordinatesIndexStride"), this.count);
                    int lastIndexPos = addExact(this.xCoordinatesSlotStride * (this.slots - 1), this.xCoordinatesIndexStride * this.count);
                    checkRangeLen(this.xCoordinatesArray.length, this.xCoordinatesOffset, lastIndexPos);
                }
                if (this.yCoordinatesArray != null) {
                    int allSlotsLength = multiplyExact(positive(this.yCoordinatesSlotStride, "yCoordinatesSlotStride"), this.slots);
                    int indexSize = multiplyExact(positive(this.yCoordinatesIndexStride, "yCoordinatesIndexStride"), this.count);
                    int lastIndexPos = addExact(this.yCoordinatesSlotStride * (this.slots - 1), this.yCoordinatesIndexStride * this.count);
                    checkRangeLen(this.yCoordinatesArray.length, this.yCoordinatesOffset, lastIndexPos);
                }
                if (this.zCoordinatesArray != null) {
                    int allSlotsLength = multiplyExact(positive(this.zCoordinatesSlotStride, "zCoordinatesSlotStride"), this.slots);
                    int indexSize = multiplyExact(positive(this.zCoordinatesIndexStride, "zCoordinatesIndexStride"), this.count);
                    int lastIndexPos = addExact(this.zCoordinatesSlotStride * (this.slots - 1), this.zCoordinatesIndexStride * this.count);
                    checkRangeLen(this.zCoordinatesArray.length, this.zCoordinatesOffset, lastIndexPos);
                }
                if (this.skippedNoDataArray != null) {
                    int allSlotsLength = multiplyExact(positive(this.skippedNoDataSlotStride, "skippedNoDataSlotStride"), this.slots);
                    int indexSize = multiplyExact(positive(this.skippedNoDataIndexStride, "skippedNoDataIndexStride"), this.count);
                    int lastIndexPos = addExact(this.skippedNoDataSlotStride * (this.slots - 1), this.skippedNoDataIndexStride * this.count);
                    checkRangeLen(this.skippedNoDataArray.length, this.skippedNoDataOffset, lastIndexPos);
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
        public void setLength(int slot, int length) {
            checkIndex(this.slots, slot);

            //we assume our state is valid, and since we know that the slot is valid we can be sure there will be no overflows here
            this.lengthsArray[this.lengthsOffset + this.lengthsStride * slot] = length;
        }

        @Override
        public int getLength(int slot) {
            checkIndex(this.slots, slot);
            return this.lengthsArray[this.lengthsOffset + this.lengthsStride * slot];
        }

        @Override
        public void setTypeTransition(int slot, int index, byte typeTransition) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            if (this.typeTransitionsArray != null) {
                //we assume our state is valid, and since we know that the slot and index are valid we can be sure there will be no overflows here
                this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsSlotStride * slot + this.typeTransitionsIndexStride * index] = typeTransition;
            }
        }

        @Override
        public byte getTypeTransition(int slot, int index) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            checkState(this.typeTransitionsArray != null);
            return this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsSlotStride * slot + this.typeTransitionsIndexStride * index];
        }

        @Override
        public void setX(int slot, int index, int x) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            if (this.xCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the slot and index are valid we can be sure there will be no overflows here
                this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesSlotStride * slot + this.xCoordinatesIndexStride * index] = x;
            }
        }

        @Override
        public int getX(int slot, int index) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            checkState(this.xCoordinatesArray != null);
            return this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesSlotStride * slot + this.xCoordinatesIndexStride * index];
        }

        @Override
        public void setY(int slot, int index, int y) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            if (this.yCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the slot and index are valid we can be sure there will be no overflows here
                this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesSlotStride * slot + this.yCoordinatesIndexStride * index] = y;
            }
        }

        @Override
        public int getY(int slot, int index) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            checkState(this.yCoordinatesArray != null);
            return this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesSlotStride * slot + this.yCoordinatesIndexStride * index];
        }

        @Override
        public void setZ(int slot, int index, int z) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            if (this.zCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the slot and index are valid we can be sure there will be no overflows here
                this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesSlotStride * slot + this.zCoordinatesIndexStride * index] = z;
            }
        }

        @Override
        public int getZ(int slot, int index) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            checkState(this.zCoordinatesArray != null);
            return this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesSlotStride * slot + this.zCoordinatesIndexStride * index];
        }

        @Override
        public void setSkippedNoData(int slot, int index, boolean skippedNoData) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            if (this.skippedNoDataArray != null) {
                //we assume our state is valid, and since we know that the slot and index are valid we can be sure there will be no overflows here
                this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataSlotStride * slot + this.skippedNoDataIndexStride * index] = skippedNoData;
            }
        }

        @Override
        public boolean getSkippedNoData(int slot, int index) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            checkState(this.skippedNoDataArray != null);
            return this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataSlotStride * slot + this.skippedNoDataIndexStride * index];
        }

        @Override
        public void setAll(int slot, int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
            checkIndex(this.slots, slot);
            checkIndex(this.count, index);
            if (this.typeTransitionsArray != null) {
                //we assume our state is valid, and since we know that the slot and index are valid we can be sure there will be no overflows here
                this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsSlotStride * slot + this.typeTransitionsIndexStride * index] = typeTransition;
            }
            if (this.xCoordinatesArray != null) {
                this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesSlotStride * slot + this.xCoordinatesIndexStride * index] = x;
            }
            if (this.yCoordinatesArray != null) {
                this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesSlotStride * slot + this.yCoordinatesIndexStride * index] = y;
            }
            if (this.zCoordinatesArray != null) {
                this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesSlotStride * slot + this.zCoordinatesIndexStride * index] = z;
            }
            if (this.skippedNoDataArray != null) {
                this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataSlotStride * slot + this.skippedNoDataIndexStride * index] = skippedNoData;
            }
        }
    }
}
