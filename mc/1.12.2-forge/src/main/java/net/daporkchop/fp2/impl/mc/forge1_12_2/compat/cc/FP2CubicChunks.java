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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc;

import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.mode.api.ctx.IFarLevel;
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
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.exactfblocklevel.CCExactFBlockLevelHolder1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.FCube1_12_2;
import net.daporkchop.lib.math.vector.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Optional;

import static net.daporkchop.fp2.api.FP2.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = "fp2_cubicchunks", useMetadata = true, dependencies = "required-after:fp2;after:cubicchunks@[1.12.2-0.0.1188.0,)")
public class FP2CubicChunks {
    private static boolean CC;

    public static boolean isCubicWorld(World world) {
        return CC && world instanceof ICubicWorld && ((ICubicWorld) world).isCubicWorld();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CC = Loader.isModLoaded("cubicchunks");
        if (!CC) {
            event.getModLog().info("Cubic Chunks not detected, not enabling integration");
            return;
        }

        Events events = new Events();
        fp2().eventBus().register(events); //register self to receive fp2 events
        MinecraftForge.EVENT_BUS.register(events); //register self to receive forge events
    }

    /**
     * Class containing events which will be registered to activate Cubic Chunks integration.
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Events {
        protected boolean isCubicWorld(IFarLevel world) {
            return ((ICubicWorld) world.implLevel()).isCubicWorld();
        }

        //forge events

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onCubeDataSave(CubeDataEvent.Save event) {
            ICube cube = event.getCube();
            ((IMixinWorldServer) cube.getWorld()).fp2_levelServer().eventBus().fire(new CubeSavedEvent(Vec3i.of(cube.getX(), cube.getY(), cube.getZ()), new FCube1_12_2(cube), event.getData()));
        }

        //world information providers

        @FEventHandler(name = "cubicchunks_world_coordinate_limits",
                constrain = @Constrain(before = "vanilla_world_coordinate_limits"))
        public Optional<IntAxisAlignedBB> getCoordinateLimits(GetCoordinateLimitsEvent event) {
            if (this.isCubicWorld(event.world())) {
                ICubicWorld cubicWorld = (ICubicWorld) event.world().implLevel();
                int minY = cubicWorld.getMinHeight();
                int maxY = cubicWorld.getMaxHeight();

                final int HORIZONTAL_LIMIT = 30_000_000; //TODO: hard-coding this is probably a bad idea, but there don't seem to be any variables or methods i can use to get it
                return Optional.of(new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT, HORIZONTAL_LIMIT + 1, maxY, HORIZONTAL_LIMIT + 1));
            } else {
                return Optional.empty();
            }
        }

        @FEventHandler(name = "cubicchunks_world_exact_fblocklevel",
                constrain = @Constrain(before = "vanilla_world_exact_fblocklevel"))
        public Optional<ExactFBlockLevelHolder> getExactFBlockLevel(GetExactFBlockLevelEvent event) {
            return this.isCubicWorld(event.world())
                    ? Optional.of(new CCExactFBlockLevelHolder1_12((WorldServer) event.world().implLevel()))
                    : Optional.empty();
        }

        @FEventHandler(name = "cubicchunks_world_terrain_generator",
                constrain = @Constrain(before = "vanilla_world_terrain_generator"))
        public Optional<Object> getTerrainGenerator(GetTerrainGeneratorEvent event) {
            return this.isCubicWorld(event.world())
                    ? Optional.of(((ICubicWorldServer) event.world().implLevel()).getCubeGenerator())
                    : Optional.empty();
        }

        //exact generators

        @FEventHandler(name = "cubicchunks_heightmap_generator_exact",
                constrain = @Constrain(before = "vanilla_heightmap_generator_exact"))
        public Optional<IFarGeneratorExact<HeightmapPos, HeightmapTile>> createHeightmapGeneratorExact(IFarGeneratorExact.CreationEvent<HeightmapPos, HeightmapTile> event) {
            return this.isCubicWorld(event.world())
                    ? Optional.of(new CCHeightmapGenerator(event.world(), event.provider()))
                    : Optional.empty();
        }

        @FEventHandler(name = "cubicchunks_voxel_generator_exact",
                constrain = @Constrain(before = "vanilla_voxel_generator_exact"))
        public Optional<IFarGeneratorExact<VoxelPos, VoxelTile>> createVoxelGeneratorExact(IFarGeneratorExact.CreationEvent<VoxelPos, VoxelTile> event) {
            return this.isCubicWorld(event.world())
                    ? Optional.of(new CCVoxelGenerator(event.world(), event.provider()))
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
}
