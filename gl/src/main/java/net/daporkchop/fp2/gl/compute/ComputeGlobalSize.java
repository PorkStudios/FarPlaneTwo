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

package net.daporkchop.fp2.gl.compute;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Defines the global size of a compute shader dispatch (the size of each axis, in work groups).
 *
 * @author DaPorkchop_
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ComputeGlobalSize implements Comparable<ComputeGlobalSize> {
    private final int x;
    private final int y;
    private final int z;

    public ComputeGlobalSize(int x, int y, int z) {
        this.x = positive(x, "x");
        this.y = positive(y, "y");
        this.z = positive(z, "z");
    }

    /**
     * @return the total number of work groups per compute shader dispatch
     */
    public long count() {
        return multiplyExact(multiplyExact((long) this.x, this.y), this.z);
    }

    @Override
    public int compareTo(@NonNull ComputeGlobalSize o) {
        int d;
        if ((d = Long.compare(this.count(), o.count())) == 0
            && (d = Integer.compare(this.x, o.x)) == 0
            && (d = Integer.compare(this.y, o.y)) == 0) {
            d = Integer.compare(this.z, o.z);
        }
        return d;
    }
}
