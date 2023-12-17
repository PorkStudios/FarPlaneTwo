/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.util;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.common.util.AbstractPosHashSet;
import net.daporkchop.fp2.core.engine.VoxelPos;
import net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintset.Int3HashSet;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.engine.VoxelConstants.*;

/**
 * Implementation of {@link Set} optimized specifically for {@link VoxelPos}.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public class VoxelPosHashSet extends AbstractPosHashSet<VoxelPos, Int3HashSet> {
    public VoxelPosHashSet() {
        super(new Int3HashSet[VMAX_LODS]);
    }

    public VoxelPosHashSet(Collection<? extends VoxelPos> c) {
        super(new Int3HashSet[VMAX_LODS], c);
    }

    @Override
    protected Int3HashSet createSet() {
        return new Int3HashSet();
    }

    @Override
    protected Int3HashSet cloneSet(Int3HashSet src) {
        return new Int3HashSet(src);
    }

    @Override
    public boolean add(VoxelPos pos) {
        return this.getOrCreateDelegate(pos.level()).add(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean remove(Object value) {
        if (value instanceof VoxelPos) {
            VoxelPos pos = (VoxelPos) value;

            Int3HashSet delegate = this.delegates[pos.level()];
            return delegate != null && delegate.remove(pos.x(), pos.y(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(Object value) {
        if (value instanceof VoxelPos) {
            VoxelPos pos = (VoxelPos) value;

            Int3HashSet delegate = this.delegates[pos.level()];
            return delegate != null && delegate.contains(pos.x(), pos.y(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super VoxelPos> callback) {
        Int3HashSet[] delegates = this.delegates;

        for (int level = 0; level < delegates.length; level++) {
            Int3HashSet delegate = delegates[level];
            if (delegate != null) {
                int levelButFinal = level; //damn you java
                delegate.forEach3D((x, y, z) -> callback.accept(new VoxelPos(levelButFinal, x, y, z)));
            }
        }
    }
}
