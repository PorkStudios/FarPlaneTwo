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
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import java.nio.IntBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.daporkchop.fp2.util.Constants.T_VOXELS;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A "piece" containing the data used by the heightmap rendering mode.
 *
 * @author DaPorkchop_
 */
@Getter
public class HeightmapPiece extends HeightmapPos implements IFarPiece<HeightmapPos> {
    protected static final long TIMESTAMP_OFFSET = PUnsafe.pork_getOffset(HeightmapPiece.class, "timestamp");
    protected static final long DIRTY_OFFSET = PUnsafe.pork_getOffset(HeightmapPiece.class, "dirty");

    public static final int HEIGHT_SIZE = 4;
    public static final int HEIGHT_OFFSET = 0;

    public static final int BLOCK_SIZE = 4;
    public static final int BLOCK_OFFSET = HEIGHT_OFFSET + HEIGHT_SIZE;

    public static final int ATTRS_SIZE = 4;
    public static final int ATTRS_OFFSET = BLOCK_OFFSET + BLOCK_SIZE;

    public static final int PADDING_SIZE = 4;
    public static final int PADDING_OFFSET = ATTRS_OFFSET + ATTRS_SIZE;

    public static final int ENTRY_SIZE = PADDING_OFFSET + PADDING_SIZE;
    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS;

    public static final int TOTAL_SIZE = ENTRY_COUNT * ENTRY_SIZE;

    private static int biomeIndex(int x, int z) {
        x += 1;
        z += 1;
        checkArg(x >= 0 && x < T_VOXELS + 2 && z >= 0 && z < T_VOXELS + 2, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return x * (T_VOXELS + 2) + z;
    }

    private static int index(int x, int z) {
        checkArg(x >= 0 && x < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return (x * T_VOXELS + z) * 4;
    }

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected final IntBuffer data = Constants.createIntBuffer(ENTRY_COUNT * 4);

    protected volatile long timestamp = -1L;

    @Getter(AccessLevel.NONE)
    protected transient volatile int dirty = 0;

    public HeightmapPiece(int x, int z, int level) {
        super(x, z, level);
    }

    public HeightmapPiece(@NonNull ByteBuf buf) {
        super(buf);

        this.timestamp = buf.readLong();

        for (int i = 0; i < ENTRY_COUNT * 4; i++) {
            this.data.put(i, buf.readInt());
        }
    }

    @Override
    public void writePiece(@NonNull ByteBuf dst) {
        checkState(this.timestamp >= 0L, "piece does not contain any data!");
        this.writePos(dst);

        dst.writeLong(this.timestamp);

        for (int i = 0; i < ENTRY_COUNT * 4; i++) {
            dst.writeInt(this.data.get(i));
        }
    }

    @Override
    public void updateTimestamp(long timestamp) throws IllegalArgumentException {
        long current;
        do {
            current = PUnsafe.getLongVolatile(this, TIMESTAMP_OFFSET);
            checkArg(timestamp > current, "new timestamp (%d) must be greater than current timestamp (%d)!", timestamp, current);
        } while (!PUnsafe.compareAndSwapLong(this, TIMESTAMP_OFFSET, current, timestamp));
    }

    @Override
    public Lock readLock() {
        return this.lock.readLock();
    }

    @Override
    public Lock writeLock() {
        return this.lock.writeLock();
    }

    public int height(int x, int z) {
        return this.data.get(index(x, z) + 0);
    }

    public int block(int x, int z) {
        return this.data.get(index(x, z) + 1) & 0x00FFFFFF;
    }

    public int light(int x, int z) {
        return this.data.get(index(x, z) + 1) >>> 24;
    }

    public int biome(int x, int z) {
        return this.data.get(index(x, z) + 2) & 0xFF;
    }

    public int waterLight(int x, int z) {
        return (this.data.get(index(x, z) + 2) >>> 8) & 0xFF;
    }

    public int waterBiome(int x, int z) {
        return (this.data.get(index(x, z) + 2) >>> 16) & 0xFF;
    }

    public HeightmapPiece set(int x, int z, int height, IBlockState state, int light, Biome biome, int waterLight, Biome waterBiome) {
        return this.set(x, z, height, Block.getStateId(state), light, Biome.getIdForBiome(biome), waterLight, Biome.getIdForBiome(waterBiome));
    }

    public HeightmapPiece set(int x, int z, int height, int state, int light, int biome, int waterLight, int waterBiome) {
        int base = index(x, z);

        this.data.put(base + 0, height)
                .put(base + 1, (light << 24) | state)
                .put(base + 2, (waterBiome << 16) | (waterLight << 8) | biome)
                .put(base + 3, 0);
        this.markDirty();
        return this;
    }

    public void copy(int srcX, int srcZ, HeightmapPiece src, int x, int z)  {
        int srcBase = index(srcX, srcZ);
        int base = index(x, z);
        for (int i = 0; i < 4; i++) {
            this.data.put(base + i, src.data.get(srcBase + i));
        }
    }

    @Override
    public HeightmapPos pos() {
        return new HeightmapPos(this);
    }

    @Override
    public boolean isDirty() {
        return this.dirty != 0;
    }

    @Override
    public void markDirty() {
        this.dirty = 1;
    }

    @Override
    public boolean clearDirty() {
        return PUnsafe.compareAndSwapInt(this, DIRTY_OFFSET, 1, 0);
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("HeightmapPiece(x=%d, z=%d, level=%d, timestamp=%d, dirty=%b)", this.x, this.z, this.level, this.timestamp, this.isDirty());
    }
}
