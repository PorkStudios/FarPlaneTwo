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

package net.daporkchop.fp2.mode.voxel;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.common.AbstractFarPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.primitive.map.IntIntMap;
import net.daporkchop.lib.primitive.map.hash.HashMapHelper;
import net.daporkchop.lib.primitive.map.open.IntIntOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Stores server-side data for the voxel strategy.
 *
 * @author DaPorkchop_
 */
@Getter
public class VoxelPiece extends AbstractFarPiece<VoxelPos> {
    //layout (in ints):
    //0: (dx << 24) | (dy << 16) | (dz << 8) | edges
    //1: (biome << 8) | light
    //2: state

    public static final int ENTRY_SIZE = 3;

    public static final int MAX_ENTRY_COUNT = T_VOXELS * T_VOXELS * T_VOXELS;

    public static final int MAX_CAPACITY = MAX_ENTRY_COUNT * ENTRY_SIZE;
    public static final int DEFAULT_CAPACITY = MAX_CAPACITY / 32;

    private static int index(int x, int y, int z) {
        checkArg(x >= 0 && x < T_VOXELS && y >= 0 && y < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, y=%d, z=%d)", x, y, z);
        return (x * T_VOXELS + y) * T_VOXELS + z;
    }

    protected IntIntMap indices;
    protected IntBuffer data;
    protected int indexCounter;

    public VoxelPiece(@NonNull VoxelPos pos) {
        super(pos, RenderMode.VOXEL);
    }

    public VoxelPiece(@NonNull ByteBuf src) {
        super(src, RenderMode.VOXEL);

        int size = this.indexCounter = src.readInt();

        this.indices = new IntIntOpenHashMap(ceilI(size * (1.0d / HashMapHelper.DEFAULT_LOAD_FACTOR)));
        for (int i = 0; i < size; i++) { //indices
            this.indices.put(src.readUnsignedShort(), src.readUnsignedShort());
        }

        int capacity = DEFAULT_CAPACITY;
        while (capacity < size * ENTRY_SIZE) {
            capacity <<= 1;
        }
        this.data = Constants.createIntBuffer(capacity);

        for (int i = 0; i < size * ENTRY_SIZE; i++) { //data
            this.data.put(i, src.readInt());
        }
    }

    @Override
    protected void writeBody(@NonNull ByteBuf dst) {
        int size = this.indexCounter;
        dst.writeInt(size);

        if (size > 0) {
            this.indices.forEach((i, j) -> dst.writeShort(i).writeShort(j));
            for (int i = 0; i < size * ENTRY_SIZE; i++) {
                dst.writeInt(this.data.get(i));
            }
        }
    }

    @Override
    public boolean isBlank() {
        return this.indices == null;
    }

    @Override
    public void clear() {
        this.indexCounter = 0;
        this.indices = new IntIntOpenHashMap();

        if (this.data != null) {
            PUnsafe.pork_releaseBuffer(this.data);
        }
        this.data = Constants.createIntBuffer(DEFAULT_CAPACITY);
    }

    @Override
    public void postGenerate() {
        if (this.indexCounter == 0) { //there is no data
            this.indices = null;

            if (this.data != null) {
                PUnsafe.pork_releaseBuffer(this.data);
            }
            this.data = null;
        } else if (this.indexCounter * ENTRY_SIZE < this.data.capacity()) { //remove padding at end of buffer
            IntBuffer oldData = this.data;

            IntBuffer newData = this.data = Constants.createIntBuffer(oldData.capacity() << 1);

            oldData.clear(); //copy old data to new buffer
            newData.put(oldData);

            PUnsafe.pork_releaseBuffer(oldData);
        }
    }

    public int getIndex(int x, int y, int z) {
        return this.indices != null ? this.indices.getOrDefault(index(x, y, z), -1) : -1;
    }

    public boolean get(int x, int y, int z, VoxelData data) {
        int index = this.getIndex(x, y, z);
        if (index < 0) { //there is no value at the given position
            data.reset();
            return false;
        }

        this.get(index, data);
        return true;
    }

    public void get(int index, VoxelData data) {
        checkArg(index >= 0 && index < this.indexCounter, index);

        index *= ENTRY_SIZE;
        int i0 = this.data.get(index);
        int i1 = this.data.get(index + 1);
        int i2 = this.data.get(index + 2);

        data.x = (i0 >>> 24) / 255.0d;
        data.y = ((i0 >> 16) & 0xFF) / 255.0d;
        data.z = ((i0 >> 8) & 0xFF) / 255.0d;
        data.edges = i0 & 0xFF;

        data.state = i2;
        data.biome = i1 >>> 8;
        data.light = i1 & 0xFF;
    }

    public VoxelPiece set(int x, int y, int z, VoxelData data) {
        int index = this.indices.getOrDefault(index(x, y, z), -1);
        if (index < 0) { //allocate new index
            index = this.nextIndex(x, y, z);
        }

        index *= ENTRY_SIZE;

        int dx = floorI(clamp(data.x, 0., 1.) * 255.0d);
        int dy = floorI(clamp(data.y, 0., 1.) * 255.0d);
        int dz = floorI(clamp(data.z, 0., 1.) * 255.0d);

        int edges = data.edges;
        edges = ((edges & 0x800) >> 9) | ((edges & 0x80) >> 6) | ((edges & 0x8) >> 3);

        this.data.put(index, (dx << 24) | (dy << 16) | (dz << 8) | edges);
        this.data.put(index + 1, (data.biome << 8) | data.light);
        this.data.put(index + 2, data.state);
        return this;
    }

    protected int nextIndex(int x, int y, int z) {
        int index = this.indexCounter++;
        if (index * ENTRY_SIZE == this.data.capacity()) { //grow data buffer
            IntBuffer oldData = this.data;
            IntBuffer newData = this.data = Constants.createIntBuffer(oldData.capacity() << 1);

            oldData.clear(); //copy old data to new buffer
            newData.put(oldData);

            PUnsafe.pork_releaseBuffer(oldData);
        }
        this.indices.put(index(x, y, z), index);
        return index;
    }
}
