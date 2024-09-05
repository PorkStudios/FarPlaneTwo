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

package net.daporkchop.fp2.core.test.util.datastructure;

import lombok.val;
import net.daporkchop.fp2.core.util.datastructure.java.list.ListUtils;
import net.daporkchop.lib.common.util.PArrays;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestListUtils {
    @Test
    public void testRepeatSequence() {
        for (int times = 0; times <= 10; times++) {
            assertEquals(
                    ListUtils.repeatSequence(Collections.emptyList(), times),
                    Collections.emptyList(),
                    String.valueOf(times));
        }

        for (int times = 0; times <= 10; times++) {
            assertEquals(
                    ListUtils.repeatSequence(Collections.singletonList(0), times),
                    Arrays.asList(PArrays.filledUnchecked(times, 0)),
                    String.valueOf(times));
        }

        for (int length = 0; length <= 10; length++) {
            val sequence = IntStream.range(0, length).boxed().collect(Collectors.toList());
            for (int times = 0; times <= 10; times++) {
                assertEquals(
                        ListUtils.repeatSequence(sequence, times),
                        IntStream.range(0, times).boxed().flatMap(unused -> sequence.stream()).collect(Collectors.toList()),
                        "length=" + length + ", times=" + times);
            }
        }
    }
}
