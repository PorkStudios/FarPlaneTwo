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

package net.daporkchop.fp2.util.datastructure;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
//TODO: make this more memory-efficient
@RequiredArgsConstructor
public class ConcurrentBooleanHashSegtreeInt {
    protected static final int MAX_LEVELS = Integer.SIZE;

    protected final Set<Key> set = ConcurrentHashMap.newKeySet();
    protected final int dims;

    public void set(@NonNull int... coords) {
        checkArg(coords.length == this.dims, "expected %d-dimensional array, but found %d!", this.dims, coords.length);

        Key key = new Key(0, coords);
        for (int i = 0; i < MAX_LEVELS && this.set.add(key); i++, key = key.up()) {
        }
    }

    public boolean isSet(int level, @NonNull int... coords) {
        checkArg(coords.length == this.dims, "expected %d-dimensional array, but found %d!", this.dims, coords.length);

        return this.set.contains(new Key(level, coords));
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    protected static final class Key {
        protected final int level;
        @NonNull
        protected final int[] coords;

        public Key up() {
            return new Key(this.level + 1, IntStream.of(this.coords).map(i -> i >> 1).toArray());
        }
    }
}
