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
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
public class HeightmapPiece implements IFarPiece, IBlockAccess {
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
    public static final int ENTRY_COUNT = HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS;

    public static final int TOTAL_SIZE = ENTRY_COUNT * ENTRY_SIZE;

    private static int biomeIndex(int x, int z) {
        x += 1;
        z += 1;
        checkArg(x >= 0 && x < HEIGHTMAP_VOXELS + 2 && z >= 0 && z < HEIGHTMAP_VOXELS + 2, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return x * (HEIGHTMAP_VOXELS + 2) + z;
    }

    private static int index(int x, int z) {
        checkArg(x >= 0 && x < HEIGHTMAP_VOXELS && z >= 0 && z < HEIGHTMAP_VOXELS, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return (x * HEIGHTMAP_VOXELS + z) * 4;
    }

    protected final int x;
    protected final int z;

    @Getter(AccessLevel.NONE)
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected final ByteBuffer biome = Constants.createByteBuffer((HEIGHTMAP_VOXELS + 2) * (HEIGHTMAP_VOXELS + 2));
    protected final IntBuffer data = Constants.createIntBuffer(ENTRY_COUNT * 4);

    @Getter(AccessLevel.NONE)
    protected volatile int dirty = 0;

    public HeightmapPiece(@NonNull ByteBuf buf) {
        this(buf.readInt(), buf.readInt());

        buf.readBytes((ByteBuffer) this.biome.clear());

        for (int i = 0; i < ENTRY_COUNT * 4; i++) {
            this.data.put(i, buf.readInt());
        }
    }

    @Override
    public void write(@NonNull ByteBuf buf) {
        buf.writeInt(this.x).writeInt(this.z)
        .writeBytes((ByteBuffer) this.biome.clear());

        for (int i = 0; i < ENTRY_COUNT * 4; i++) {
            buf.writeInt(this.data.get(i));
        }
    }

    @Override
    public RenderStrategy strategy() {
        return RenderStrategy.HEIGHTMAP;
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

    public int height2(int x, int z) {
        return this.data.get(index(x, z) + 2);
    }

    public int block2(int x, int z) {
        return this.data.get(index(x, z) + 3) & 0x00FFFFFF;
    }

    public int light2(int x, int z) {
        return this.data.get(index(x, z) + 3) >>> 24;
    }

    public HeightmapPiece set(int x, int z, int height, IBlockState state, int light) {
        int base = index(x, z);

        this.data.put(base + 0, height)
                .put(base + 1, (packCombinedLight(light) << 24) | Block.getStateId(state))
                .put(base + 2, 0)
                .put(base + 3, 0);
        this.markDirty();
        return this;
    }

    public HeightmapPiece set(int x, int z, int height, IBlockState state, int light, int height2, IBlockState state2, int light2) {
        int base = index(x, z);

        this.data.put(base + 0, height)
                .put(base + 1, (packCombinedLight(light) << 24) | Block.getStateId(state))
                .put(base + 2, height2)
                .put(base + 3, (packCombinedLight(light2) << 24) | Block.getStateId(state2));
        this.markDirty();
        return this;
    }

    public HeightmapPiece setBiome(int x, int z, Biome biome) {
        this.biome.put(biomeIndex(x, z), (byte) Biome.getIdForBiome(biome));
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

    //IBlockAccess implementations

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        int x = pos.getX() - this.x * HEIGHTMAP_VOXELS;
        int z = pos.getZ() - this.z * HEIGHTMAP_VOXELS;
        return Biome.getBiome(this.biome.get(biomeIndex(x, z)) & 0xFF, Biomes.PLAINS);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorldType getWorldType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        throw new UnsupportedOperationException();
    }
}
