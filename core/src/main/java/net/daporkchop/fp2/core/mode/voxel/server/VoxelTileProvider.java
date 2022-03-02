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
 *
 */

package net.daporkchop.fp2.core.mode.voxel.server;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.mode.common.server.AbstractFarTileProvider;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.server.tracking.VoxelTrackerManager;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockWorldHolder;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;

/**
 * @author DaPorkchop_
 */
public abstract class VoxelTileProvider extends AbstractFarTileProvider<VoxelPos, VoxelTile> {
    public VoxelTileProvider(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<VoxelPos, VoxelTile> mode) {
        super(world, mode);
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
        try (FBlockWorld world = this.world().fp2_IFarWorldServer_exactBlockWorldHolder().worldFor(ExactFBlockWorldHolder.AllowGenerationRequirement.DONT_CARE)) {
            return world.containsAnyData(x << level, y << level, z << level, (x + 1) << level, (y + 1) << level, (z + 1) << level);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Vanilla extends VoxelTileProvider {
        public Vanilla(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<VoxelPos, VoxelTile> mode) {
            super(world, mode);
        }

        @Override
        protected void onColumnSaved(ColumnSavedEvent event) {
            if (event.column().isFullyPopulated()) {
                //schedule entire column to be updated
                int minY = this.coordLimits.min(0).y();
                int maxY = this.coordLimits.max(0).y();

                VoxelPos[] positions = new VoxelPos[maxY + 1 - minY];
                for (int i = 0, y = minY; y <= maxY; i++, y++) {
                    positions[i] = new VoxelPos(0, event.pos().x(), y, event.pos().y());
                }
                this.scheduleForUpdate(positions);
            }
        }

        @Override
        protected void onCubeSaved(CubeSavedEvent event) {
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
        protected void onColumnSaved(ColumnSavedEvent event) {
            //no-op
        }

        @Override
        protected void onCubeSaved(CubeSavedEvent event) {
            if (event.cube().isFullyPopulated()) {
                this.scheduleForUpdate(new VoxelPos(0, event.pos().x(), event.pos().y(), event.pos().z()));
            }
        }
    }
}
