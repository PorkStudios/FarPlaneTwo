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
 */

package net.daporkchop.fp2.core.mode.heightmap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A "tile" containing the data used by the heightmap rendering mode.
 *
 * @author DaPorkchop_
 */
@Getter
public class HeightmapTile implements IFarTile {
    //index layout (in ints):
    //0: layer_flags
    //   ^ top 28 bits are free

    //layer layout (in ints):
    //0: height_int
    //1: (state << 8) | height_frac
    //2: (secondary_connection << 16) | (light << 8) | biome
    //   ^ top 8 bits are free

    /*
     * struct Entry {
     *   index _index;
     *   layer _layers[MAX_LAYERS];
     * };
     *
     * struct HeightmapTile {
     *   Entry _entries[ENTRY_COUNT];
     * };
     */

    public static final int ENTRY_COUNT = HT_VOXELS * HT_VOXELS;

    public static final int INDEX_SIZE = 1;
    public static final int LAYER_SIZE = 3;
    public static final int ENTRY_SIZE = INDEX_SIZE + LAYER_SIZE * MAX_LAYERS;
    public static final int TILE_SIZE = ENTRY_SIZE * ENTRY_COUNT;

    public static final int INDEX_SIZE_BYTES = INDEX_SIZE * INT_SIZE;
    public static final int LAYER_SIZE_BYTES = LAYER_SIZE * INT_SIZE;
    public static final int ENTRY_SIZE_BYTES = INDEX_SIZE_BYTES + LAYER_SIZE_BYTES * MAX_LAYERS;
    public static final int TILE_SIZE_BYTES = ENTRY_SIZE_BYTES * ENTRY_COUNT;

    public static final IVariableSizeRecyclingCodec<HeightmapTile> CODEC = new IVariableSizeRecyclingCodec<HeightmapTile>() {
        @Override
        public long maxSize() {
            return TILE_SIZE_BYTES;
        }

        @Override
        public void load(@NonNull HeightmapTile tile, @NonNull DataIn in) throws IOException {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                //copy everything in one go
                in.readFully(Unpooled.wrappedBuffer(tile.addr, TILE_SIZE_BYTES, false).writerIndex(0));
            } else {
                //read individual ints (reversing the byte order each time)
                for (long addr = tile.addr, end = addr + TILE_SIZE_BYTES; addr != end; addr += INT_SIZE) {
                    PUnsafe.putInt(addr, in.readIntLE());
                }
            }
        }

        @Override
        public void store(@NonNull HeightmapTile tile, @NonNull DataOut out) throws IOException {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                //copy everything in one go
                out.write(Unpooled.wrappedBuffer(tile.addr, TILE_SIZE_BYTES, false));
            } else {
                //write individual ints (reversing the byte order each time)
                for (long addr = tile.addr, end = addr + TILE_SIZE_BYTES; addr != end; addr += INT_SIZE) {
                    out.writeIntLE(PUnsafe.getInt(addr));
                }
            }
        }
    };

    public static int layerFlag(int layer) {
        checkArg(layer >= 0 && layer < MAX_LAYERS, "layer index out of bounds (%d)", layer);
        return 1 << layer;
    }

    static int entryOffset(int x, int z) {
        assert x >= 0 && x < HT_VOXELS : "x=" + x;
        assert z >= 0 && z < HT_VOXELS : "z=" + z;

        return (x * HT_VOXELS + z) * ENTRY_SIZE_BYTES;
    }

    static int layerOffset(int x, int z, int layer) {
        return entryOffset(x, z) + INDEX_SIZE_BYTES + layer * LAYER_SIZE_BYTES;
    }

    static void writeLayer(long base, HeightmapData data) {
        int height_int = data.height_int;
        int height_frac = data.height_frac;

        /*
         * if (height_frac < 0) {
         *     height_int -= 1;
         *     height_frac += 256;
         * }
         */
        int mask = height_frac >> 31;
        height_int += mask;
        height_frac += (mask & 256);

        PUnsafe.putInt(base + 0L, height_int);
        PUnsafe.putInt(base + 4L, (data.state << 8) | height_frac);
        PUnsafe.putInt(base + 8L, (data.secondaryConnection << 16) | (data.light << 8) | data.biome);
    }

    static void readLayer(long base, HeightmapData data) {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);
        int i2 = PUnsafe.getInt(base + 8L);

        data.height_int = i0;
        data.height_frac = i1 & 0xFF;
        data.state = i1 >>> 8;
        data.light = (i2 >>> 8) & 0xFF;
        data.biome = i2 & 0xFF;
        data.secondaryConnection = i2 >>> 16;
    }

    static double readLayerOnlyHeight(long base) {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);

        return i0 + ((i1 & 0xFF) * (1.0d / 256.0d));
    }

    protected final long addr = PUnsafe.allocateMemory(TILE_SIZE_BYTES);

    public HeightmapTile() {
        this.reset();

        PCleaner.cleaner(this, this.addr);
    }

    public boolean getLayer(int x, int z, int layer, @NonNull HeightmapData data) {
        if ((PUnsafe.getInt(this.addr + entryOffset(x, z)) & layerFlag(layer)) != 0) {
            //the layer is set, read it
            this._getLayerUnchecked(x, z, layer, data);
            return true;
        } else {
            //the layer is unset, don't read it
            return false;
        }
    }

    public double getLayerOnlyHeight(int x, int z, int layer) {
        if ((PUnsafe.getInt(this.addr + entryOffset(x, z)) & layerFlag(layer)) != 0) {
            //the layer is set, read it
            return readLayerOnlyHeight(this.addr + layerOffset(x, z, layer));
        } else {
            //the layer is unset, don't read it
            return Double.NaN;
        }
    }

    public int _getLayerFlags(int x, int z) {
        return PUnsafe.getInt(this.addr + entryOffset(x, z));
    }

    public void _getLayerUnchecked(int x, int z, int layer, @NonNull HeightmapData data) {
        readLayer(this.addr + layerOffset(x, z, layer), data);
    }

    public void setLayer(int x, int z, int layer, @NonNull HeightmapData data) {
        //set layer flag
        long entry = this.addr + entryOffset(x, z);
        PUnsafe.putInt(entry, PUnsafe.getInt(entry) | layerFlag(layer));

        //actually write data
        writeLayer(this.addr + layerOffset(x, z, layer), data);
    }

    public void unsetLayer(int x, int z, int layer) {
        long entry = this.addr + entryOffset(x, z);
        int layer_flags = PUnsafe.getInt(entry);
        int flag = layerFlag(layer);

        if ((layer_flags & flag) != 0) { //the layer is set
            //clear the flag for this layer
            PUnsafe.putInt(entry, layer_flags & ~flag);

            //clear the layer data
            PUnsafe.setMemory(this.addr + layerOffset(x, z, layer), LAYER_SIZE_BYTES, (byte) 0);
        }
    }

    @Override
    public boolean isEmpty() {
        return false; //heightmap tiles are never empty
    }

    @Override
    public void reset() {
        PUnsafe.setMemory(this.addr, TILE_SIZE_BYTES, (byte) 0); //just clear it
    }

    @Override
    public void read(@NonNull ByteBuf src) {
        if (PlatformInfo.IS_LITTLE_ENDIAN) {
            //copy everything in one go
            src.readBytes(Unpooled.wrappedBuffer(this.addr, TILE_SIZE_BYTES, false).writerIndex(0));
        } else {
            //read individual ints (reversing the byte order each time)
            for (long addr = this.addr, end = addr + TILE_SIZE_BYTES; addr != end; addr += INT_SIZE) {
                PUnsafe.putInt(addr, src.readIntLE());
            }
        }
    }

    @Override
    public boolean write(@NonNull ByteBuf dst) {
        if (PlatformInfo.IS_LITTLE_ENDIAN) {
            //copy everything in one go
            dst.writeBytes(Unpooled.wrappedBuffer(this.addr, TILE_SIZE_BYTES, false));
        } else {
            //write individual ints (reversing the byte order each time)
            dst.ensureWritable(TILE_SIZE_BYTES);
            for (long addr = this.addr, end = addr + TILE_SIZE_BYTES; addr != end; addr += INT_SIZE) {
                dst.writeIntLE(PUnsafe.getInt(addr));
            }
        }
        return false; //the heightmap renderer has no concept of an "empty" tile
    }

    @Override
    public long extra() {
        return 0L; //heightmap renderer doesn't use the extra data field
    }
}
