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

package net.daporkchop.fp2.compat.cc;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorld;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.mode.heightmap.server.HeightmapTileProvider;
import net.daporkchop.fp2.core.mode.heightmap.server.gen.exact.CCHeightmapGenerator;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.server.VoxelTileProvider;
import net.daporkchop.fp2.core.mode.voxel.server.gen.exact.CCVoxelGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Optional;

import static net.daporkchop.fp2.api.FP2.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = "fp2_cubicchunks", useMetadata = true, dependencies = "required-after:fp2;after:cubicchunks@[1.12.2-0.0.1188.0,)")
public class FP2CubicChunks {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!Loader.isModLoaded("cubicchunks")) {
            event.getModLog().info("Cubic Chunks not detected, not enabling integration");
            return;
        }

        fp2().eventBus().register(this); //register self to receive fp2 events
    }

    //
    // all subsequent code can only be called if cubic chunks is present, and can therefore safely access the cubic chunks API
    //

    protected boolean isCubicWorld(IFarWorld world) {
        return ((ICubicWorld) world.fp2_IFarWorld_implWorld()).isCubicWorld();
    }

    //exact generators

    @FEventHandler(name = "cubicchunks_heightmap_generator_exact",
            constrain = @Constrain(before = "vanilla_heightmap_generator_exact"))
    public Optional<IFarGeneratorExact<HeightmapPos, HeightmapTile>> createHeightmapGeneratorExact(IFarGeneratorExact.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return this.isCubicWorld(event.world())
                ? Optional.of(new CCHeightmapGenerator(event.world()))
                : Optional.empty();
    }

    @FEventHandler(name = "cubicchunks_voxel_generator_exact",
            constrain = @Constrain(before = "vanilla_voxel_generator_exact"))
    public Optional<IFarGeneratorExact<VoxelPos, VoxelTile>> createVoxelGeneratorExact(IFarGeneratorExact.CreationEvent<VoxelPos, VoxelTile> event) {
        return this.isCubicWorld(event.world())
                ? Optional.of(new CCVoxelGenerator(event.world()))
                : Optional.empty();
    }

    //tile providers

    @FEventHandler(name = "cubicchunks_heightmap_tileprovider",
            constrain = @Constrain(before = "vanilla_heightmap_tileprovider"))
    public Optional<IFarTileProvider<HeightmapPos, HeightmapTile>> createHeightmapTileProvider(IFarTileProvider.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return this.isCubicWorld(event.world())
                ? Optional.of(new HeightmapTileProvider.CubicChunks(event.world(), event.mode()))
                : Optional.empty();
    }

    @FEventHandler(name = "cubicchunks_voxel_tileprovider",
            constrain = @Constrain(before = "vanilla_voxel_tileprovider"))
    public Optional<IFarTileProvider<VoxelPos, VoxelTile>> createVoxelTileProvider(IFarTileProvider.CreationEvent<VoxelPos, VoxelTile> event) {
        return this.isCubicWorld(event.world())
                ? Optional.of(new VoxelTileProvider.CubicChunks(event.world(), event.mode()))
                : Optional.empty();
    }
}
