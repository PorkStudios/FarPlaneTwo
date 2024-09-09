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

package net.daporkchop.fp2.core.test.engine;

import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.util.math.MathUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestTilePos {
    private static TilePos[] allPositionsInBB_correct(int level, int xIn, int yIn, int zIn, int offsetMin, int offsetMax) {
        List<TilePos> out = new ArrayList<>(MathUtil.cb(offsetMin + offsetMax + 1));
        for (int x = xIn - offsetMin; x <= xIn + offsetMax; x++) {
            for (int y = yIn - offsetMin; y <= yIn + offsetMax; y++) {
                for (int z = zIn - offsetMin; z <= zIn + offsetMax; z++) {
                    out.add(new TilePos(level, x, y, z));
                }
            }
        }
        return out.toArray(new TilePos[0]);
    }

    private static void testAllPositionsInBB(int level, int xIn, int yIn, int zIn, int offsetMin, int offsetMax) {
        notNegative(offsetMin, "offsetMin");
        notNegative(offsetMax, "offsetMax");

        assertArrayEquals(
                allPositionsInBB_correct(level, xIn, yIn, zIn, offsetMin, offsetMax),
                new TilePos(level, xIn, yIn, zIn).allPositionsInBB(offsetMin, offsetMax).toArray(TilePos[]::new));
    }

    @Test
    public void testAllPositionsInBB() {
        int[] coords = { 0, -1, -1000, 1000, 192748762 };
        int[] offsets = { 0, 1, 2 };

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            for (int x : coords) {
                for (int y : coords) {
                    for (int z : coords) {
                        for (int offsetMin : offsets) {
                            for (int offsetMax : offsets) {
                                testAllPositionsInBB(level, x, y, z, offsetMin, offsetMax);
                            }
                        }
                    }
                }
            }
        }
    }
}
