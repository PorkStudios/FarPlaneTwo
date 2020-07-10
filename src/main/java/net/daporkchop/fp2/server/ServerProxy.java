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

package net.daporkchop.fp2.server;

import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.net.server.SPacketHeightmapData;
import net.daporkchop.fp2.net.server.SPacketRenderingStrategy;
import net.daporkchop.fp2.strategy.heightmap.HeightmapChunk;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class ServerProxy {
    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
        ServerThreadExecutor.INSTANCE.startup();
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        ServerThreadExecutor.INSTANCE.shutdown();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        int seaLevel = event.player.world.getSeaLevel();
        NETWORK_WRAPPER.sendTo(new SPacketRenderingStrategy()
                .strategy(Config.renderStrategy)
                .seaLevel(seaLevel), (EntityPlayerMP) event.player);

        for (int _chunkX = 0; _chunkX < 10; _chunkX++) {
            for (int _chunkZ = 0; _chunkZ < 10; _chunkZ++) {
                int chunkX = _chunkX;
                int chunkZ = _chunkZ;
                THREAD_POOL.submit(() -> {
                    HeightmapChunk chunk = new HeightmapChunk(chunkX, chunkZ);
                    CachedBlockAccess access = ((CachedBlockAccess.Holder) ((EntityPlayerMP) event.player).world).fp2_cachedBlockAccess();
                    access.prefetch(new AxisAlignedBB(
                            chunkX * HEIGHT_VOXELS, 0, chunkZ * HEIGHT_VOXELS,
                            (chunkX + 1) * HEIGHT_VOXELS + 1, 255, (chunkZ + 1) * HEIGHT_VOXELS + 1));
                    for (int x = 0; x < HEIGHT_VERTS; x++) {
                        for (int z = 0; z < HEIGHT_VERTS; z++) {
                            int height = access.getTopBlockY(chunkX * HEIGHT_VOXELS + x, chunkZ * HEIGHT_VOXELS + z) - 1;
                            BlockPos pos = new BlockPos(chunkX * HEIGHT_VOXELS + x, height, chunkZ * HEIGHT_VOXELS + z);
                            IBlockState state = access.getBlockState(pos);

                            while (height <= seaLevel && state.getMaterial() == Material.WATER)   {
                                pos = new BlockPos(pos.getX(), --height, pos.getZ());
                                state = access.getBlockState(pos);
                            }

                            Biome biome = access.getBiome(pos);
                            MapColor color = state.getMapColor(access, pos);
                            chunk.height(x, z, height)
                                    .color(x, z, color.colorIndex)
                                    .biome(x, z, Biome.getIdForBiome(biome))
                                    .block(x, z, Block.getStateId(state));
                        }
                    }
                    NETWORK_WRAPPER.sendTo(new SPacketHeightmapData().chunk(chunk), (EntityPlayerMP) event.player);
                });
            }
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(FP2.MODID)) {
            ConfigManager.sync(FP2.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
        }
    }
}
