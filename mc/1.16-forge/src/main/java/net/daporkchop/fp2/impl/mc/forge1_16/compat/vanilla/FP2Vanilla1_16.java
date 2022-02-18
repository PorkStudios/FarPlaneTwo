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

package net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla;

import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.mode.heightmap.server.HeightmapTileProvider;
import net.daporkchop.fp2.core.mode.heightmap.server.gen.exact.VanillaHeightmapGenerator;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.server.VoxelTileProvider;
import net.daporkchop.fp2.core.mode.voxel.server.gen.exact.VanillaVoxelGenerator;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockWorldEvent;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.impl.mc.forge1_16.server.world.ExactFBlockWorld1_16;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
@Mod("fp2_vanilla")
public class FP2Vanilla1_16 {
    protected FP2 fp2;

    public FP2Vanilla1_16() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void construct(FMLConstructModEvent event) {
        InterModComms.sendTo("fp2", "early_register_events", () -> (Consumer<FEventBus>) eventBus -> eventBus.register(this));
    }

    //world information providers

    @FEventHandler(name = "vanilla_world_coordinate_limits")
    public IntAxisAlignedBB getCoordinateLimits(GetCoordinateLimitsEvent event) {
        int minY = 0;
        int maxY = ((World) event.world().fp2_IFarWorld_implWorld()).getHeight() - 1;

        final int HORIZONTAL_LIMIT = 30_000_000; //TODO: hard-coding this is probably a bad idea, but there don't seem to be any variables or methods i can use to get it
        return new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT, HORIZONTAL_LIMIT, maxY, HORIZONTAL_LIMIT);
    }

    @FEventHandler(name = "vanilla_world_exact_fblockworld")
    public FBlockWorld getExactFBlockWorld(GetExactFBlockWorldEvent event) {
        return new ExactFBlockWorld1_16((ServerWorld) event.world().fp2_IFarWorld_implWorld(), event.world().fp2_IFarWorld_registry());
    }

    @FEventHandler(name = "vanilla_world_terrain_generator")
    public Object getTerrainGenerator(GetTerrainGeneratorEvent event) {
        return ((ServerWorld) event.world().fp2_IFarWorld_implWorld()).getChunkSource().getGenerator();
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

    //tile providers

    @FEventHandler(name = "vanilla_heightmap_tileprovider")
    public IFarTileProvider<HeightmapPos, HeightmapTile> createHeightmapTileProvider(IFarTileProvider.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return new HeightmapTileProvider.Vanilla(event.world(), event.mode());
    }

    @FEventHandler(name = "vanilla_voxel_tileprovider")
    public IFarTileProvider<VoxelPos, VoxelTile> createVoxelTileProvider(IFarTileProvider.CreationEvent<VoxelPos, VoxelTile> event) {
        return new VoxelTileProvider.Vanilla(event.world(), event.mode());
    }

    //texture UVs

    @OnlyIn(Dist.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_default")
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsDefault(TextureUVs.StateFaceQuadRenderEvent event) {
        //BlockState state = (BlockState) event.registry().id2state(event.state());

        //TODO: this

        return Optional.empty();
    }
}
