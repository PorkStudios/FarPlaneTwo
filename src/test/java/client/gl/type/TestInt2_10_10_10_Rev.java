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

package client.gl.type;

import net.daporkchop.fp2.client.gl.type.Int2_10_10_10_Rev;
import org.junit.Test;

import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 * @see Int2_10_10_10_Rev
 */
public class TestInt2_10_10_10_Rev {
    @Test
    public void test() {
        //there can't be any bugs if i test every possible value!
        IntStream.rangeClosed(Int2_10_10_10_Rev.MIN_XYZ_VALUE, Int2_10_10_10_Rev.MAX_XYZ_VALUE).parallel()
                .forEach(x -> {
                    for (int y = Int2_10_10_10_Rev.MIN_XYZ_VALUE; y <= Int2_10_10_10_Rev.MAX_XYZ_VALUE; y++) {
                        for (int z = Int2_10_10_10_Rev.MIN_XYZ_VALUE; z <= Int2_10_10_10_Rev.MAX_XYZ_VALUE; z++) {
                            int pos = Int2_10_10_10_Rev.packXYZ(x, y, z);

                            int ux = Int2_10_10_10_Rev.unpackX(pos);
                            int uy = Int2_10_10_10_Rev.unpackY(pos);
                            int uz = Int2_10_10_10_Rev.unpackZ(pos);
                            checkState(x == ux, "x: %d != %d", x, ux);
                            checkState(y == uy, "y: %d != %d", y, uy);
                            checkState(z == uz, "z: %d != %d", z, uz);
                        }
                    }
                });
    }
}
