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

package net.daporkchop.fp2.api.test.util.math;

import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestIntAxisAlignedBB {
    private static void check(BiPredicate<IntAxisAlignedBB, IntAxisAlignedBB> op, boolean expected, boolean symmetric, String name, IntAxisAlignedBB a, IntAxisAlignedBB b) {
        checkState(op.test(a, b) == expected, "a: %s should %s%s b: %s", a, expected ? "" : "not ", name, b);
        checkState(!symmetric || op.test(b, a) == expected, "b: %s should %s%s a: %s", b, expected ? "" : "not ", name, a);
    }

    private static void checkContains(boolean expected, IntAxisAlignedBB a, IntAxisAlignedBB b) {
        check(IntAxisAlignedBB::contains, expected, false, "contain", a, b);
    }

    private static void checkIntersects(boolean expected, IntAxisAlignedBB a, IntAxisAlignedBB b) {
        check(IntAxisAlignedBB::intersects, expected, true, "intersect", a, b);
    }

    @Test
    public void testContains() {
        for (int i = 1; i <= 2; i++) {
            checkContains(true,
                    new IntAxisAlignedBB(0, 0, 0, i, i, i),
                    new IntAxisAlignedBB(0, 0, 0, 1, 1, 1));

            checkContains(false,
                    new IntAxisAlignedBB(0, 0, 0, i, i, i),
                    new IntAxisAlignedBB(-1, -1, -1, 1, 1, 1));
        }

        for (int i = 0; i < 3; i++) {
            checkContains(true,
                    new IntAxisAlignedBB(0, 0, 0, 1 + ((1 << i) & 1), 1 + (((1 << i) & 2) >> 1), 1 + (((1 << i) & 4) >> 2)),
                    new IntAxisAlignedBB(0, 0, 0, 1, 1, 1));
        }

        for (int x = -5; x <= 4; x++) {
            for (int y = -5; y <= 4; y++) {
                for (int z = -5; z <= 4; z++) {
                    checkContains(true,
                            new IntAxisAlignedBB(-5, -5, -5, 5, 5, 5),
                            new IntAxisAlignedBB(x, y, z, x + 1, y + 1, z + 1));
                }
            }
        }

        for (int x = -6; x <= 5; x++) {
            for (int y = -6; y <= 5; y++) {
                for (int z = -6; z <= 5; z++) {
                    if (min(min(x, y), z) == -6 || max(max(x, y), z) == 5) {
                        checkContains(false,
                                new IntAxisAlignedBB(-5, -5, -5, 5, 5, 5),
                                new IntAxisAlignedBB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
    }
    
    @Test
    public void testIntersects() {
        for (int i = 1; i <= 2; i++) {
            checkIntersects(true,
                    new IntAxisAlignedBB(0, 0, 0, i, i, i),
                    new IntAxisAlignedBB(0, 0, 0, 1, 1, 1));

            checkIntersects(true,
                    new IntAxisAlignedBB(0, 0, 0, i, i, i),
                    new IntAxisAlignedBB(-1, -1, -1, 1, 1, 1));
        }

        for (int i = 0; i < 3; i++) {
            checkIntersects(true,
                    new IntAxisAlignedBB(0, 0, 0, 1 + ((1 << i) & 1), 1 + (((1 << i) & 2) >> 1), 1 + (((1 << i) & 4) >> 2)),
                    new IntAxisAlignedBB(0, 0, 0, 1, 1, 1));
        }

        for (int x = -5; x <= 4; x++) {
            for (int z = -5; z <= 4; z++) {
                checkIntersects(true,
                        new IntAxisAlignedBB(-5, 0, -5, 5, 1, 5),
                        new IntAxisAlignedBB(x, -100, z, x + 1, 100, z + 1));

                checkIntersects(true,
                        new IntAxisAlignedBB(-5, 0, -5, 5, 1, 5),
                        new IntAxisAlignedBB(x, 0, z, x + 1, 100, z + 1));

                checkIntersects(false,
                        new IntAxisAlignedBB(-5, 0, -5, 5, 1, 5),
                        new IntAxisAlignedBB(x, 1, z, x + 1, 100, z + 1));
            }
        }

        for (int x = -6; x <= 5; x++) {
            for (int z = -6; z <= 6; z++) {
                if (min(x, z) == -6 || max(x, z) == 5) {
                    checkIntersects(false,
                            new IntAxisAlignedBB(-5, 0, -5, 5, 1, 5),
                            new IntAxisAlignedBB(x, -100, z, x + 1, 100, z + 1));

                    checkIntersects(false,
                            new IntAxisAlignedBB(-5, 0, -5, 5, 1, 5),
                            new IntAxisAlignedBB(x, 0, z, x + 1, 100, z + 1));

                    checkIntersects(false,
                            new IntAxisAlignedBB(-5, 0, -5, 5, 1, 5),
                            new IntAxisAlignedBB(x, 1, z, x + 1, 100, z + 1));
                }
            }
        }
    }
}
