/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
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

package dev.farplane.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Provides biome-aware colors for terrain rendering.
 * <p>
 * This replaces the placeholder green with actual biome tints for grass, foliage, and water.
 *
 * @author FarPlane contributors
 */
public class BiomeColorProvider {
    private static final int DEFAULT_GRASS_COLOR = 0x79A83E;   // Plains grass
    private static final int DEFAULT_FOLIAGE_COLOR = 0x59AE30; // Oak leaves
    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;   // Water

    /**
     * Gets the tint color for a block at the given position.
     *
     * @param level     the client level
     * @param pos       the block position
     * @param state     the block state
     * @param biomeHash the biome hash (placeholder from TileData)
     * @return RGB color as float array [r, g, b]
     */
    public static float[] getTintColor(ClientLevel level, BlockPos pos, BlockState state, int biomeHash) {
        if (level == null) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }

        Holder<Biome> biome = level.getBiome(pos);

        // Check block type for tinting
        if (isGrassBlock(state)) {
            int color = biome.value().getGrassColor(pos.getX(), pos.getZ());
            return unpackColor(color);
        } else if (isFoliageBlock(state)) {
            int color = biome.value().getFoliageColor();
            return unpackColor(color);
        } else if (isWaterBlock(state)) {
            int color = biome.value().getWaterColor();
            return unpackColor(color);
        }

        // No tint
        return new float[]{1.0f, 1.0f, 1.0f};
    }

    /**
     * Gets a simple color for a block state (for v1 rendering without textures).
     */
    public static float[] getBlockColor(BlockState state) {
        if (state == null || state.isAir()) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }

        // Stone/ore - gray
        if (isStoneBlock(state)) {
            return new float[]{0.5f, 0.5f, 0.5f};
        }

        // Dirt - brown
        if (isDirtBlock(state)) {
            return new float[]{0.6f, 0.4f, 0.2f};
        }

        // Grass - green (will be tinted by biome)
        if (isGrassBlock(state)) {
            return new float[]{0.4f, 0.7f, 0.3f};
        }

        // Water - blue
        if (isWaterBlock(state)) {
            return new float[]{0.2f, 0.4f, 0.8f};
        }

        // Sand - yellow
        if (isSandBlock(state)) {
            return new float[]{0.9f, 0.8f, 0.5f};
        }

        // Wood - brown
        if (isWoodBlock(state)) {
            return new float[]{0.6f, 0.4f, 0.2f};
        }

        // Leaves - green (will be tinted by biome)
        if (isFoliageBlock(state)) {
            return new float[]{0.3f, 0.6f, 0.2f};
        }

        // Default - light gray
        return new float[]{0.7f, 0.7f, 0.7f};
    }

    private static float[] unpackColor(int rgb) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f
        };
    }

    private static boolean isGrassBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS);
    }

    private static boolean isFoliageBlock(BlockState state) {
        return state.is(Blocks.OAK_LEAVES) || state.is(Blocks.BIRCH_LEAVES) ||
               state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.JUNGLE_LEAVES) ||
               state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES);
    }

    private static boolean isWaterBlock(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.KELP) || state.is(Blocks.SEAGRASS);
    }

    private static boolean isStoneBlock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) ||
               state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE);
    }

    private static boolean isDirtBlock(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL);
    }

    private static boolean isSandBlock(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL);
    }

    private static boolean isWoodBlock(BlockState state) {
        return state.is(Blocks.OAK_LOG) || state.is(Blocks.BIRCH_LOG) ||
               state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.JUNGLE_LOG);
    }
}
