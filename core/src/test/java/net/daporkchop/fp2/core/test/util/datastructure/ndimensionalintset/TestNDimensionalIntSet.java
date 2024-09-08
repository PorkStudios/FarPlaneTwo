/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.test.util.datastructure.ndimensionalintset;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class TestNDimensionalIntSet<IntSet extends NDimensionalIntSet> {
    private void ensureEqual(Set<int[]> reference, IntSet test) {
        checkState(reference.size() == test.size());

        reference.forEach(point -> checkState(test.contains(point)));
        test.forEach(point -> checkState(reference.contains(point)));
    }

    protected final int dims;

    protected abstract IntSet makeSet();

    protected static final Hash.Strategy<int[]> INT_ARRAY_STRATEGY = new Hash.Strategy<int[]>() {
        @Override
        public int hashCode(int[] o) {
            return Arrays.hashCode(o);
        }

        @Override
        public boolean equals(int[] a, int[] b) {
            return Arrays.equals(a, b);
        }
    };

    protected Set<int[]> makeArraySet() {
        return this.makeArraySet(16);
    }

    protected Set<int[]> makeArraySet(int initialCapacity) {
        return new ObjectOpenCustomHashSet<>(initialCapacity, INT_ARRAY_STRATEGY);
    }

    protected SplittableRandom makeRandom() {
        return new SplittableRandom(1337L);
    }

    protected int[] getPoint(SplittableRandom rng, int min, int max) {
        int[] result = new int[this.dims];
        for (int i = 0; i < result.length; i++) {
            result[i] = rng.nextInt(min, max);
        }
        return result;
    }

    @Test
    public void test1000BigCoordinates() {
        this.test(1000, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void test100000BigCoordinates() {
        this.test(100000, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void test1000SmallCoordinates() {
        this.test(1000, -500, 500);
    }

    @Test
    public void test1000000SmallCoordinates() {
        this.test(1000000, -500, 500);
    }

    protected void test(int nPoints, int min, int max) {
        val reference = this.makeArraySet(nPoints);
        val r = this.makeRandom();

        {
            val test = this.makeSet();
            for (int i = 0; i < nPoints; i++) { //insert some random values
                int[] point = this.getPoint(r, min, max);

                boolean b0 = reference.add(point);
                boolean b1 = test.add(point);
                if (b0 != b1) {
                    throw new IllegalStateException();
                }
            }

            this.ensureEqual(reference, test);

            for (Iterator<int[]> itr = reference.iterator(); itr.hasNext(); ) { //remove some positions at random
                val point = itr.next();

                if (r.nextInt(4) == 0) {
                    itr.remove();
                    test.remove(point);
                }
            }

            this.ensureEqual(reference, test);
        }
    }

    @Test
    public void testDuplicateInsertionBigCoordinates() {
        this.testDuplicateInsertion(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void testDuplicateInsertionSmallCoordinates() {
        this.testDuplicateInsertion(-500, 500);
    }

    protected void testDuplicateInsertion(int min, int max) {
        val reference = this.makeArraySet();
        val r = this.makeRandom();

        {
            val test = this.makeSet();
            this.ensureEqual(reference, test);

            //in the 1d case, 10000 iterations is too many
            int itrs = Math.min(10000, (int) (Math.pow(max - min, this.dims) * 0.5d));

            for (int i = 0; i < itrs; i++) {
                int[] point = this.getPoint(r, min, max);

                if (!reference.add(point)) {
                    i--;
                    continue;
                }

                checkState(test.add(point)); //should be true the first time
                checkState(!test.add(point)); //should be false the second time
            }

            this.ensureEqual(reference, test);
        }
    }

    @Test
    public void testDuplicateRemovalBigCoordinates() {
        this.testDuplicateRemoval(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void testDuplicateRemovalSmallCoordinates() {
        this.testDuplicateRemoval(-500, 500);
    }

    protected void testDuplicateRemoval(int min, int max) {
        val reference = this.makeArraySet();
        val r = this.makeRandom();

        {
            val test = this.makeSet();
            this.ensureEqual(reference, test);

            for (int i = 0; i < 10000; i++) {
                int[] point = this.getPoint(r, min, max);

                checkState(reference.add(point) == test.add(point));
            }

            this.ensureEqual(reference, test);

            reference.forEach(point -> {
                checkState(test.remove(point));
                checkState(!test.remove(point));
            });

            this.ensureEqual(Collections.emptySet(), test);
        }
    }

    @Test
    public void testBulkOperationsHighCoordinates() {
        this.testBulkOperations(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void testBulkOperationsSmallCoordinates() {
        this.testBulkOperations(-500, 500);
    }

    protected void testBulkOperations(int min, int max) {
        val reference0 = this.makeArraySet();
        val reference1 = this.makeArraySet();
        val r = this.makeRandom();

        {
            val test0 = this.makeSet();
            val test1 = this.makeSet();

            //in the 1d case, 10000 iterations is too many
            int itrs = Math.min(10000, (int) (Math.pow(max - min, this.dims) * 0.5d));

            for (int i = 0; i < itrs; i++) {
                int[] point = this.getPoint(r, min, max);

                if (r.nextBoolean()) {
                    checkState(reference0.add(point) == test0.add(point));
                }
                if (r.nextBoolean()) {
                    checkState(reference1.add(point) == test1.add(point));
                }
            }

            this.ensureEqual(reference0, test0);
            this.ensureEqual(reference1, test1);

            checkState(reference1.addAll(reference0) == test1.addAll(test0));

            this.ensureEqual(reference0, test0);
            this.ensureEqual(reference1, test1);

            checkState(reference1.containsAll(reference0));
            checkState(test1.containsAll(test0));

            checkState(reference1.removeAll(reference0) == test1.removeAll(test0));
        }
    }

    @Test
    public void testCountInRangeSmallCoordinates() {
        this.testCountInRange(-500, 500);
    }

    protected static Stream<int[]> allPointsBetween(int[] begin, int[] end) {
        checkArg(begin.length == end.length);
        if (begin.length == 0) {
            return Stream.of(new int[0]);
        } else {
            int beginCoord = begin[begin.length - 1];
            int endCoord = end[end.length - 1];
            checkArg(beginCoord <= endCoord);

            return allPointsBetween(Arrays.copyOf(begin, begin.length - 1), Arrays.copyOf(end, end.length - 1)).flatMap(point -> IntStream.range(beginCoord, endCoord)
                    .mapToObj(i -> {
                        int[] result = Arrays.copyOf(point, point.length + 1);
                        result[point.length] = i;
                        return result;
                    }));
        }
    }

    protected void testCountInRange(int min, int max) {
        val reference = this.makeArraySet();
        val r = this.makeRandom();

        {
            val test = this.makeSet();
            this.ensureEqual(reference, test);

            //in the 1d case, 10000 iterations is too many
            int itrs = Math.min(10000, (int) (Math.pow(max - min, this.dims) * 0.5d));

            for (int i = 0; i < itrs; i++) {
                int[] point = this.getPoint(r, min, max);
                checkState(reference.add(point) == test.add(point));
            }

            this.ensureEqual(reference, test);

            for (int i = 0; i < 10000; i++) {
                int[] begin = this.getPoint(r, min, max);
                int[] end = Arrays.stream(begin).map(j -> j + r.nextInt(1, (max - min) / 16)).toArray();

                this.testCountInRangeBetween(reference, test, begin, end);
            }

            for (int i = 0; i < 10; i++) {
                int[] begin = this.getPoint(r, min, max);
                int[] end = Arrays.stream(begin).map(j -> j + 128).toArray();

                this.testCountInRangeBetween(reference, test, begin, end);
            }
        }
    }

    protected void testCountInRangeBetween(Set<int[]> reference, IntSet test, int[] begin, int[] end) {
        val expected = allPointsBetween(begin, end).filter(reference::contains).count();
        val computed = test.countInRange(begin, end);
        checkState(expected == computed, "%s != %s", expected, computed);
    }

    @Test
    public void testAddAllInRangeSmallCoordinates() {
        this.testAddAllInRange(-500, 500);
    }

    protected void testAddAllInRangeBetween(Set<int[]> reference, IntSet test, int[] begin, int[] end) {
        val expected = allPointsBetween(begin, end).filter(reference::add).count();
        val computed = test.addAllInRange(begin, end);
        checkState(expected == computed, "%s != %s", expected, computed);
    }

    protected void testAddAllInRange(int min, int max) {
        val reference = this.makeArraySet();
        val r = this.makeRandom();

        {
            val test = this.makeSet();
            this.ensureEqual(reference, test);

            for (int i = 0; i < 100; i++) {
                if (r.nextBoolean()) {
                    test.clear();
                    reference.clear();
                }

                int[] begin = this.getPoint(r, min, max);
                int[] end = Arrays.stream(begin).map(j -> j + r.nextInt(1, (max - min) / 16)).toArray();

                this.testAddAllInRangeBetween(reference, test, begin, end);
                this.ensureEqual(reference, test);
            }

            for (int i = 0; i < 10; i++) {
                if (r.nextBoolean()) {
                    test.clear();
                    reference.clear();
                }

                int[] begin = this.getPoint(r, min, max);
                int[] end = Arrays.stream(begin).map(j -> j + 128).toArray();

                this.testAddAllInRangeBetween(reference, test, begin, end);
                this.ensureEqual(reference, test);
            }
        }
    }
}
