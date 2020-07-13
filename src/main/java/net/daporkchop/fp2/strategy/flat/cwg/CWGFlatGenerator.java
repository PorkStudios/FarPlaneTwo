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

package net.daporkchop.fp2.strategy.flat.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomTerrainGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.NoiseSource;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.flat.FlatGenerator;
import net.daporkchop.fp2.strategy.flat.FlatPiece;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

import static java.lang.Math.*;
import static net.daporkchop.fp2.strategy.flat.FlatConstants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class CWGFlatGenerator implements FlatGenerator {
    protected Ref<BiomeSource> biomesCache;
    protected Ref<IBuilder> terrainBuilder;

    @Override
    public void init(@NonNull WorldServer world) {
        CustomGeneratorSettings conf = CustomGeneratorSettings.getFromWorld(world);

        this.biomesCache = ThreadRef.late(() -> new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), new BiomeProvider(world.getWorldInfo()), 2));
        this.terrainBuilder = ThreadRef.late(() -> {
            Random rnd = new Random(world.getSeed());

            IBuilder selector = NoiseSource.perlin()
                    .seed(rnd.nextLong())
                    .normalizeTo(-1, 1)
                    .frequency(conf.selectorNoiseFrequencyX, conf.selectorNoiseFrequencyY, conf.selectorNoiseFrequencyZ)
                    .octaves(conf.selectorNoiseOctaves)
                    .create()
                    .mul(conf.selectorNoiseFactor).add(conf.selectorNoiseOffset).clamp(0, 1);

            IBuilder low = NoiseSource.perlin()
                    .seed(rnd.nextLong())
                    .normalizeTo(-1, 1)
                    .frequency(conf.lowNoiseFrequencyX, conf.lowNoiseFrequencyY, conf.lowNoiseFrequencyZ)
                    .octaves(conf.lowNoiseOctaves)
                    .create()
                    .mul(conf.lowNoiseFactor).add(conf.lowNoiseOffset);

            IBuilder high = NoiseSource.perlin()
                    .seed(rnd.nextLong())
                    .normalizeTo(-1, 1)
                    .frequency(conf.highNoiseFrequencyX, conf.highNoiseFrequencyY, conf.highNoiseFrequencyZ)
                    .octaves(conf.highNoiseOctaves)
                    .create()
                    .mul(conf.highNoiseFactor).add(conf.highNoiseOffset);

            IBuilder randomHeight2d = NoiseSource.perlin()
                    .seed(rnd.nextLong())
                    .normalizeTo(-1, 1)
                    .frequency(conf.depthNoiseFrequencyX, 0, conf.depthNoiseFrequencyZ)
                    .octaves(conf.depthNoiseOctaves)
                    .create()
                    .mul(conf.depthNoiseFactor).add(conf.depthNoiseOffset)
                    .mulIf(IBuilder.NEGATIVE, -0.3).mul(3).sub(2).clamp(-2, 1)
                    .divIf(IBuilder.NEGATIVE, 2 * 2 * 1.4).divIf(IBuilder.POSITIVE, 8)
                    .mul(0.2 * 17 / 64.0);

            BiomeSource biomeSource = this.biomesCache.get();
            IBuilder height = ((IBuilder) biomeSource::getHeight)
                    .mul(conf.heightFactor)
                    .add(conf.heightOffset);

            double specialVariationFactor = conf.specialHeightVariationFactorBelowAverageY;
            IBuilder volatility = ((IBuilder) biomeSource::getVolatility)
                    .mul((x, y, z) -> height.get(x, y, z) > y ? specialVariationFactor : 1)
                    .mul(conf.heightVariationFactor)
                    .add(conf.heightVariationOffset);

            return selector
                    .lerp(low, high).add(randomHeight2d).mul(volatility).add(height)
                    .sub(volatility.signum().mul((x, y, z) -> y));
        });
    }

    @Override
    public void generateRough(@NonNull CachedBlockAccess world, @NonNull FlatPiece piece) {
        int pieceX = piece.x();
        int pieceZ = piece.z();

        BiomeSource biomeSource = this.biomesCache.get();
        IBuilder terrainBuilder = this.terrainBuilder.get();

        for (int x = 0; x < FLAT_VERTS; x++) {
            for (int z = 0; z < FLAT_VERTS; z++) {
                int blockX = pieceX * FLAT_VOXELS + x;
                int blockZ = pieceZ * FLAT_VOXELS + z;

                //find height
                //this can be considered to be some form of binary search
                int height = 0;
                double density = 0.0d;
                {
                    height = floorI(density = terrainBuilder.get(blockX, height, blockZ));
                    /*if (density > 0.0d) {
                        double next;
                        while ((next = terrainBuilder.get(blockX, height + 1, blockZ)) > 0.0d) {
                            density = next;
                            height++;
                        }
                    } else {
                        double next;
                        while ((next = terrainBuilder.get(blockX, height - 1, blockZ)) <= 0.0d) {
                            density = next;
                            height--;
                        }
                    }*/
                }

                Biome biome = biomeSource.getBiome(blockX, height, blockZ).getBiome();

                IBlockState state = Blocks.AIR.getDefaultState();
                List<IBiomeBlockReplacer> replacers = biomeSource.getReplacers(blockX, height, blockZ);
                double dx = terrainBuilder.get(blockX + 1, height, blockZ) - density;
                double dy = terrainBuilder.get(blockX, height + 1, blockZ) - density;
                double dz = terrainBuilder.get(blockX, height, blockZ + 1) - density;
                for (int i = 0, size = replacers.size(); i < size; i++) {
                    state = replacers.get(i).getReplacedBlock(state, blockX, height, blockZ, dx, dy, dz, density);
                }

                MapColor color = state.getMapColor(world, new BlockPos(blockX, height, blockZ));
                piece.height(x, z, height)
                        .color(x, z, color.colorIndex)
                        .biome(x, z, Biome.getIdForBiome(biome))
                        .block(x, z, Block.getStateId(state))
                        .light(x, z, (height < 63 ? max(15 - (63 - height) * 3, 0) : 15) << 16);
            }
        }
    }

    @Override
    public void generateExact(@NonNull CachedBlockAccess world, @NonNull FlatPiece piece) {
        int pieceX = piece.x();
        int pieceZ = piece.z();
        world.prefetch(new AxisAlignedBB(
                pieceX * FLAT_VOXELS, 0, pieceZ * FLAT_VOXELS,
                (pieceX + 1) * FLAT_VOXELS, 255, (pieceZ + 1) * FLAT_VOXELS));
        for (int x = 0; x < FLAT_VERTS; x++) {
            for (int z = 0; z < FLAT_VERTS; z++) {
                int height = world.getTopBlockY(pieceX * FLAT_VOXELS + x, pieceZ * FLAT_VOXELS + z);
                BlockPos pos = new BlockPos(pieceX * FLAT_VOXELS + x, height, pieceZ * FLAT_VOXELS + z);
                IBlockState state = world.getBlockState(pos);

                while (height <= 63 && state.getMaterial() == Material.WATER) {
                    pos = new BlockPos(pos.getX(), --height, pos.getZ());
                    state = world.getBlockState(pos);
                }

                Biome biome = world.getBiome(pos);
                MapColor color = state.getMapColor(world, pos);
                piece.height(x, z, height)
                        .color(x, z, color.colorIndex)
                        .biome(x, z, Biome.getIdForBiome(biome))
                        .block(x, z, Block.getStateId(state))
                        .light(x, z, world.getCombinedLight(pos.add(0, 1, 0), 0) >> 4);
            }
        }
    }
}
