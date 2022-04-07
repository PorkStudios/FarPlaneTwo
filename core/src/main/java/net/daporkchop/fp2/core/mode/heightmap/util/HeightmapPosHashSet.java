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
 *
 */

package net.daporkchop.fp2.core.mode.heightmap.util;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.common.util.AbstractPosHashSet;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintset.Int2HashSet;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;

/**
 * Implementation of {@link Set} optimized specifically for {@link HeightmapPos}.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public class HeightmapPosHashSet extends AbstractPosHashSet<HeightmapPos, Int2HashSet> {
    public HeightmapPosHashSet() {
        super(new Int2HashSet[HMAX_LODS]);
    }

    public HeightmapPosHashSet(Collection<? extends HeightmapPos> c) {
        super(new Int2HashSet[HMAX_LODS], c);
    }

    @Override
    protected Int2HashSet createSet() {
        return new Int2HashSet();
    }

    @Override
    protected Int2HashSet cloneSet(Int2HashSet src) {
        return new Int2HashSet(src);
    }

    @Override
    public boolean add(HeightmapPos pos) {
        return this.getOrCreateDelegate(pos.level()).add(pos.x(), pos.z());
    }

    @Override
    public boolean remove(Object value) {
        if (value instanceof HeightmapPos) {
            HeightmapPos pos = (HeightmapPos) value;

            Int2HashSet delegate = this.delegates[pos.level()];
            return delegate != null && delegate.remove(pos.x(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(Object value) {
        if (value instanceof HeightmapPos) {
            HeightmapPos pos = (HeightmapPos) value;

            Int2HashSet delegate = this.delegates[pos.level()];
            return delegate != null && delegate.contains(pos.x(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super HeightmapPos> callback) {
        Int2HashSet[] delegates = this.delegates;

        for (int level = 0; level < delegates.length; level++) {
            Int2HashSet delegate = delegates[level];
            if (delegate != null) {
                int levelButFinal = level; //damn you java
                delegate.forEach2D((x, y) -> callback.accept(new HeightmapPos(levelButFinal, x, y)));
            }
        }
    }
}
