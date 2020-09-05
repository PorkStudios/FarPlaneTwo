/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.voxel;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.base.AbstractFarPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.primitive.map.IntIntMap;
import net.daporkchop.lib.primitive.map.open.IntIntOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class VoxelPiece extends AbstractFarPiece<VoxelPos> {
    public static final int ENTRY_SIZE = 1;

    public static final int MAX_ENTRY_COUNT = T_VOXELS * T_VOXELS * T_VOXELS;

    public static final int MAX_CAPACITY = MAX_ENTRY_COUNT * ENTRY_SIZE;
    public static final int DEFAULT_CAPACITY = MAX_CAPACITY / 32;

    private static int index(int x, int y, int z) {
        checkArg(x >= 0 && x < T_VOXELS && y >= 0 && y < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, y=%d, z=%d)", x, y, z);
        return (x * T_VOXELS + y) * T_VOXELS + z;
    }

    protected final IntIntMap indices = new IntIntOpenHashMap();
    protected IntBuffer data;

    public VoxelPiece(@NonNull VoxelPos pos) {
        super(pos, RenderMode.VOXEL);

        this.data = Constants.createIntBuffer(DEFAULT_CAPACITY);
    }

    public VoxelPiece(@NonNull ByteBuf src) {
        super(src, RenderMode.VOXEL);

        int size = src.readInt();
        for (int i = 0; i < size; i++)  { //indices
            this.indices.put(src.readUnsignedShort(), src.readUnsignedShort());
        }

        int capacity = DEFAULT_CAPACITY;
        while (capacity < size * ENTRY_SIZE)    {
            capacity <<= 1;
        }
        this.data = Constants.createIntBuffer(capacity);

        for (int i = 0; i < size * ENTRY_SIZE; i++) { //data
            this.data.put(i, src.readInt());
        }
    }

    @Override
    protected void writeBody(@NonNull ByteBuf dst) {
        int size = this.indices.size();
        dst.writeInt(size);
        this.indices.forEach((i, j) -> dst.writeShort(i).writeShort(j));
        for (int i = 0; i < size * ENTRY_SIZE; i++) {
            dst.writeInt(this.data.get(i));
        }
    }

    public int getIndex(int x, int y, int z)    {
        return this.indices.getOrDefault(index(x, y, z), -1);
    }

    public boolean get(int x, int y, int z, VoxelData data) {
        int index = this.getIndex(x, y, z);
        if (index < 0) { //there is no value at the given position
            return false;
        }

        index *= ENTRY_SIZE;
        int i = this.data.get(index);

        data.dx = (i >>> 24) / 255.0d;
        data.dy = ((i >> 16) & 0xFF) / 255.0d;
        data.dz = ((i >> 8) & 0xFF) / 255.0d;
        data.edges = i & 0xFF;
        return true;
    }

    public VoxelPiece set(int x, int y, int z, double dx, double dy, double dz, int edgeMask) {
        int index = this.indices.getOrDefault(index(x, y, z), -1);
        if (index < 0)  { //allocate new index
            index = this.nextIndex(x, y, z);
        }

        edgeMask = ((edgeMask & 0x800) >> 9) | ((edgeMask & 0x80) >> 6) | ((edgeMask & 0x8) >> 3);

        dx = clamp(dx, 0., 1.) * 255.0d;
        dy = clamp(dy, 0., 1.) * 255.0d;
        dz = clamp(dz, 0., 1.) * 255.0d;

        this.data.put(index, (floorI(dx) << 24) | (floorI(dy) << 16) | (floorI(dz) << 8) | edgeMask);
        this.markDirty();
        return this;
    }

    protected int nextIndex(int x, int y, int z)   {
        int size = this.indices.size();
        if (size * ENTRY_SIZE == this.data.capacity())   {
            IntBuffer newData = Constants.createIntBuffer(this.data.capacity() << 1);
            newData.put(this.data);
            PUnsafe.pork_releaseBuffer(this.data);
            this.data = newData;
        }
        this.indices.put(index(x, y, z), size);
        return size;
    }

    public void clear() {
        this.indices.clear();
        if (this.data.capacity() >= DEFAULT_CAPACITY)   {
            PUnsafe.pork_releaseBuffer(this.data);
            this.data = Constants.createIntBuffer(DEFAULT_CAPACITY);
        }
    }
}
