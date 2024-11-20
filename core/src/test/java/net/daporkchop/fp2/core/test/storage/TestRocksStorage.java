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

package net.daporkchop.fp2.core.test.storage;

import lombok.val;
import net.daporkchop.fp2.core.storage.rocks.RocksStorage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestRocksStorage {
    private static int signum(int i) {
        return Integer.compare(i, 0);
    }

    @Test
    public void testCompareBytesLex() {
        for (int len = 0; len < 256; len++) {
            assertEquals(0, RocksStorage.compareBytesLex(new byte[len], new byte[len]));
        }

        val rng = ThreadLocalRandom.current();
        for (int len = 0; len < 256; len++) {
            byte b0 = (byte) rng.nextInt();
            byte b1 = (byte) rng.nextInt();
            int expectedDiff = Integer.compare(b0 & 0xFF, b1 & 0xFF);

            byte[] a0 = new byte[len];
            rng.nextBytes(a0);
            byte[] a1 = a0.clone();

            for (int i = 0; i < len; i++) {
                byte oldByte = a0[i];
                a0[i] = b0;
                a1[i] = b1;
                assertEquals(expectedDiff, signum(RocksStorage.compareBytesLex(a0, a1)));
                a0[i] = a1[i] = oldByte;
            }
        }
    }
}
