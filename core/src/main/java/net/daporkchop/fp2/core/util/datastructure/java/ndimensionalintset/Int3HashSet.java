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

package net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintset;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;

import java.util.Arrays;
import java.util.function.Consumer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A fast hash-set implementation for 3-dimensional vectors with {@code int} components.
 * <p>
 * Optimized for the case where queries will be close to each other.
 *
 * @author DaPorkchop_
 * @see <a href="https://github.com/OpenCubicChunks/CubicChunks/pull/674">https://github.com/OpenCubicChunks/CubicChunks/pull/674</a>
 */
public class Int3HashSet implements NDimensionalIntSet {
    protected static final int AXIS_COUNT = 3;

    protected static final int AXIS_X_OFFSET = 0;
    protected static final int AXIS_Y_OFFSET = 1;
    protected static final int AXIS_Z_OFFSET = 2;

    protected static final int BUCKET_AXIS_BITS = 2; //the number of bits per axis which are used inside of the bucket rather than identifying the bucket
    protected static final int BUCKET_AXIS_MASK = (1 << BUCKET_AXIS_BITS) - 1;

    protected static final int DEFAULT_TABLE_SIZE = 16;

    protected static int hashPosition(int x, int y, int z) {
        return (int) ((x * 1403638657883916319L //some random prime numbers
                       + y * 4408464607732138253L
                       + z * 2587306874955016303L) >>> 32L);
    }

    protected static long positionFlag(int x, int y, int z) {
        return 1L << (((x & BUCKET_AXIS_MASK) << (BUCKET_AXIS_BITS * 2)) | ((y & BUCKET_AXIS_MASK) << BUCKET_AXIS_BITS) | (z & BUCKET_AXIS_MASK));
    }

    protected int[] keys = null;
    protected long[] values = null;

    protected int tableSize = 0; //the physical size of the table (in buckets). always a non-zero power of two
    protected int resizeThreshold = 0;
    protected int usedBuckets = 0;

    @Getter
    protected int size = 0; //the number of values stored in the set

    public Int3HashSet() {
        this.setTableSize(DEFAULT_TABLE_SIZE);
    }

    public Int3HashSet(int initialCapacity) {
        initialCapacity = (int) Math.ceil(initialCapacity * (1.0d / 0.75d)); //scale according to resize threshold
        initialCapacity = 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(initialCapacity - 1)); //round up to next power of two
        this.setTableSize(Math.max(initialCapacity, DEFAULT_TABLE_SIZE));
    }

    @Override
    public boolean add(int x, int y, int z) {
        long flag = positionFlag(x, y, z);
        int bucket = this.findBucket(x >> BUCKET_AXIS_BITS, y >> BUCKET_AXIS_BITS, z >> BUCKET_AXIS_BITS, true);

        long value = this.values[bucket];
        if ((value & flag) == 0L) { //flag wasn't previously set
            this.values[bucket] = value | flag;
            this.size = incrementExact(this.size); //the position was newly added, so we need to increment the total size
            return true;
        } else { //flag was already set
            return false;
        }
    }

    @Override
    public boolean contains(int x, int y, int z) {
        long flag = positionFlag(x, y, z);
        int bucket = this.findBucket(x >> BUCKET_AXIS_BITS, y >> BUCKET_AXIS_BITS, z >> BUCKET_AXIS_BITS, false);

        return bucket >= 0 //bucket exists
               && (this.values[bucket] & flag) != 0L; //flag is set
    }

    protected int findBucket(int x, int y, int z, boolean createIfAbsent) {
        int[] keys = this.keys;
        long[] values = this.values;

        int tableSize = this.tableSize;
        if (keys == null) {
            if (createIfAbsent) { //the table hasn't been allocated yet - let's make a new one!
                this.keys = keys = new int[multiplyExact(tableSize, AXIS_COUNT)];
                this.values = values = new long[tableSize];
            } else { //the table isn't even allocated yet, so the bucket clearly isn't present
                return -1;
            }
        }

        int mask = tableSize - 1; //tableSize is always a power of two, so we can safely create a bitmask like this
        int hash = hashPosition(x, y, z);

        for (int i = 0; ; i++) {
            int bucket = (hash + i) & mask;

            if (values[bucket] == 0L) { //if the bucket value is 0, it means the bucket hasn't been assigned yet
                if (createIfAbsent) {
                    if (this.usedBuckets < this.resizeThreshold) { //let's assign the bucket to our current position
                        this.usedBuckets++;
                        keys[bucket * AXIS_COUNT + AXIS_X_OFFSET] = x;
                        keys[bucket * AXIS_COUNT + AXIS_Y_OFFSET] = y;
                        keys[bucket * AXIS_COUNT + AXIS_Z_OFFSET] = z;
                        return bucket;
                    } else {
                        //we've established that there's no matching bucket, but the table is full. let's resize it before allocating a bucket
                        // to avoid overfilling the table
                        this.resize();
                        return this.findBucket(x, y, z, createIfAbsent); //tail recursion will probably be optimized away
                    }
                } else { //empty bucket, abort search - there won't be anything else later on
                    return -1;
                }
            }

            //the bucket is set. check coordinates to see if it matches the one we're searching for
            if (keys[bucket * AXIS_COUNT + AXIS_X_OFFSET] == x
                && keys[bucket * AXIS_COUNT + AXIS_Y_OFFSET] == y
                && keys[bucket * AXIS_COUNT + AXIS_Z_OFFSET] == z) { //we found the matching bucket!
                return bucket;
            }

            //continue search...
        }
    }

    protected void resize() {
        int oldTableSize = this.tableSize;
        int[] oldKeys = this.keys;
        long[] oldValues = this.values;

        //allocate new table
        int newTableSize = multiplyExact(oldTableSize, 2);
        this.setTableSize(newTableSize);
        int[] newKeys = this.keys = new int[multiplyExact(newTableSize, AXIS_COUNT)];
        long[] newValues = this.values = new long[newTableSize];
        int newMask = newTableSize - 1;

        //iterate through every bucket in the old table and copy it to the new one
        for (int oldBucket = 0; oldBucket < oldTableSize; oldBucket++) {

            //read the bucket into registers
            int x = oldKeys[oldBucket * AXIS_COUNT + AXIS_X_OFFSET];
            int y = oldKeys[oldBucket * AXIS_COUNT + AXIS_Y_OFFSET];
            int z = oldKeys[oldBucket * AXIS_COUNT + AXIS_Z_OFFSET];
            long value = oldValues[oldBucket];
            if (value == 0L) { //the bucket is unset, so there's no reason to copy it
                continue;
            }

            for (int hash = hashPosition(x, y, z), j = 0; ; j++) {
                int newBucket = (hash + j) & newMask;

                if (newValues[newBucket] == 0L) { //if the bucket value is 0, it means the bucket hasn't been assigned yet
                    //write bucket into new table
                    newKeys[newBucket * AXIS_COUNT + AXIS_X_OFFSET] = x;
                    newKeys[newBucket * AXIS_COUNT + AXIS_Y_OFFSET] = y;
                    newKeys[newBucket * AXIS_COUNT + AXIS_Z_OFFSET] = z;
                    newValues[newBucket] = value;
                    break; //advance to next bucket in old table
                }

                //continue search...
            }
        }
    }

    @Override
    public void forEach(@NonNull Consumer<int[]> callback) {
        this.forEach3D((x, y, z) -> callback.accept(new int[]{ x, y, z }));
    }

    @Override
    public void forEach3D(@NonNull IntIntIntConsumer action) {
        int[] keys = this.keys;
        long[] values = this.values;

        if (keys == null) { //the table isn't even allocated yet, there's nothing to iterate through...
            return;
        }

        for (int bucket = 0; bucket < values.length; bucket++) {
            //read the bucket into registers
            int bucketX = keys[bucket * AXIS_COUNT + AXIS_X_OFFSET];
            int bucketY = keys[bucket * AXIS_COUNT + AXIS_Y_OFFSET];
            int bucketZ = keys[bucket * AXIS_COUNT + AXIS_Z_OFFSET];
            long value = values[bucket];

            while (value != 0L) {
                //this is intrinsic and compiles into TZCNT, which has a latency of 3 cycles - much faster than iterating through all 64 bits
                //  and checking each one individually!
                int index = Long.numberOfTrailingZeros(value);

                //clear the bit in question so that it won't be returned next time around
                value &= ~(1L << index);

                int dx = index >> (BUCKET_AXIS_BITS * 2);
                int dy = (index >> BUCKET_AXIS_BITS) & BUCKET_AXIS_MASK;
                int dz = index & BUCKET_AXIS_MASK;
                action.accept((bucketX << BUCKET_AXIS_BITS) + dx, (bucketY << BUCKET_AXIS_BITS) + dy, (bucketZ << BUCKET_AXIS_BITS) + dz);
            }
        }
    }

    @Override
    public boolean remove(int x, int y, int z) {
        int[] keys = this.keys;
        long[] values = this.values;

        if (keys == null) { //the table isn't even allocated yet, there's nothing to remove...
            return false;
        }

        int mask = this.tableSize - 1; //tableSize is always a power of two, so we can safely create a bitmask like this

        long flag = positionFlag(x, y, z);
        int searchBucketX = x >> BUCKET_AXIS_BITS;
        int searchBucketY = y >> BUCKET_AXIS_BITS;
        int searchBucketZ = z >> BUCKET_AXIS_BITS;
        int hash = hashPosition(searchBucketX, searchBucketY, searchBucketZ);

        for (int i = 0; ; i++) {
            int bucket = (hash + i) & mask;

            //read the bucket into registers
            int bucketX = keys[bucket * AXIS_COUNT + AXIS_X_OFFSET];
            int bucketY = keys[bucket * AXIS_COUNT + AXIS_Y_OFFSET];
            int bucketZ = keys[bucket * AXIS_COUNT + AXIS_Z_OFFSET];
            long value = values[bucket];
            if (value == 0L) { //the bucket is unset. we've reached the end of the bucket chain for this hash, which means
                return false;
            } else if (bucketX != searchBucketX || bucketY != searchBucketY || bucketZ != searchBucketZ) { //the bucket doesn't match, so the search must go on
                continue;
            } else if ((value & flag) == 0L) { //we've found a matching bucket, but the position's flag is unset. there's nothing for us to do...
                return false;
            }

            //the bucket that we found contains the position, so now we remove it from the set
            this.size--;

            if ((value & ~flag) == 0L) { //this position is the only position in the bucket, so we need to delete the bucket
                this.usedBuckets--;

                //shifting the buckets IS expensive, yes, but it'll only happen when the entire bucket is deleted, which won't happen on every removal
                this.shiftBuckets(keys, values, (hash + i) & mask, mask);
            } else { //update bucket value with this position removed
                values[bucket] = value & ~flag;
            }

            return true;
        }
    }

    //adapted from it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap#shiftKeys(int)
    protected void shiftBuckets(int[] keys, long[] values, int pos, int mask) {
        int last;
        int slot;

        int currX;
        int currY;
        int currZ;
        long currValue;

        for (; ; ) {
            pos = ((last = pos) + 1) & mask;
            for (; ; pos = (pos + 1) & mask) {
                if ((currValue = values[pos]) == 0L) { //curr points to an unset bucket
                    values[last] = 0L; //delete last bucket
                    return;
                }

                slot = hashPosition(
                        currX = keys[pos * AXIS_COUNT + AXIS_X_OFFSET],
                        currY = keys[pos * AXIS_COUNT + AXIS_Y_OFFSET],
                        currZ = keys[pos * AXIS_COUNT + AXIS_Z_OFFSET]) & mask;

                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
                    keys[last * AXIS_COUNT + AXIS_X_OFFSET] = currX;
                    keys[last * AXIS_COUNT + AXIS_Y_OFFSET] = currY;
                    keys[last * AXIS_COUNT + AXIS_Z_OFFSET] = currZ;
                    values[last] = currValue;
                    break;
                }
            }
        }
    }

    @Override
    public boolean containsAll(@NonNull NDimensionalIntSet set) {
        return set instanceof Int3HashSet ? this.containsAll((Int3HashSet) set) : NDimensionalIntSet.super.containsAll(set);
    }

    protected boolean containsAll(@NonNull Int3HashSet other) {
        if (this.size < other.size) { //we contain fewer points than the other set, and therefore cannot contain all of them
            return false;
        } else if (other.size == 0) { //other set is empty, we contain everything
            return true;
        }

        //we can assume that neither set is empty, and therefore both sets' tables must be initialized

        long[] thisValues = this.values;

        int[] otherKeys = other.keys;
        long[] otherValues = other.values;

        //iterate over every bucket in the other set
        for (int otherBucket = 0; otherBucket < otherValues.length; otherBucket++) {
            long otherValue = otherValues[otherBucket];
            if (otherValue != 0L) { //the other bucket is occupied
                //find corresponding bucket in self
                int thisBucket = this.findBucket(
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_X_OFFSET],
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_Y_OFFSET],
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_Z_OFFSET], false);

                if (thisBucket < 0 //this set doesn't contain the bucket, all the points in the bucket are missing
                    || (otherValue & ~thisValues[thisBucket]) != 0L) { //the other bucket contains some points which this one doesn't
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean addAll(@NonNull NDimensionalIntSet set) {
        return set instanceof Int3HashSet ? this.addAll((Int3HashSet) set) : NDimensionalIntSet.super.addAll(set);
    }

    protected boolean addAll(@NonNull Int3HashSet other) {
        if (other.size == 0) { //other set is empty, there's nothing to add
            return false;
        }

        //the other set isn't empty, so its table must have been initialized.
        // we can't assume the same about this set, but we wouldn't want to keep a constant reference to this set's table anyway since it could be resized
        // by findBucket() during iteration

        boolean modified = false;

        int[] otherKeys = other.keys;
        long[] otherValues = other.values;

        //iterate over every bucket in the other set
        for (int otherBucket = 0; otherBucket < otherValues.length; otherBucket++) {
            long otherValue = otherValues[otherBucket];
            if (otherValue != 0L) { //the other bucket is occupied
                //find corresponding bucket in self, or create it if none exists
                int thisBucket = this.findBucket(
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_X_OFFSET],
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_Y_OFFSET],
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_Z_OFFSET], true);

                //get current bucket value
                long[] thisValues = this.values; //findBucket() has initialized the table for us if it wasn't already
                long thisValue = thisValues[thisBucket];

                //determine which points this bucket didn't contain before and must be added
                long addedBits = otherValue & ~thisValue;
                if (addedBits != 0L) { //the other bucket contained some points that this one didn't, add them all simultaneously!
                    thisValues[thisBucket] = thisValue | otherValue;
                    this.size = addExact(this.size, Long.bitCount(addedBits)); //increase this set's total size according to the number of added points
                    modified = true; //remember that this set was modified for later
                }
            }
        }

        return modified;
    }

    @Override
    public boolean removeAll(@NonNull NDimensionalIntSet set) {
        return set instanceof Int3HashSet ? this.removeAll((Int3HashSet) set) : NDimensionalIntSet.super.removeAll(set);
    }

    protected boolean removeAll(@NonNull Int3HashSet other) {
        if (this.size == 0) { //this set is empty, there's nothing to be removed
            return false;
        } else if (other.size == 0) { //other set is empty, there's nothing to remove
            return true;
        }

        //we can assume that neither set is empty, and therefore both sets' tables must be initialized

        boolean modified = false;

        int[] thisKeys = this.keys;
        long[] thisValues = this.values;

        int[] otherKeys = other.keys;
        long[] otherValues = other.values;

        //iterate over every bucket in the other set
        for (int otherBucket = 0; otherBucket < otherValues.length; otherBucket++) {
            long otherValue = otherValues[otherBucket];
            if (otherValue != 0L) { //the other bucket is occupied
                //find corresponding bucket in self
                int thisBucket = this.findBucket(
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_X_OFFSET],
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_Y_OFFSET],
                        otherKeys[otherBucket * AXIS_COUNT + AXIS_Z_OFFSET], false);

                if (thisBucket < 0) { //this set doesn't contain the bucket, all the points in the bucket are missing
                    continue;
                }

                long thisValue = thisValues[thisBucket];
                long removedBits = otherValue & thisValue;
                if (removedBits == 0L) { //this bucket doesn't contain any of the points contained by the other bucket, there's nothing to remove
                    continue;
                }

                //decrease this set's total size according to the number of points we removed from this bucket
                this.size -= Long.bitCount(removedBits);

                if (thisValue == removedBits) { //all the points were removed from the bucket! the bucket is now empty and need to be removed
                    this.usedBuckets--;

                    //shifting the buckets IS expensive, yes, but it'll only happen when the entire bucket is deleted, which won't happen on every removal
                    this.shiftBuckets(thisKeys, thisValues, thisBucket, thisValues.length - 1);
                } else { //update bucket value with the positions removed
                    thisValues[thisBucket] = thisValue & ~otherValue;
                }

                modified = true;
            }
        }

        return modified;
    }

    @Override
    public void clear() {
        if (this.isEmpty()) { //if the set is empty, there's nothing to clear
            return;
        }

        //fill the entire table with zeroes
        // (since the table isn't empty, we can be sure that the table has been allocated so there's no reason to check for it)
        Arrays.fill(this.values, 0L);

        //reset all size counters
        this.usedBuckets = 0;
        this.size = 0;
    }

    protected void setTableSize(int tableSize) {
        this.tableSize = tableSize;
        this.resizeThreshold = (tableSize >> 1) + (tableSize >> 2); //count * 0.75
    }

    //
    // NDimensionalIntSet methods
    //

    @Override
    public int dimensions() {
        return 3;
    }

    @Override
    public boolean add(@NonNull int... point) {
        checkArg(point.length == 3);
        return this.add(point[0], point[1], point[2]);
    }

    @Override
    public boolean remove(@NonNull int... point) {
        checkArg(point.length == 3);
        return this.remove(point[0], point[1], point[2]);
    }

    @Override
    public boolean contains(@NonNull int... point) {
        checkArg(point.length == 3);
        return this.contains(point[0], point[1], point[2]);
    }
}
