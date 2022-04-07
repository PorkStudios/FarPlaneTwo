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

package net.daporkchop.fp2.core.mode.common.util;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.util.datastructure.simple.SimpleList;
import net.daporkchop.lib.common.math.BinMath;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base class for implementations of {@link List} optimized specifically for a specific {@link IFarPos} type.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractPosArrayList<POS extends IFarPos> extends SimpleList<POS> {
    protected static final int DEFAULT_ARRAY_SIZE = 16;

    protected int[] array;

    protected final int wordsPerPos;
    @Getter
    protected int size;

    public AbstractPosArrayList(int wordsPerPos) {
        this.wordsPerPos = positive(wordsPerPos, "wordsPerPos");
    }

    public AbstractPosArrayList(int wordsPerPos, int initialCapacity) {
        this(wordsPerPos);
        this.array = new int[multiplyExact(BinMath.roundToNearestPowerOf2(notNegative(initialCapacity, "initialCapacity")), wordsPerPos)];
    }

    public AbstractPosArrayList(int wordsPerPos, @NonNull Collection<? extends POS> src) {
        this(wordsPerPos, src.size());

        if (this.getClass() == src.getClass()) { //the source collection is a position list of the same type, clone it
            AbstractPosArrayList<? extends POS> srcList = uncheckedCast(src);

            //the array is pre-allocated and is big enough to fit all the values: we can do a simple array copy
            this.size = srcList.size;
            System.arraycopy(srcList.array, 0, this.array, 0, this.size);
        } else { //some other type of collection, fall back to regular addAll
            this.addAll(src);
        }
    }

    /**
     * Reads a sequence of exactly {@link #wordsPerPos} {@code int}s from the given array starting at the given index and converts them to a new {@link POS} instance.
     *
     * @param srcArray     the array to read from
     * @param srcBaseArray the index in the array to begin reading at
     * @return the new {@link POS} instance
     */
    protected abstract POS readPos(int[] srcArray, int srcBaseArray);

    /**
     * Converts the given position to a sequence of exactly {@link #wordsPerPos} {@code int}s and writes them to the given array starting at the given index.
     *
     * @param pos          the position to write
     * @param dstArray     the array to write to
     * @param dstBaseIndex the index in the array to begin writing at
     */
    protected abstract void writePos(POS pos, int[] dstArray, int dstBaseIndex);

    @Override
    public void clear() {
        //setting the size to zero effectively removes everything from the list, no further cleanup is required
        this.size = 0;
    }

    @Override
    public POS get(int index) {
        checkIndex(this.size, index);

        //the size must be non-zero, therefore the array must have been allocated
        return this.readPos(this.array, index * this.wordsPerPos);
    }

    @Override
    public POS set(int index, POS pos) {
        checkIndex(this.size, index);

        //the size must be non-zero, therefore the array must have been allocated
        int[] array = this.array;
        int baseIndex = index * this.wordsPerPos;

        //read the current position, then replace it with the new one
        POS old = this.readPos(array, baseIndex);
        this.writePos(pos, array, baseIndex);
        return old;
    }

    @Override
    public boolean add(POS pos) {
        int size = this.size;
        int[] array = this.array;
        int baseIndex = size * this.wordsPerPos;

        if (array == null //array is unallocated
            || baseIndex == array.length) { //array is full
            array = this.allocateOrGrow();
        }

        this.writePos(pos, array, baseIndex);
        this.size = size + 1;
        return true;
    }

    @Override
    public void add(int index, POS pos) {
        int size = this.size;
        checkIndex(size + 1, index);

        int[] array = this.array;
        int wordsPerPos = this.wordsPerPos;
        int baseIndex = index * wordsPerPos;

        if (array == null //array is unallocated
            || (size + 1) * wordsPerPos == array.length) { //array is full
            array = this.allocateOrGrow();
        }

        if (index != size) { //we're inserting a value in the middle of the list, so we need to shift subsequent elements forwards
            System.arraycopy(array, baseIndex, array, baseIndex + wordsPerPos, size * wordsPerPos - baseIndex);
        }

        this.writePos(pos, array, baseIndex);
        this.size = size + 1;
    }

    protected int[] allocateOrGrow() {
        return this.array = this.array == null
                ? new int[this.wordsPerPos * DEFAULT_ARRAY_SIZE] //array is currently unallocated
                : Arrays.copyOf(this.array, multiplyExact(this.array.length, 2)); //array is already allocated, but too small
    }

    @Override
    public POS remove(int index) {
        int size = this.size;
        checkIndex(size, index);

        //the size must be non-zero, therefore the array must have been allocated
        int[] array = this.array;
        int wordsPerPos = this.wordsPerPos;
        int baseIndex = index * wordsPerPos;

        //read the current position, then remove it
        POS old = this.readPos(array, baseIndex);

        this.size = --size; //decrement size
        if (index != size) { //we're not at the end of the list, so we need to shift the subsequent elements down
            System.arraycopy(array, baseIndex + wordsPerPos, array, baseIndex, size * wordsPerPos - baseIndex);
        }
        return old;
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        int size = this.size;
        checkRange(size, fromIndex, toIndex);

        //the size must be non-zero, therefore the array must have been allocated

        //decrease size
        this.size = size - (toIndex - fromIndex);

        if (toIndex < size) { //the range being removed isn't at the end, so we need to shift the subsequent elements down
            int[] array = this.array;
            int wordsPerPos = this.wordsPerPos;
            System.arraycopy(array, toIndex * wordsPerPos, array, fromIndex * wordsPerPos, (size - toIndex) * wordsPerPos);
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super POS> callback) {
        int[] array = this.array;
        if (array != null) { //the array has been allocated
            for (int wordsPerPos = this.wordsPerPos, i = 0, end = this.size * wordsPerPos; i != end; i += wordsPerPos) {
                callback.accept(this.readPos(array, i));
            }
        }
    }

    @Override
    public boolean removeIf(@NonNull Predicate<? super POS> filter) {
        int[] array = this.array;
        int wordsPerPos = this.wordsPerPos;
        int size = this.size;

        //this will not behave correctly if the filter throws an exception. also, i don't care!

        boolean anyRemoved = false;

        for (int readIndex = 0, writeIndex = 0, end = size * wordsPerPos; readIndex != end; readIndex += wordsPerPos) {
            if (filter.test(this.readPos(array, readIndex))) { //the position needs to be removed
                anyRemoved = true;
                size--;
            } else { //the position needs to be retained
                System.arraycopy(array, readIndex, array, writeIndex, wordsPerPos);
                writeIndex += wordsPerPos;
            }
        }

        //TODO: optimized implementation for longer sequences of retained values
        /*OUTER:
        for (int readIndex = 0, writeIndex = 0, end = size * wordsPerPos; readIndex != end; ) {
            //skip a run of positions which need to be removed
            while (filter.test(this.readPos(array, readIndex))) {
                anyRemoved = true;
                size--;

                readIndex += wordsPerPos; //advance iterator
                if (readIndex == end) { //we've reached the end of the list
                    break OUTER;
                }
            }

            //the position at the current index needs to be retained
            int retainStartIndex = readIndex;
            readIndex += wordsPerPos; //advance iterator because it wasn't advanced before

            //keep advancing until we find the end of this run of positions to remove
            while (readIndex != end //we haven't reached the end
                   && !filter.test(this.readPos(array, readIndex += wordsPerPos))) { //position needs to stay
                //no-op
            }

            //we've reached the end of a sequence to retain, shift all of the elements down
            int retainEndIndex = readIndex;
            int retainLengthWords = retainEndIndex - retainStartIndex;
            System.arraycopy(array, retainStartIndex, array, writeIndex, retainLengthWords);
            writeIndex += retainLengthWords;
        }*/

        if (anyRemoved) {
            this.size = size; //some elements were removed, update the size counter
        }
        return anyRemoved;
    }
}
