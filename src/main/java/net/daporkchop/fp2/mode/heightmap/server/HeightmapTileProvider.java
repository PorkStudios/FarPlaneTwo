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

package net.daporkchop.fp2.mode.heightmap.server;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.mode.common.server.AbstractFarTileProvider;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.mode.heightmap.server.scale.HeightmapScalerMinMax;
import net.daporkchop.fp2.mode.heightmap.server.tracking.HeightmapTrackerManager;

/**
 * @author DaPorkchop_
 */
public abstract class HeightmapTileProvider extends AbstractFarTileProvider<HeightmapPos, HeightmapTile> {
    public HeightmapTileProvider(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
        super(world, mode);
    }

    @Override
    protected IFarScaler<HeightmapPos, HeightmapTile> createScaler() {
        return new HeightmapScalerMinMax();
    }

    @Override
    protected IFarTrackerManager<HeightmapPos, HeightmapTile> createTracker() {
        return new HeightmapTrackerManager(this);
    }

    @Override
    protected boolean anyVanillaTerrainExistsAt(@NonNull HeightmapPos pos) {
        int x = pos.x();
        int z = pos.z();
        int level = pos.level();
        return this.world().fp2_IFarWorldServer_fblockWorld().containsAnyData(x << level, Integer.MIN_VALUE, z << level, (x + 1) << level, Integer.MAX_VALUE, (z + 1) << level);
    }

    @Override
    protected void onColumnSaved(ColumnSavedEvent event) {
        this.scheduleForUpdate(new HeightmapPos(0, event.pos().x(), event.pos().y()));
    }

    public static class Vanilla extends HeightmapTileProvider {
        public Vanilla(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
            super(world, mode);
        }

        @Override
        protected void onCubeSaved(CubeSavedEvent event) {
            throw new UnsupportedOperationException();
        }
    }

    public static class CubicChunks extends HeightmapTileProvider {
        public CubicChunks(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
            super(world, mode);
        }

        @Override
        protected void onCubeSaved(CubeSavedEvent event) {
            if (event.cube().isFullyPopulated()) {
                this.scheduleForUpdate(new HeightmapPos(0, event.pos().x(), event.pos().z()));
            }
        }
    }
}
