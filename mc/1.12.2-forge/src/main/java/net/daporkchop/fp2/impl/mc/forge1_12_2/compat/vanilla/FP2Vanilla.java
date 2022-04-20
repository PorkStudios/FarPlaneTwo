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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla;

import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.render.TextureUVs;
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
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.exactfblocklevel.VanillaExactFBlockLevelHolder1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.generator.heightmap.FlatHeightmapGenerator;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.Util1_12_2;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
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
        int maxY = ((World) event.world().implLevel()).getHeight();

        final int HORIZONTAL_LIMIT = 30_000_000; //TODO: hard-coding this is probably a bad idea, but there don't seem to be any variables or methods i can use to get it
        return new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT, HORIZONTAL_LIMIT + 1, maxY, HORIZONTAL_LIMIT + 1);
    }

    @FEventHandler(name = "vanilla_world_exact_fblocklevel")
    public ExactFBlockLevelHolder getExactFBlockLevel(GetExactFBlockLevelEvent event) {
        return new VanillaExactFBlockLevelHolder1_12((WorldServer) event.world().implLevel());
    }

    @FEventHandler(name = "vanilla_world_terrain_generator")
    public Object getTerrainGenerator(GetTerrainGeneratorEvent event) {
        return ((WorldServer) event.world().implLevel()).getChunkProvider().chunkGenerator;
    }

    //exact generators

    @FEventHandler(name = "vanilla_heightmap_generator_exact")
    public IFarGeneratorExact<HeightmapPos, HeightmapTile> createHeightmapGeneratorExact(IFarGeneratorExact.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return new VanillaHeightmapGenerator(event.world(), event.provider());
    }

    @FEventHandler(name = "vanilla_voxel_generator_exact")
    public IFarGeneratorExact<VoxelPos, VoxelTile> createVoxelGeneratorExact(IFarGeneratorExact.CreationEvent<VoxelPos, VoxelTile> event) {
        return new VanillaVoxelGenerator(event.world(), event.provider());
    }

    //Superflat rough generators

    protected boolean isSuperflatWorld(IFarLevelServer world) {
        return world.terrainGeneratorInfo().implGenerator() instanceof ChunkGeneratorFlat;
    }

    @FEventHandler(name = "vanilla_heightmap_generator_rough_superflat")
    public Optional<IFarGeneratorRough<HeightmapPos, HeightmapTile>> createHeightmapGeneratorRoughSuperflat(IFarGeneratorRough.CreationEvent<HeightmapPos, HeightmapTile> event) {
        return this.isSuperflatWorld(event.world())
                ? Optional.of(new FlatHeightmapGenerator(event.world(), event.provider()))
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
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsWater(TextureUVs.StateFaceQuadRenderEvent event) {
        IBlockState state = (IBlockState) event.registry().id2state(event.state());
        if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER) {
            String spriteName;
            double spriteFactor;
            if (event.direction().vector().z() == 0) { //(POSITIVE|NEGATIVE)_Z
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
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsLava(TextureUVs.StateFaceQuadRenderEvent event) {
        IBlockState state = (IBlockState) event.registry().id2state(event.state());
        if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA) {
            String spriteName;
            double spriteFactor;
            if (event.direction().vector().z() == 0) { //(POSITIVE|NEGATIVE)_Z
                spriteName = "minecraft:blocks/lava_still";
                spriteFactor = 16.0d;
            } else {
                spriteName = "minecraft:blocks/lava_flow";
                spriteFactor = 8.0d;
            }

            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(spriteName);
            return Optional.of(Collections.singletonList(new TextureUVs.PackedBakedQuad(
                    sprite.getInterpolatedU(0.0d), sprite.getInterpolatedV(0.0d), sprite.getInterpolatedU(spriteFactor), sprite.getInterpolatedV(spriteFactor), 0.0f)));
        }

        return Optional.empty();
    }

    @SideOnly(Side.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_default")
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsDefault(TextureUVs.StateFaceQuadRenderEvent event) {
        IBlockState state = (IBlockState) event.registry().id2state(event.state());
        EnumFacing facing = Util1_12_2.directionToFacing(event.direction());

        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        List<BakedQuad> quads = model.getQuads(state, facing, 0L);
        if (quads.isEmpty()) { //the model has no cullfaces for the given facing direction, try to find a matching non-cullface
            for (BakedQuad quad : model.getQuads(state, null, 0L)) {
                if (quad.getFace() == facing) {
                    quads = Collections.singletonList(quad);
                    break;
                }
            }
        }

        //TODO: we could possibly do something using the code from FaceBakery#getFacingFromVertexData(int[]) to find the quad closest to this face, even if it's not
        // an exact match...

        if (!quads.isEmpty()) {
            List<TextureUVs.PackedBakedQuad> out = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                TextureAtlasSprite sprite = quad.getSprite();
                float tintFactor = quad.getTintIndex() < 0 ? 1.0f : 0.0f;
                out.add(new TextureUVs.PackedBakedQuad(sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), tintFactor));
            }
            return Optional.of(out);
        }

        return Optional.empty();
    }
}
