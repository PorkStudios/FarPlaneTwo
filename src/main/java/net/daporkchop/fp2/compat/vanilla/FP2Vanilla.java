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

package net.daporkchop.fp2.compat.vanilla;

import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.mode.heightmap.server.HeightmapTileProvider;
import net.daporkchop.fp2.core.mode.heightmap.server.gen.exact.VanillaHeightmapGenerator;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.server.VoxelTileProvider;
import net.daporkchop.fp2.core.mode.voxel.server.gen.exact.VanillaVoxelGenerator;
import net.daporkchop.fp2.compat.vanilla.generator.heightmap.FlatHeightmapGenerator;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Optional;

import static net.daporkchop.fp2.api.FP2.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = "fp2_vanilla", useMetadata = true, dependencies = "required-after:fp2")
public class FP2Vanilla {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        fp2().eventBus().register(this); //register self to receive fp2 events
    }

    //exact generators

    @FEventHandler(name = "vanilla_heightmap_generator_exact")
    public IFarGeneratorExact<HeightmapPos, HeightmapTile> createHeightmapGeneratorExact(IFarGeneratorExact.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return new VanillaHeightmapGenerator(event.world());
    }

    @FEventHandler(name = "vanilla_voxel_generator_exact")
    public IFarGeneratorExact<VoxelPos, VoxelTile> createVoxelGeneratorExact(IFarGeneratorExact.CreationEvent<VoxelPos, VoxelTile> event) {
        return new VanillaVoxelGenerator(event.world());
    }

    //Superflat rough generators

    protected boolean isSuperflatWorld(IFarWorldServer world) {
        return world.fp2_IFarWorldServer_terrainGeneratorInfo().implGenerator() instanceof ChunkGeneratorFlat;
    }

    @FEventHandler(name = "vanilla_heightmap_generator_rough_superflat")
    public Optional<IFarGeneratorRough<HeightmapPos, HeightmapTile>> createHeightmapGeneratorRoughSuperflat(IFarGeneratorRough.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return this.isSuperflatWorld(event.world())
                ? Optional.of(new FlatHeightmapGenerator(event.world()))
                : Optional.empty();
    }

    //tile providers

    @FEventHandler(name = "vanilla_heightmap_tileprovider")
    public IFarTileProvider<HeightmapPos, HeightmapTile> createHeightmapTileProvider(IFarTileProvider.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return new HeightmapTileProvider.Vanilla(event.world(), event.mode());
    }

    @FEventHandler(name = "vanilla_voxel_tileprovider")
    public IFarTileProvider<VoxelPos, VoxelTile> createVoxelTileProvider(IFarTileProvider.CreationEvent<VoxelPos, VoxelTile> event) {
        return new VoxelTileProvider.Vanilla(event.world(), event.mode());
    }
}
