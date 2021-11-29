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

package net.daporkchop.fp2.mode.voxel.util;

import lombok.NonNull;
import net.daporkchop.fp2.mode.common.util.AbstractPosSet;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.core.util.datastructure.SimpleSet;

import java.util.function.Consumer;

import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;

/**
 * Implementation of {@link SimpleSet} optimized specifically for {@link VoxelPos}.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public class VoxelPosSet extends AbstractPosSet<VoxelPos> {
    public VoxelPosSet() {
        super(3, V_MAX_LODS);
    }

    @Override
    public boolean add(@NonNull VoxelPos pos) {
        return this.delegates[pos.level()].add(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean remove(@NonNull VoxelPos pos) {
        return this.delegates[pos.level()].remove(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean contains(@NonNull VoxelPos pos) {
        return this.delegates[pos.level()].contains(pos.x(), pos.y(), pos.z());
    }

    @Override
    public void forEach(@NonNull Consumer<? super VoxelPos> callback) {
        NDimensionalIntSet[] delegates = this.delegates;

        for (int level = 0; level < delegates.length; level++) {
            int levelButFinal = level; //damn you java
            delegates[level].forEach3D((x, y, z) -> callback.accept(new VoxelPos(levelButFinal, x, y, z)));
        }
    }
}
