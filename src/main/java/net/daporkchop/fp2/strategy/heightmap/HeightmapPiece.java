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

package net.daporkchop.fp2.strategy.heightmap;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.strategy.RenderStrategy;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A "piece" containing the data used by the heightmap rendering strategy.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class HeightmapPiece implements IFarPiece {
    protected static final long DIRTY_OFFSET = PUnsafe.pork_getOffset(HeightmapPiece.class, "dirty");

    public static final int HEIGHT_SIZE = 4;
    public static final int HEIGHT_OFFSET = 0;

    public static final int BLOCK_SIZE = 4;
    public static final int BLOCK_OFFSET = HEIGHT_OFFSET + HEIGHT_SIZE;

    public static final int ATTRS_SIZE = 4;
    public static final int ATTRS_OFFSET = BLOCK_OFFSET + BLOCK_SIZE;

    public static final int ENTRY_SIZE = ATTRS_OFFSET + ATTRS_SIZE;
    public static final int ENTRY_COUNT = HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS;

    private static int base(int x, int z) {
        checkArg(x >= 0 && x < HEIGHTMAP_VOXELS && z >= 0 && z < HEIGHTMAP_VOXELS, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return (x * HEIGHTMAP_VOXELS + z) * ENTRY_SIZE;
    }

    protected final int x;
    protected final int z;

    @Getter(AccessLevel.NONE)
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected final ByteBuffer data = Constants.createByteBuffer(ENTRY_COUNT * ENTRY_SIZE);

    @Getter(AccessLevel.NONE)
    protected volatile int dirty = 0;

    public HeightmapPiece(@NonNull ByteBuf buf) {
        this(buf.readInt(), buf.readInt());
        for (int i = 0; i < ENTRY_COUNT; i++) {
            this.data.putInt(i * ENTRY_SIZE + HEIGHT_OFFSET, buf.readInt())
                    .putInt(i * ENTRY_SIZE + BLOCK_OFFSET, buf.readInt())
                    .putInt(i * ENTRY_SIZE + ATTRS_OFFSET, buf.readInt());
        }
    }

    @Override
    public void write(@NonNull ByteBuf buf) {
        buf.ensureWritable(8 + ENTRY_SIZE * ENTRY_COUNT);
        buf.writeInt(this.x).writeInt(this.z);
        for (int i = 0; i < ENTRY_COUNT; i++) {
            buf.writeInt(this.data.getInt(i * ENTRY_SIZE + HEIGHT_OFFSET))
                    .writeInt(this.data.getInt(i * ENTRY_SIZE + BLOCK_OFFSET))
                    .writeInt(this.data.getInt(i * ENTRY_SIZE + ATTRS_OFFSET));
        }
    }

    @Override
    public RenderStrategy strategy() {
        return RenderStrategy.HEIGHTMAP;
    }

    public int height(int x, int z) {
        return this.data.getInt(base(x, z) + HEIGHT_OFFSET);
    }

    public int block(int x, int z) {
        return this.data.getInt(base(x, z) + BLOCK_OFFSET);
    }

    public int attrs(int x, int z) {
        return this.data.get(base(x, z) + ATTRS_OFFSET) & 0xFF;
    }

    public HeightmapPiece set(int x, int z, int height, IBlockState state, Biome biome, int combinedLight) {
        int base = base(x, z);

        int attrs = Biome.getIdForBiome(biome) | (packCombinedLight(combinedLight) << 6);
        MapColor color = state.getMaterial().getMaterialMapColor();
        if (color == MapColor.GRASS) {
            attrs |= 1 << (6 + 8 + 0);
        } else if (color == MapColor.FOLIAGE) {
            attrs |= 1 << (6 + 8 + 1);
        } else if (color == MapColor.WATER) {
            attrs |= 1 << (6 + 8 + 2);
        }

        this.data.putInt(base + HEIGHT_OFFSET, height)
                .putInt(base + BLOCK_OFFSET, Block.getStateId(state))
                .putInt(base + ATTRS_OFFSET, attrs);
        this.markDirty();
        return this;
    }

    @Override
    public HeightmapPiecePos pos() {
        return new HeightmapPiecePos(this.x, this.z);
    }

    @Override
    public Lock readLock() {
        return this.lock.readLock();
    }

    @Override
    public Lock writeLock() {
        return this.lock.writeLock();
    }

    public boolean isDirty() {
        return this.dirty != 0;
    }

    public void markDirty() {
        this.dirty = 1;
    }

    public boolean clearDirty() {
        return PUnsafe.compareAndSwapInt(this, DIRTY_OFFSET, 1, 0);
    }
}
