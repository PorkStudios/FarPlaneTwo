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

package net.daporkchop.fp2.core.test.util.datastructure;

import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.util.TilePosArrayList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestTilePosList {
    protected static <POS extends IFarPos> void ensureEqual(List<POS> reference, List<POS> test) {
        checkState(reference.size() == test.size());

        checkState(reference.equals(test));
        checkState(test.equals(reference));
    }

    @Test
    public void testVoxelPosArrayList() {
        this.testPosList(TilePosArrayList::new, r -> new TilePos(r.nextInt(0, EngineConstants.MAX_LODS), r.nextInt(), r.nextInt(), r.nextInt()));
    }

    protected <POS extends IFarPos> void testPosList(Supplier<? extends List<POS>> listFactory, Function<ThreadLocalRandom, POS> randomPOSFunction) {
        IntStream.range(0, 1024).parallel().forEach(_unused -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();

            List<POS> reference = new ArrayList<>();
            List<POS> test = listFactory.get();

            //append some random positions
            for (int i = 0; i < 1000; i++) {
                POS pos = randomPOSFunction.apply(r);
                reference.add(pos);
                test.add(pos);
            }

            ensureEqual(reference, test);

            //insert some random positions
            for (int i = 0; i < 32; i++) {
                int index = r.nextInt(reference.size());
                POS pos = randomPOSFunction.apply(r);
                reference.add(index, pos);
                test.add(index, pos);
            }

            ensureEqual(reference, test);

            //replace some random positions
            for (int i = 0; i < 32; i++) {
                int index = r.nextInt(reference.size());
                POS pos = randomPOSFunction.apply(r);
                reference.set(index, pos);
                test.set(index, pos);
            }

            ensureEqual(reference, test);

            //remove some positions at random indices
            for (int i = 0; i < 32; i++) {
                int index = r.nextInt(reference.size());
                POS p0 = reference.remove(index);
                POS p1 = test.remove(index);
                checkState(Objects.equals(p0, p1));
            }

            ensureEqual(reference, test);

            //removeIf half the positions at random
            {
                long seed = r.nextLong();
                SplittableRandom r0 = new SplittableRandom(seed);
                SplittableRandom r1 = new SplittableRandom(seed);
                reference.removeIf(pos -> r0.nextBoolean());
                test.removeIf(pos -> r1.nextBoolean());
            }

            ensureEqual(reference, test);

            //clear a random sub-range of the list
            {
                int low = r.nextInt(0, reference.size() >> 1);
                int high = r.nextInt(reference.size() >> 1, reference.size());
                reference.subList(low, high).clear();
                test.subList(low, high).clear();
            }

            ensureEqual(reference, test);

            //duplicate the lists using addAll
            List<POS> reference2 = new ArrayList<>();
            reference2.addAll(reference);
            List<POS> test2 = listFactory.get();
            test2.addAll(test);

            ensureEqual(reference, reference2);
            ensureEqual(test, test2);
            ensureEqual(reference2, test2);

            //addAll the lists again
            reference2.addAll(reference);
            test2.addAll(test);

            ensureEqual(reference2, test2);
            ensureEqual(reference, reference2.subList(0, reference.size()));
            ensureEqual(reference, reference2.subList(reference.size(), reference2.size()));
            ensureEqual(test, test2.subList(0, test.size()));
            ensureEqual(test, test2.subList(test.size(), test2.size()));

            //addAll the lists multiple time at random starting indices
            for (int i = 0; i < 32; i++) {
                int index = r.nextInt(reference2.size());
                List<POS> srcList = r.nextBoolean() ? reference : test;
                reference2.addAll(index, srcList);
                test2.addAll(index, srcList);
            }

            ensureEqual(reference2, test2);
        });
    }
}
