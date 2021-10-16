/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.util.datastructure.java.ndimensionalintset;

import io.netty.util.internal.PlatformDependent;
import lombok.NonNull;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A fast hash-set implementation for 1-dimensional vectors with {@code int} components.
 * <p>
 * Optimized for the case where queries will be close to each other.
 * <p>
 * Adapted from {@link Int3HashSet}.
 *
 * @author DaPorkchop_
 */
public class Int1HashSet extends AbstractRefCounted implements NDimensionalIntSet {
    protected static final int BUCKET_AXIS_BITS = 6; //the number of bits per axis which are used inside of the bucket rather than identifying the bucket
    protected static final int BUCKET_AXIS_MASK = (1 << BUCKET_AXIS_BITS) - 1;

    protected static final long KEY_X_OFFSET = 0L;
    protected static final long KEY_BYTES = KEY_X_OFFSET + Integer.BYTES;

    protected static final long VALUE_BYTES = Long.BYTES;

    protected static final long BUCKET_KEY_OFFSET = 0L;
    protected static final long BUCKET_VALUE_OFFSET = BUCKET_KEY_OFFSET + KEY_BYTES;
    protected static final long BUCKET_BYTES = BUCKET_VALUE_OFFSET + VALUE_BYTES;

    protected static final long DEFAULT_TABLE_SIZE = 16L;

    protected static long hashPosition(int x) {
        return x * 1403638657883916319L; //some random prime number
    }

    protected static long positionFlag(int x) {
        return 1L << (x & BUCKET_AXIS_MASK);
    }

    protected static long allocateTable(long tableSize) {
        long size = tableSize * BUCKET_BYTES;
        long addr = PUnsafe.allocateMemory(size); //allocate
        PUnsafe.setMemory(addr, size, (byte) 0); //clear
        return addr;
    }

    protected long tableAddr = 0L; //the address of the table in memory
    protected long tableSize = 0L; //the physical size of the table (in buckets). always a non-zero power of two
    protected long resizeThreshold = 0L;
    protected long usedBuckets = 0L;

    protected long size = 0L; //the number of values stored in the set

    protected PCleaner cleaner;

    public Int1HashSet() {
        this.setTableSize(DEFAULT_TABLE_SIZE);
    }

    public Int1HashSet(int initialCapacity) {
        initialCapacity = (int) Math.ceil(initialCapacity * (1.0d / 0.75d)); //scale according to resize threshold
        initialCapacity = 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(initialCapacity - 1)); //round up to next power of two
        this.setTableSize(Math.max(initialCapacity, DEFAULT_TABLE_SIZE));
    }

    @Override
    public boolean add(int x) {
        long flag = positionFlag(x);
        long bucket = this.findBucket(x >> BUCKET_AXIS_BITS, true);

        long value = PUnsafe.getLong(bucket + BUCKET_VALUE_OFFSET);
        if ((value & flag) == 0L) { //flag wasn't previously set
            PUnsafe.putLong(bucket + BUCKET_VALUE_OFFSET, value | flag);
            this.size++; //the position was newly added, so we need to increment the total size
            return true;
        } else { //flag was already set
            return false;
        }
    }

    @Override
    public boolean contains(int x) {
        long flag = positionFlag(x);
        long bucket = this.findBucket(x >> BUCKET_AXIS_BITS, false);

        return bucket != 0L //bucket exists
               && (PUnsafe.getLong(bucket + BUCKET_VALUE_OFFSET) & flag) != 0L; //flag is set
    }

    protected long findBucket(int x, boolean createIfAbsent) {
        long tableSize = this.tableSize;
        long tableAddr = this.tableAddr;
        if (tableAddr == 0L) {
            if (createIfAbsent) { //the table hasn't been allocated yet - let's make a new one!
                this.tableAddr = tableAddr = allocateTable(tableSize);
                this.cleaner = PCleaner.cleaner(this, tableAddr);
            } else { //the table isn't even allocated yet, so the bucket clearly isn't present
                return 0L;
            }
        }

        long mask = tableSize - 1L; //tableSize is always a power of two, so we can safely create a bitmask like this
        long hash = hashPosition(x);

        for (long i = 0L; ; i++) {
            long bucketAddr = tableAddr + ((hash + i) & mask) * BUCKET_BYTES;

            if (PUnsafe.getLong(bucketAddr + BUCKET_VALUE_OFFSET) == 0L) { //if the bucket value is 0, it means the bucket hasn't been assigned yet
                if (createIfAbsent) {
                    if (this.usedBuckets < this.resizeThreshold) { //let's assign the bucket to our current position
                        this.usedBuckets++;
                        PUnsafe.putInt(bucketAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET, x);
                        return bucketAddr;
                    } else {
                        //we've established that there's no matching bucket, but the table is full. let's resize it before allocating a bucket
                        // to avoid overfilling the table
                        this.resize();
                        return this.findBucket(x, createIfAbsent); //tail recursion will probably be optimized away
                    }
                } else { //empty bucket, abort search - there won't be anything else later on
                    return 0L;
                }
            }

            //the bucket is set. check coordinates to see if it matches the one we're searching for
            if (PUnsafe.getInt(bucketAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET) == x) { //we found the matching bucket!
                return bucketAddr;
            }

            //continue search...
        }
    }

    protected void resize() {
        long oldTableSize = this.tableSize;
        long oldTableAddr = this.tableAddr;
        PCleaner oldCleaner = this.cleaner;

        //allocate new table
        long newTableSize = oldTableSize << 1L;
        this.setTableSize(newTableSize);
        long newTableAddr = this.tableAddr = allocateTable(newTableSize);
        this.cleaner = PCleaner.cleaner(this, newTableAddr);
        long newMask = newTableSize - 1L;

        //iterate through every bucket in the old table and copy it to the new one
        for (long i = 0; i < oldTableSize; i++) {
            long oldBucketAddr = oldTableAddr + i * BUCKET_BYTES;

            //read the bucket into registers
            int x = PUnsafe.getInt(oldBucketAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET);
            long value = PUnsafe.getLong(oldBucketAddr + BUCKET_VALUE_OFFSET);
            if (value == 0L) { //the bucket is unset, so there's no reason to copy it
                continue;
            }

            for (long hash = hashPosition(x), j = 0L; ; j++) {
                long newBucketAddr = newTableAddr + ((hash + j) & newMask) * BUCKET_BYTES;

                if (PUnsafe.getLong(newBucketAddr + BUCKET_VALUE_OFFSET) == 0L) { //if the bucket value is 0, it means the bucket hasn't been assigned yet
                    //write bucket into new table
                    PUnsafe.putInt(newBucketAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET, x);
                    PUnsafe.putLong(newBucketAddr + BUCKET_VALUE_OFFSET, value);
                    break; //advance to next bucket in old table
                }

                //continue search...
            }
        }

        //delete old table
        oldCleaner.clean();
    }

    @Override
    public void forEach(@NonNull Consumer<int[]> callback) {
        this.forEach1D(x -> callback.accept(new int[]{ x }));
    }

    @Override
    public void forEach1D(@NonNull IntConsumer action) {
        long tableAddr = this.tableAddr;
        if (tableAddr == 0L) { //the table isn't even allocated yet, there's nothing to iterate through...
            return;
        }

        //haha yes, c-style iterators
        for (long bucket = tableAddr, end = tableAddr + this.tableSize * BUCKET_BYTES; bucket != end; bucket += BUCKET_BYTES) {
            //read the bucket into registers
            int bucketX = PUnsafe.getInt(bucket + BUCKET_KEY_OFFSET + KEY_X_OFFSET);
            long value = PUnsafe.getLong(bucket + BUCKET_VALUE_OFFSET);

            while (value != 0L) {
                //this is intrinsic and compiles into TZCNT, which has a latency of 3 cycles - much faster than iterating through all 64 bits
                //  and checking each one individually!
                int index = Long.numberOfTrailingZeros(value);

                //clear the bit in question so that it won't be returned next time around
                value &= ~(1L << index);

                int dx = index & BUCKET_AXIS_MASK;
                action.accept((bucketX << BUCKET_AXIS_BITS) + dx);
            }
        }
    }

    @Override
    public boolean remove(int x) {
        long tableAddr = this.tableAddr;
        if (tableAddr == 0L) { //the table isn't even allocated yet, there's nothing to remove...
            return false;
        }

        long mask = this.tableSize - 1L; //tableSize is always a power of two, so we can safely create a bitmask like this

        long flag = positionFlag(x);
        int searchBucketX = x >> BUCKET_AXIS_BITS;
        long hash = hashPosition(searchBucketX);

        for (long i = 0L; ; i++) {
            long bucketAddr = tableAddr + ((hash + i) & mask) * BUCKET_BYTES;

            //read the bucket into registers
            int bucketX = PUnsafe.getInt(bucketAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET);
            long value = PUnsafe.getLong(bucketAddr + BUCKET_VALUE_OFFSET);
            if (value == 0L) { //the bucket is unset. we've reached the end of the bucket chain for this hash, which means
                return false;
            } else if (bucketX != searchBucketX) { //the bucket doesn't match, so the search must go on
                continue;
            } else if ((value & flag) == 0L) { //we've found a matching bucket, but the position's flag is unset. there's nothing for us to do...
                return false;
            }

            //the bucket that we found contains the position, so now we remove it from the set
            this.size--;

            if ((value & ~flag) == 0L) { //this position is the only position in the bucket, so we need to delete the bucket
                this.usedBuckets--;

                //shifting the buckets IS expensive, yes, but it'll only happen when the entire bucket is deleted, which won't happen on every removal
                this.shiftBuckets(tableAddr, (hash + i) & mask, mask);
            } else { //update bucket value with this position removed
                PUnsafe.putLong(bucketAddr + BUCKET_VALUE_OFFSET, value & ~flag);
            }

            return true;
        }
    }

    //adapted from it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap#shiftKeys(int)
    protected void shiftBuckets(long tableAddr, long pos, long mask) {
        long last;
        long slot;

        int currX;
        long currValue;

        for (; ; ) {
            pos = ((last = pos) + 1L) & mask;
            for (; ; pos = (pos + 1L) & mask) {
                long currAddr = tableAddr + pos * BUCKET_BYTES;
                if ((currValue = PUnsafe.getLong(currAddr + BUCKET_VALUE_OFFSET)) == 0L) { //curr points to an unset bucket
                    PUnsafe.setMemory(tableAddr + last * BUCKET_BYTES, BUCKET_BYTES, (byte) 0); //delete last bucket
                    return;
                }

                slot = hashPosition(
                        currX = PUnsafe.getInt(currAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET)) & mask;

                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
                    long lastAddr = tableAddr + last * BUCKET_BYTES;
                    PlatformDependent.putInt(lastAddr + BUCKET_KEY_OFFSET + KEY_X_OFFSET, currX);
                    PlatformDependent.putLong(lastAddr + BUCKET_VALUE_OFFSET, currValue);
                    break;
                }
            }
        }
    }

    @Override
    public void clear() {
        if (this.isEmpty()) { //if the set is empty, there's nothing to clear
            return;
        }

        //fill the entire table with zeroes
        // (since the table isn't empty, we can be sure that the table has been allocated so there's no reason to check for it)
        PUnsafe.setMemory(this.tableAddr, this.tableSize * BUCKET_BYTES, (byte) 0);

        //reset all size counters
        this.usedBuckets = 0L;
        this.size = 0L;
    }

    protected void setTableSize(long tableSize) {
        this.tableSize = tableSize;
        this.resizeThreshold = (tableSize >> 1L) + (tableSize >> 2L); //count * 0.75
    }

    @Override
    public long count() {
        return this.size;
    }

    @Override
    public Int1HashSet retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        if (this.cleaner != null) {
            this.cleaner.clean();
        }
    }

    //
    // NDimensionalIntSet methods
    //

    @Override
    public int dimensions() {
        return 2;
    }

    @Override
    public boolean add(@NonNull int... point) {
        checkArg(point.length == 2);
        return this.add(point[0], point[1]);
    }

    @Override
    public boolean remove(@NonNull int... point) {
        checkArg(point.length == 2);
        return this.remove(point[0], point[1]);
    }

    @Override
    public boolean contains(@NonNull int... point) {
        checkArg(point.length == 2);
        return this.contains(point[0], point[1]);
    }
}
