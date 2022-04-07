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

package util.datastructure;

import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.lib.math.vector.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static util.datastructure.TestNDimensionalIntSet.*;

/**
 * @author DaPorkchop_
 */
public class TestNDimensionalIntSegtreeSet {
    protected static void doAABBIntersections(Set<Vec3i> reference, NDimensionalIntSegtreeSet test, int _min, int _max) {
        int d = abs(_max - _min);
        int min = min(_min, _min - (d >> 1));
        int max = max(_max, _max + (d >> 1));

        IntStream.range(0, 128).parallel().forEach(_unused -> {
            int x0 = ThreadLocalRandom.current().nextInt(min, max);
            int x1 = ThreadLocalRandom.current().nextInt(min, max);
            int y0 = ThreadLocalRandom.current().nextInt(min, max);
            int y1 = ThreadLocalRandom.current().nextInt(min, max);
            int z0 = ThreadLocalRandom.current().nextInt(min, max);
            int z1 = ThreadLocalRandom.current().nextInt(min, max);

            int minX = min(x0, x1);
            int maxX = max(x0, x1);
            int minY = min(y0, y1);
            int maxY = max(y0, y1);
            int minZ = min(z0, z1);
            int maxZ = max(z0, z1);

            boolean ref = reference.stream().anyMatch(v -> v.x() >= minX && v.x() <= maxX && v.y() >= minY && v.y() <= maxY && v.z() >= minZ && v.z() <= maxZ);
            boolean tst = test.containsAny(minX, minY, minZ, maxX, maxY, maxZ);

            checkState(ref == tst);
        });
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
        Set<Vec3i> reference = new HashSet<>(nPoints);
        ThreadLocalRandom r = ThreadLocalRandom.current();

        {
            NDimensionalIntSegtreeSet test = Datastructures.INSTANCE.nDimensionalIntSegtreeSet().dimensions(3).threadSafe(false).build();
            for (int i = 0; i < nPoints; i++) { //insert some random values
                int x = r.nextInt(min, max);
                int y = r.nextInt(min, max);
                int z = r.nextInt(min, max);

                boolean b0 = reference.add(Vec3i.of(x, y, z));
                boolean b1 = test.add(x, y, z);
                if (b0 != b1) {
                    throw new IllegalStateException();
                }
            }

            ensureEqual(reference, test);
            doAABBIntersections(reference, test, min, max);

            for (Iterator<Vec3i> itr = reference.iterator(); itr.hasNext(); ) { //remove some positions at random
                Vec3i pos = itr.next();

                if (r.nextInt(4) == 0) {
                    itr.remove();
                    test.remove(pos.x(), pos.y(), pos.z());
                }
            }

            ensureEqual(reference, test);
            doAABBIntersections(reference, test, min, max);
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
        Set<Vec3i> reference = new HashSet<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();

        {
            NDimensionalIntSegtreeSet test = Datastructures.INSTANCE.nDimensionalIntSegtreeSet().dimensions(3).threadSafe(false).build();
            ensureEqual(reference, test);
            doAABBIntersections(reference, test, min, max);

            for (int i = 0; i < 10000; i++) {
                int x = r.nextInt(min, max);
                int y = r.nextInt(min, max);
                int z = r.nextInt(min, max);

                if (!reference.add(Vec3i.of(x, y, z))) {
                    i--;
                    continue;
                }

                checkState(test.add(x, y, z)); //should be true the first time
                checkState(!test.add(x, y, z)); //should be false the second time
            }

            ensureEqual(reference, test);
            doAABBIntersections(reference, test, min, max);
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
        Set<Vec3i> reference = new HashSet<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();

        {
            NDimensionalIntSegtreeSet test = Datastructures.INSTANCE.nDimensionalIntSegtreeSet().dimensions(3).threadSafe(false).build();
            for (int i = 0; i < 32; i++) {
                int x = r.nextInt(min, max);
                int y = r.nextInt(min, max);
                int z = r.nextInt(min, max);

                checkState(reference.add(Vec3i.of(x, y, z)) == test.add(x, y, z));
            }

            ensureEqual(reference, test);
            doAABBIntersections(reference, test, min, max);

            reference.forEach(pos -> {
                checkState(test.remove(pos.x(), pos.y(), pos.z()));
                checkState(!test.remove(pos.x(), pos.y(), pos.z()));
            });

            ensureEqual(Collections.emptySet(), test);
            doAABBIntersections(Collections.emptySet(), test, min, max);
        }
    }
}
