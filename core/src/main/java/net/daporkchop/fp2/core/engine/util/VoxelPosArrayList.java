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
import net.daporkchop.fp2.core.mode.common.util.AbstractPosArrayList;
import net.daporkchop.fp2.core.engine.VoxelPos;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link List} optimized specifically for {@link VoxelPos}.
 *
 * @author DaPorkchop_
 */
public class VoxelPosArrayList extends AbstractPosArrayList<VoxelPos> {
    public VoxelPosArrayList() {
        super(4);
    }

    public VoxelPosArrayList(int initialCapacity) {
        super(4, initialCapacity);
    }

    public VoxelPosArrayList(@NonNull Collection<? extends VoxelPos> src) {
        super(4, src);
    }

    @Override
    protected VoxelPos readPos(int[] srcArray, int srcBaseArray) {
        return new VoxelPos(srcArray[srcBaseArray++], srcArray[srcBaseArray++], srcArray[srcBaseArray++], srcArray[srcBaseArray]);
    }

    @Override
    protected void writePos(VoxelPos pos, int[] dstArray, int dstBaseIndex) {
        dstArray[dstBaseIndex++] = pos.level();
        dstArray[dstBaseIndex++] = pos.x();
        dstArray[dstBaseIndex++] = pos.y();
        dstArray[dstBaseIndex] = pos.z();
    }
}
