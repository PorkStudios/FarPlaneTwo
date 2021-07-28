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

package net.daporkchop.fp2.util.datastructure.java;

import net.daporkchop.fp2.util.datastructure.Datastructures;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintsegtree.JavaNDimensionalIntSegtreeSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintsegtree.SynchronizedNDimensionalIntSegtreeSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintsegtree.AsyncInitializedNDimensionalIntSegtreeSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintset.Int1HashSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintset.Int2HashSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintset.Int3HashSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintset.JavaNDimensionalIntHashSet;
import net.daporkchop.fp2.util.datastructure.java.ndimensionalintset.SynchronizedNDimensionalIntSet;

import java.util.stream.Stream;

/**
 * {@link Datastructures} implementation which provides pure-Java implementations of all constructed datastructures.
 *
 * @author DaPorkchop_
 */
public class JavaDatastructures implements Datastructures {
    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public NDimensionalIntSet.Builder nDimensionalIntSet() {
        return new NDimensionalIntSet.Builder() {
            @Override
            protected NDimensionalIntSet buildThreadSafe() {
                return new SynchronizedNDimensionalIntSet(this.buildNotThreadSafe());
            }

            @Override
            protected NDimensionalIntSet buildNotThreadSafe() {
                switch (this.dimensions) {
                    case 1:
                        return new Int1HashSet();
                    case 2:
                        return new Int2HashSet();
                    case 3:
                        return new Int3HashSet();
                    default:
                        return new JavaNDimensionalIntHashSet(this.dimensions);
                }
            }
        };
    }

    @Override
    public NDimensionalIntSegtreeSet.Builder nDimensionalIntSegtreeSet() {
        return new NDimensionalIntSegtreeSet.Builder() {
            @Override
            protected NDimensionalIntSegtreeSet buildThreadSafe() {
                NDimensionalIntSegtreeSet set = new SynchronizedNDimensionalIntSegtreeSet(this.buildNotThreadSafe());
                if (this.initialPoints != null) {
                    set = new AsyncInitializedNDimensionalIntSegtreeSet(set, this.initialPoints);
                }
                return set;
            }

            @Override
            protected NDimensionalIntSegtreeSet buildNotThreadSafe() {
                NDimensionalIntSegtreeSet set = new JavaNDimensionalIntSegtreeSet(this.dimensions, JavaDatastructures.this);
                if (!this.threadSafe && this.initialPoints != null) {
                    try (Stream<int[]> stream = this.initialPoints.get()) {
                        stream.forEach(set::add);
                    }
                }
                return set;
            }
        };
    }
}
