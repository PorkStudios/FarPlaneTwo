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
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.server.scale.HeightmapScalerMinMax;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * @author DaPorkchop_
 */
public abstract class HeightmapWorld extends AbstractFarWorld<HeightmapPos, HeightmapTile> {
    public HeightmapWorld(@NonNull WorldServer world, @NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
        super(world, mode);
    }

    @Override
    protected IFarScaler<HeightmapPos, HeightmapTile> createScaler() {
        return new HeightmapScalerMinMax();
    }

    @Override
    protected IFarPlayerTracker<HeightmapPos, HeightmapTile> createTracker() {
        return new HeightmapPlayerTracker(this);
    }

    @Override
    protected boolean anyVanillaTerrainExistsAt(@NonNull HeightmapPos pos) {
        return this.blockAccess().anyColumnIntersects(pos.x(), pos.z(), pos.level());
    }

    @Override
    public void onColumnSaved(@NonNull World world, int columnX, int columnZ, @NonNull NBTTagCompound nbt) {
        this.scheduleForUpdate(new HeightmapPos(0, columnX, columnZ));
    }

    public static class Vanilla extends HeightmapWorld {
        public Vanilla(@NonNull WorldServer world, @NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
            super(world, mode);
        }

        @Override
        public void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt) {
            throw new UnsupportedOperationException();
        }
    }

    public static class CubicChunks extends HeightmapWorld {
        public CubicChunks(@NonNull WorldServer world, @NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
            super(world, mode);
        }

        @Override
        public void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt) {
            this.scheduleForUpdate(new HeightmapPos(0, cubeX, cubeZ));
        }
    }
}
