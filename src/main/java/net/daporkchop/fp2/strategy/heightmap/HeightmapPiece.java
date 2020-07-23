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
import lombok.experimental.Accessors;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.Block;
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

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A "piece" containing the data used by the heightmap rendering strategy.
 *
 * @author DaPorkchop_
 */
@Getter
public class HeightmapPiece extends HeightmapPos implements IFarPiece, IBlockAccess {
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

    protected final ByteBuffer biome = Constants.createByteBuffer((HEIGHTMAP_VOXELS + 2) * (HEIGHTMAP_VOXELS + 2));
    protected final IntBuffer data = Constants.createIntBuffer(ENTRY_COUNT * 4);

    @Getter(AccessLevel.NONE)
    protected volatile int dirty = 0;

    public HeightmapPiece(int x, int z, int level) {
        super(x, z, level);
    }

    public HeightmapPiece(@NonNull ByteBuf buf) {
        super(buf);

        buf.readLong();
        buf.readLong(); //placeholder

        for (int i = 0; i < (HEIGHTMAP_VOXELS + 2) * (HEIGHTMAP_VOXELS + 2); i++) {
            this.biome.put(i, buf.readByte());
        }

        for (int i = 0; i < ENTRY_COUNT * 4; i++) {
            this.data.put(i, buf.readInt());
        }
    }

    @Override
    public void write(@NonNull ByteBuf dst) {
        this.writePos(dst);

        dst.writeLong(0L).writeLong(0L); //placeholder

        for (int i = 0; i < (HEIGHTMAP_VOXELS + 2) * (HEIGHTMAP_VOXELS + 2); i++) {
            dst.writeByte(this.biome.get(i));
        }

        for (int i = 0; i < ENTRY_COUNT * 4; i++) {
            dst.writeInt(this.data.get(i));
        }
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
        return this.biome.get(biomeIndex(x, z)) & 0xFF;
    }

    public HeightmapPiece set(int x, int z, int height, IBlockState state, int light) {
        return this.set(x, z, height, Block.getStateId(state), light);
    }

    public HeightmapPiece set(int x, int z, int height, int state, int light) {
        int base = index(x, z);

        this.data.put(base + 0, height)
                .put(base + 1, (light << 24) | state)
                .put(base + 2, 0)
                .put(base + 3, 0);
        this.markDirty();
        return this;
    }

    public HeightmapPiece setBiome(int x, int z, Biome biome) {
        return this.setBiome(x, z, Biome.getIdForBiome(biome));
    }

    public HeightmapPiece setBiome(int x, int z, int biome) {
        this.biome.put(biomeIndex(x, z), (byte) biome);
        this.markDirty();
        return this;
    }

    @Override
    public HeightmapPos pos() {
        return new HeightmapPos(this);
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
        int x = (pos.getX() - this.blockX()) >> this.level;
        int z = (pos.getZ() - this.blockZ()) >> this.level;
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
