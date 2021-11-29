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

package net.daporkchop.fp2.mode.voxel.server;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.mode.common.server.AbstractFarTileProvider;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.mode.voxel.server.scale.VoxelScalerIntersection;
import net.daporkchop.fp2.mode.voxel.server.tracking.VoxelTrackerManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * @author DaPorkchop_
 */
public abstract class VoxelTileProvider extends AbstractFarTileProvider<VoxelPos, VoxelTile> {
    public VoxelTileProvider(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<VoxelPos, VoxelTile> mode) {
        super(world, mode);
    }

    @Override
    protected IFarScaler<VoxelPos, VoxelTile> createScaler() {
        return new VoxelScalerIntersection();
    }

    @Override
    protected IFarTrackerManager<VoxelPos, VoxelTile> createTracker() {
        return new VoxelTrackerManager(this);
    }

    @Override
    protected boolean anyVanillaTerrainExistsAt(@NonNull VoxelPos pos) {
        int x = pos.x();
        int y = pos.y();
        int z = pos.z();
        int level = pos.level();
        return this.world().fp2_IFarWorldServer_fblockWorld().containsAnyData(x << level, y << level, z << level, (x + 1) << level, (y + 1) << level, (z + 1) << level);
    }

    /**
     * @author DaPorkchop_
     */
    public static class Vanilla extends VoxelTileProvider {
        public Vanilla(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<VoxelPos, VoxelTile> mode) {
            super(world, mode);
        }

        @Override
        public void onColumnSaved(@NonNull World world, int columnX, int columnZ, @NonNull NBTTagCompound nbt, @NonNull Chunk column) {
            if (column.isPopulated()) { //TODO: we want to check if the chunk is FULLY populated
                //schedule entire column to be updated
                IntAxisAlignedBB coordLimits = this.world.fp2_IFarWorld_coordLimits()[0];
                VoxelPos[] positions = new VoxelPos[coordLimits.maxX() + 1 - coordLimits.minY()];
                for (int i = 0, y = coordLimits.minY(); y <= coordLimits.maxY(); i++, y++) {
                    positions[i] = new VoxelPos(0, columnX, y, columnZ);
                }
                this.scheduleForUpdate(positions);
            }
        }

        @Override
        public void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt, @NonNull ICube cube) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class CubicChunks extends VoxelTileProvider {
        public CubicChunks(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<VoxelPos, VoxelTile> mode) {
            super(world, mode);
        }

        @Override
        public void onColumnSaved(@NonNull World world, int columnX, int columnZ, @NonNull NBTTagCompound nbt, @NonNull Chunk column) {
            //no-op
        }

        @Override
        public void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt, @NonNull ICube cube) {
            if (cube.isFullyPopulated()) {
                this.scheduleForUpdate(new VoxelPos(0, cubeX, cubeY, cubeZ));
            }
        }
    }
}
