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

package net.daporkchop.fp2.core.engine.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.util.datastructure.simple.SimpleList;
import net.daporkchop.lib.common.annotation.NotThreadSafe;
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
 * Implementation of {@link List} optimized specifically for {@link TilePos}.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
@NotThreadSafe
public final class TilePosArrayList extends SimpleList<TilePos> {
    private static final int DEFAULT_ARRAY_SIZE = 16;

    private static final int INTS_PER_POS = 4;

    private int[] array;

    @Getter
    private int size;

    public TilePosArrayList() {
        this.array = new int[DEFAULT_ARRAY_SIZE * INTS_PER_POS];
    }

    public TilePosArrayList(int initialCapacity) {
        this.array = new int[multiplyExact(BinMath.roundToNearestPowerOf2(notNegative(initialCapacity, "initialCapacity")), INTS_PER_POS)];
    }

    public TilePosArrayList(@NonNull Collection<? extends TilePos> src) {
        if (src instanceof TilePosArrayList) { //the source collection is a position list of the same type, clone it
            TilePosArrayList srcList = uncheckedCast(src);

            //the array is pre-allocated and is big enough to fit all the values: we can do a simple array copy
            this.size = srcList.size;
            this.array = Arrays.copyOf(srcList.array, srcList.size * INTS_PER_POS);
        } else { //some other type of collection, fall back to regular addAll
            this.addAll(src);
        }
    }

    private TilePos readPos(int[] srcArray, int srcBaseArray) {
        return new TilePos(srcArray[srcBaseArray++], srcArray[srcBaseArray++], srcArray[srcBaseArray++], srcArray[srcBaseArray]);
    }

    private void writePos(TilePos pos, int[] dstArray, int dstBaseIndex) {
        dstArray[dstBaseIndex++] = pos.level();
        dstArray[dstBaseIndex++] = pos.x();
        dstArray[dstBaseIndex++] = pos.y();
        dstArray[dstBaseIndex] = pos.z();
    }

    @Override
    public void clear() {
        //setting the size to zero effectively removes everything from the list, no further cleanup is required
        this.size = 0;
    }

    @Override
    public TilePos get(int index) {
        checkIndex(this.size, index);

        //the size must be non-zero, therefore the array must have been allocated
        return this.readPos(this.array, index * INTS_PER_POS);
    }

    @Override
    public TilePos set(int index, TilePos pos) {
        checkIndex(this.size, index);

        //the size must be non-zero, therefore the array must have been allocated
        int[] array = this.array;
        int baseIndex = index * INTS_PER_POS;

        //read the current position, then replace it with the new one
        TilePos old = this.readPos(array, baseIndex);
        this.writePos(pos, array, baseIndex);
        return old;
    }

    @Override
    public boolean add(TilePos pos) {
        int size = this.size;
        int[] array = this.array;
        int baseIndex = size * INTS_PER_POS;
        addExact(baseIndex, INTS_PER_POS); //ensure we don't overflow

        if (array == null //array is unallocated
            || baseIndex == array.length) { //array is full
            array = this.allocateOrGrow();
        }

        this.writePos(pos, array, baseIndex);
        this.size = size + 1;
        return true;
    }

    @Override
    public void add(int index, TilePos pos) {
        int size = this.size;
        checkIndex(size + 1, index);

        int[] array = this.array;
        int baseIndex = index * INTS_PER_POS;

        if (array == null //array is unallocated
            || (size + 1) * INTS_PER_POS == array.length) { //array is full
            array = this.allocateOrGrow();
        }

        if (index != size) { //we're inserting a value in the middle of the list, so we need to shift subsequent elements forwards
            System.arraycopy(array, baseIndex, array, baseIndex + INTS_PER_POS, size * INTS_PER_POS - baseIndex);
        }

        this.writePos(pos, array, baseIndex);
        this.size = size + 1;
    }

    @Override
    public boolean addAll(int index, Collection<? extends TilePos> c) {
        int size = this.size;
        if (index != size) { //we're not appending to the tail of the list
            checkIndex(size, index);
        }

        int cSize = c.size();
        if (cSize == 0) { //other collection is empty, nothing to do!
            return false;
        }

        int[] array = this.array;
        int wordsPerPos = INTS_PER_POS;

        int totalSize = addExact(size, cSize);
        int baseIndex = index * wordsPerPos;

        if (array == null //array is unallocated
            || multiplyExact(totalSize, wordsPerPos) >= array.length) { //array is full
            array = this.allocateOrGrow(cSize);
        }

        if (index != size) { //we're inserting the values in the middle of the list, so we need to shift subsequent elements forwards
            System.arraycopy(array, baseIndex, array, (index + cSize) * wordsPerPos, size * wordsPerPos - baseIndex);
        }

        if (c instanceof TilePosArrayList) { //source collection is of the same class, we can simply copy the array elements
            System.arraycopy(((TilePosArrayList) c).array, 0, array, baseIndex, cSize * wordsPerPos);
        } else { //source collection is of some other type, write the elements to the target array using forEach
            @AllArgsConstructor
            class State implements Consumer<TilePos> {
                final int[] array;
                int baseIndex;

                @Override
                public void accept(TilePos pos) {
                    int baseIndex = this.baseIndex;
                    TilePosArrayList.this.writePos(pos, this.array, baseIndex);
                    this.baseIndex = baseIndex + wordsPerPos;
                }
            }

            c.forEach(new State(array, baseIndex));
        }

        //increase final size counter
        this.size = totalSize;
        return true;
    }

    private int[] allocateOrGrow() {
        return this.array = this.array == null
                ? new int[INTS_PER_POS * DEFAULT_ARRAY_SIZE] //array is currently unallocated
                : Arrays.copyOf(this.array, multiplyExact(this.array.length, 2)); //array is already allocated, but too small
    }

    private int[] allocateOrGrow(int minIncrease) {
        return this.array = this.array == null
                ? new int[multiplyExact(INTS_PER_POS, max(BinMath.roundToNearestPowerOf2(minIncrease), DEFAULT_ARRAY_SIZE))] //array is currently unallocated
                : Arrays.copyOf(this.array, max(multiplyExact(INTS_PER_POS, BinMath.roundToNearestPowerOf2(addExact(this.array.length / INTS_PER_POS, minIncrease))), multiplyExact(this.array.length, 2))); //array is already allocated, but too small
    }

    @Override
    public TilePos remove(int index) {
        int size = this.size;
        checkIndex(size, index);

        //the size must be non-zero, therefore the array must have been allocated
        int[] array = this.array;
        int wordsPerPos = INTS_PER_POS;
        int baseIndex = index * wordsPerPos;

        //read the current position, then remove it
        TilePos old = this.readPos(array, baseIndex);

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
            int wordsPerPos = INTS_PER_POS;
            System.arraycopy(array, toIndex * wordsPerPos, array, fromIndex * wordsPerPos, (size - toIndex) * wordsPerPos);
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super TilePos> callback) {
        int[] array = this.array;
        if (array != null) { //the array has been allocated
            for (int wordsPerPos = INTS_PER_POS, i = 0, end = this.size * wordsPerPos; i != end; i += wordsPerPos) {
                callback.accept(this.readPos(array, i));
            }
        }
    }

    @Override
    public boolean removeIf(@NonNull Predicate<? super TilePos> filter) {
        int[] array = this.array;
        int wordsPerPos = INTS_PER_POS;
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
