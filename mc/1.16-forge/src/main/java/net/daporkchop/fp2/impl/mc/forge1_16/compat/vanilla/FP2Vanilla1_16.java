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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla;

import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
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
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla.exactfblockworld.VanillaExactFBlockLevelHolder1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.world.level.FLevelServer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.Util1_16;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
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
        int maxY = ((World) event.world().implLevel()).getHeight() - 1;

        final int HORIZONTAL_LIMIT = 30_000_000; //TODO: hard-coding this is probably a bad idea, but there don't seem to be any variables or methods i can use to get it
        return new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT, HORIZONTAL_LIMIT, maxY, HORIZONTAL_LIMIT);
    }

    @FEventHandler(name = "vanilla_world_exact_fblockworld")
    public ExactFBlockLevelHolder getExactFBlockWorld(GetExactFBlockLevelEvent event) {
        return new VanillaExactFBlockLevelHolder1_16((FLevelServer1_16) event.level());
    }

    @FEventHandler(name = "vanilla_world_terrain_generator")
    public Object getTerrainGenerator(GetTerrainGeneratorEvent event) {
        return ((ServerWorld) event.world().implLevel()).getChunkSource().getGenerator();
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

    @SuppressWarnings("deprecation")
    @OnlyIn(Dist.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_water",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsWater(TextureUVs.StateFaceQuadRenderEvent event) {
        BlockState state = (BlockState) event.registry().id2state(event.state());
        if (state.getBlock() == Blocks.WATER) {
            TextureAtlasSprite sprite;
            double spriteFactor;
            if (event.direction().vector().y() != 0) { //(POSITIVE|NEGATIVE)_Y
                sprite = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
                spriteFactor = 16.0d;
            } else {
                sprite = ModelBakery.WATER_FLOW.sprite();
                spriteFactor = 8.0d;
            }

            return Optional.of(Collections.singletonList(new TextureUVs.PackedBakedQuad(
                    sprite.getU(0.0d), sprite.getV(0.0d), sprite.getU(spriteFactor), sprite.getV(spriteFactor), 0.0f)));
        }

        return Optional.empty();
    }

    @SuppressWarnings("deprecation")
    @OnlyIn(Dist.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_lava",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsLava(TextureUVs.StateFaceQuadRenderEvent event) {
        BlockState state = (BlockState) event.registry().id2state(event.state());
        if (state.getBlock() == Blocks.LAVA) {
            TextureAtlasSprite sprite;
            double spriteFactor;
            if (event.direction().vector().y() != 0) { //(POSITIVE|NEGATIVE)_Y
                sprite = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
                spriteFactor = 16.0d;
            } else {
                sprite = ModelBakery.LAVA_FLOW.sprite();
                spriteFactor = 8.0d;
            }

            return Optional.of(Collections.singletonList(new TextureUVs.PackedBakedQuad(
                    sprite.getU(0.0d), sprite.getV(0.0d), sprite.getU(spriteFactor), sprite.getV(spriteFactor), 0.0f)));
        }

        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    @FEventHandler(name = "vanilla_texuvs_renderquads_default")
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsDefault(TextureUVs.StateFaceQuadRenderEvent event) {
        BlockState state = (BlockState) event.registry().id2state(event.state());
        Direction direction = Util1_16.directionToFacing(event.direction());
        IBakedModel model = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(state);

        //TODO: probably just want a null random tbh
        List<BakedQuad> quads = model.getQuads(state, direction, ThreadLocalRandom.current(), EmptyModelData.INSTANCE);
        if (quads.isEmpty()) { //the model has no cullfaces for the given facing direction, try to find a matching non-cullface
            for (BakedQuad quad : model.getQuads(state, null, ThreadLocalRandom.current(), EmptyModelData.INSTANCE)) {
                if (quad.getDirection() == direction) {
                    quads = Collections.singletonList(quad);
                    break;
                }
            }
        }

        if (!quads.isEmpty()) {
            List<TextureUVs.PackedBakedQuad> out = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                TextureAtlasSprite sprite = quad.getSprite();
                float tintFactor = quad.getTintIndex() < 0 ? 1.0f : 0.0f;
                out.add(new TextureUVs.PackedBakedQuad(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), tintFactor));
            }
            return Optional.of(out);
        }

        return Optional.empty();
    }
}
