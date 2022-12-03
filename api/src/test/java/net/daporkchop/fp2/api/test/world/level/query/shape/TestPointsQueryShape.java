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

package net.daporkchop.fp2.api.test.world.level.query.shape;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestPointsQueryShape {
    //
    // QueryShape and implementations
    //

    @Test
    public void testQueryShape_SinglePoint() {
        IntStream.range(0, 1024).parallel().forEach(_unused -> {
            int x = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
            int y = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
            int z = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);

            this.testQueryShape(new PointsQueryShape.Single(x, y, z));
        });
    }

    @Test
    public void testQueryShape_MultiPoints() {
        //separate
        IntStream.range(0, 1024).parallel().forEach(_unused -> {
            int count = ThreadLocalRandom.current().nextInt(4096);
            int[] x = ThreadLocalRandom.current().ints(count).toArray();
            int[] y = ThreadLocalRandom.current().ints(count).toArray();
            int[] z = ThreadLocalRandom.current().ints(count).toArray();

            this.testQueryShape(new PointsQueryShape.Multi(x, 0, 1, y, 0, 1, z, 0, 1, count));
        });

        //interleaved
        IntStream.range(0, 1024).parallel().forEach(_unused -> {
            int count = ThreadLocalRandom.current().nextInt(4096);
            int[] positions = ThreadLocalRandom.current().ints(count * 3).toArray();

            this.testQueryShape(new PointsQueryShape.Multi(positions, 0, 3, positions, 1, 3, positions, 2, 3, count));
        });
    }

    @Test
    public void testQueryShape_OriginSizeStride() {
        IntStream.range(0, 1024).parallel().forEach(_unused -> {
            int originX = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE >> 1, Integer.MAX_VALUE >> 1);
            int originY = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE >> 1, Integer.MAX_VALUE >> 1);
            int originZ = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE >> 1, Integer.MAX_VALUE >> 1);
            int sizeX = ThreadLocalRandom.current().nextInt(64);
            int sizeY = ThreadLocalRandom.current().nextInt(64);
            int sizeZ = ThreadLocalRandom.current().nextInt(64);
            int strideX = ThreadLocalRandom.current().nextInt(65536);
            int strideY = ThreadLocalRandom.current().nextInt(65536);
            int strideZ = ThreadLocalRandom.current().nextInt(65536);

            this.testQueryShape(new PointsQueryShape.OriginSizeStride(originX, originY, originZ, sizeX, sizeY, sizeZ, strideX, strideY, strideZ));
        });
    }

    private void testQueryShape(@NonNull PointsQueryShape shape) {
        shape.validate();

        //keep track of which voxel position indices have been visited so far
        BitSet visited = new BitSet(shape.count());
        int[] position = new int[3];

        //iterate over all the positions
        shape.forEach((index, x0, y0, z0) -> {
            checkState(!visited.get(index), "voxel position #%d (%d,%d,%d) was visited twice", index, x0, y0, z0);
            visited.set(index);

            //compare with per-axis getters
            int x1 = shape.x(index);
            int y1 = shape.y(index);
            int z1 = shape.z(index);
            checkState(x0 == x1 && y0 == y1 && z0 == z1, "voxel position #%d has inconsistent positions between forEach() and x()/y()/z()", index);

            //compare with full position getter
            shape.position(index, position);
            checkState(x0 == position[0]
                       && y0 == position[1]
                       && z0 == position[2], "voxel position #%d has inconsistent positions between forEach() and position()", index);
        });

        //make sure we visited every position
        checkState(visited.cardinality() == shape.count(), "only visited %d/%d voxel position indices!", visited.cardinality(), shape.count());
    }
}
