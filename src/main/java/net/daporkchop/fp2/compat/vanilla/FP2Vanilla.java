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

import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.compat.vanilla.asyncblockaccess.VanillaAsyncBlockAccessImpl;
import net.daporkchop.fp2.compat.vanilla.generator.heightmap.FlatHeightmapGenerator;
import net.daporkchop.fp2.core.client.render.TextureUVs;
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
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockWorldEvent;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.render.TextureUVs1_12_2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        return new VanillaAsyncBlockAccessImpl((WorldServer) event.world().fp2_IFarWorld_implWorld());
    }

    @FEventHandler(name = "vanilla_world_terrain_generator")
    public Object getTerrainGenerator(GetTerrainGeneratorEvent event) {
        return ((WorldServer) event.world().fp2_IFarWorld_implWorld()).getChunkProvider().chunkGenerator;
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

    //texture UVs

    @SideOnly(Side.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_water",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsWater(TextureUVs1_12_2.StateFaceQuadRenderEvent event) {
        if (event.state().getBlock() == Blocks.WATER || event.state().getBlock() == Blocks.FLOWING_WATER) {
            String spriteName;
            double spriteFactor;
            if (event.facing().getHorizontalIndex() < 0) {
                spriteName = "minecraft:blocks/water_still";
                spriteFactor = 16.0d;
            } else {
                spriteName = "minecraft:blocks/water_flow";
                spriteFactor = 8.0d;
            }

            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(spriteName);
            return Optional.of(Collections.singletonList(new TextureUVs.PackedBakedQuad(
                    sprite.getInterpolatedU(0.0d), sprite.getInterpolatedV(0.0d), sprite.getInterpolatedU(spriteFactor), sprite.getInterpolatedV(spriteFactor), 0.0f)));
        }

        return Optional.empty();
    }

    @SideOnly(Side.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_lava",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsLava(TextureUVs1_12_2.StateFaceQuadRenderEvent event) {
        if (event.state().getBlock() == Blocks.LAVA || event.state().getBlock() == Blocks.FLOWING_LAVA) {

            return Optional.of(Collections.singletonList(TextureUVs1_12_2.quad(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
                            event.state().getBlock() == Blocks.LAVA ? "minecraft:blocks/lava_still" : "minecraft:blocks/lava_flow"), -1)));
        }

        return Optional.empty();
    }

    @SideOnly(Side.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_default")
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsDefault(TextureUVs1_12_2.StateFaceQuadRenderEvent event) {
        List<BakedQuad> quads = event.model().getQuads(event.state(), event.facing(), 0L);
        if (quads.isEmpty()) { //the model has no cullfaces for the given facing direction, try to find a matching non-cullface
            for (BakedQuad quad : event.model().getQuads(event.state(), null, 0L)) {
                if (quad.getFace() == event.facing()) {
                    quads = Collections.singletonList(quad);
                    break;
                }
            }
        }

        //TODO: we could possibly do something using the code from FaceBakery#getFacingFromVertexData(int[]) to find the quad closest to this face, even if it's not
        // an exact match...

        if (!quads.isEmpty()) {
            List<TextureUVs.PackedBakedQuad> out = new ArrayList<>(quads.size());
            for (int i = 0, len = quads.size(); i < len; i++) {
                out.add(TextureUVs1_12_2.quad(quads.get(i)));
            }
            return Optional.of(out);
        }

        return Optional.empty();
    }
}
