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

package net.daporkchop.fp2.util.alloc;

import lombok.NonNull;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.common.util.PValidation;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.ArrayDeque;
import java.util.Deque;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A simple pooling allocator for {@code int[]}s.
 * <p>
 * Not thread-safe!
 *
 * @author DaPorkchop_
 */
public class IntArrayAllocator {
    public static final Ref<IntArrayAllocator> DEFAULT = ThreadRef.soft(() -> new IntArrayAllocator(32));

    protected final Deque<int[]>[] arenas;
    protected final int maxArenaSize;

    public IntArrayAllocator(int maxArenaSize) {
        this.maxArenaSize = PValidation.positive(maxArenaSize);
        this.arenas = PorkUtil.uncheckedCast(PArrays.filled(32, Deque[]::new, () -> new ArrayDeque(maxArenaSize)));
    }

    public int[] get(int minSize) {
        int minRequiredBits = 32 - Integer.numberOfLeadingZeros(positive(minSize, "minSize") - 1);
        int[] arr = this.arenas[minRequiredBits].pollLast();
        return arr != null ? arr : new int[1 << minRequiredBits];
    }

    public void release(@NonNull int[] arr) {
        int length = arr.length;
        checkArg(length != 0 && BinMath.isPow2(length), "invalid array length: %s", length);

        int minRequiredBits = 32 - Integer.numberOfLeadingZeros(length - 1);
        Deque<int[]> arena = this.arenas[minRequiredBits];
        if (arena.size() < this.maxArenaSize) {
            arena.addLast(arr);
        }
    }
}
