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

package net.daporkchop.fp2.core.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Comparator;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class EqualsTieBreakComparator<T> implements Comparator<T> {
    protected final Comparator<T> comparator;
    protected final boolean useHashCode;
    protected final boolean up;

    @Override
    public int compare(T o1, T o2) {
        int d;
        if (this.comparator != null)    {
            d = this.comparator.compare(o1, o2);
        } else {
            d = PorkUtil.<Comparable<T>>uncheckedCast(o1).compareTo(o2);
        }

        if (d == 0 && !o1.equals(o2)) { //comparison resulted in a tie, but the objects are different
            if (this.useHashCode)   {
                d = Integer.compare(o1.hashCode(), o2.hashCode());
            }
            if (d == 0) {
                d = this.up ? 1 : -1;
            }
        }
        return d;
    }
}
