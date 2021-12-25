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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.cube;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.IBiomeAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Map;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.*;

/**
 * Alternative implementation of {@link ICube} which doesn't belong to a {@link World}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class CubeWithoutWorld implements ICube {
    @NonNull
    protected final ExtendedBlockStorage storage;
    @NonNull
    protected final IBiomeAccess biomes;
    @NonNull
    protected final CubePos pos;

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    @Override
    public IBlockState setBlockState(BlockPos pos, IBlockState newstate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBlockState getBlockState(int blockX, int localOrBlockY, int blockZ) {
        return this.storage.get(blockX & 0xF, localOrBlockY & 0xF, blockZ & 0xF);
    }

    @Override
    public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        return lightType == EnumSkyBlock.SKY
                ? this.storage.getSkyLight(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF)
                : this.storage.getBlockLight(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
    }

    @Override
    public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType createType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTileEntity(TileEntity tileEntity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return this.storage.isEmpty();
    }

    @Override
    public BlockPos localAddressToBlockPos(int localAddress) {
        int x = localToBlock(this.pos.getX(), AddressTools.getLocalX(localAddress));
        int y = localToBlock(this.pos.getY(), AddressTools.getLocalY(localAddress));
        int z = localToBlock(this.pos.getZ(), AddressTools.getLocalZ(localAddress));
        return new BlockPos(x, y, z);
    }

    @Override
    public <T extends World & ICubicWorld> T getWorld() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Chunk & IColumn> T getColumn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getX() {
        return this.pos.getX();
    }

    @Override
    public int getY() {
        return this.pos.getY();
    }

    @Override
    public int getZ() {
        return this.pos.getZ();
    }

    @Override
    public CubePos getCoords() {
        return this.pos;
    }

    @Override
    public boolean containsBlockPos(BlockPos blockPos) {
        return this.pos.getX() == blockToCube(blockPos.getX())
               && this.pos.getY() == blockToCube(blockPos.getY())
               && this.pos.getZ() == blockToCube(blockPos.getZ());
    }

    @Nullable
    @Override
    public ExtendedBlockStorage getStorage() {
        return this.storage;
    }

    @Override
    public Map<BlockPos, TileEntity> getTileEntityMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassInheritanceMultiMap<Entity> getEntitySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEntity(Entity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeEntity(Entity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needsSaving() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPopulated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFullyPopulated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSurfaceTracked() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInitialLightingDone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCubeLoaded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasLightUpdates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.biomes.getBiome(pos);
    }

    @Override
    public void setBiome(int localBiomeX, int localBiomeZ, Biome biome) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public CapabilityDispatcher getCapabilities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnumSet<ForcedLoadReason> getForceLoadStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        throw new UnsupportedOperationException();
    }
}
