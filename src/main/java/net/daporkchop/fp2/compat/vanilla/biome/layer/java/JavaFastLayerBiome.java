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

package net.daporkchop.fp2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayerBiome;
import net.minecraftforge.common.BiomeManager;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelper.*;
import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelperCached.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 * @see GenLayerBiome
 */
public class JavaFastLayerBiome extends AbstractFastLayer implements IJavaTranslationLayer {
    protected static final long GENLAYERBIOME_BIOMES_OFFSET = PUnsafe.pork_getOffset(GenLayerBiome.class, "biomes"); //i can't use an access transformer for this since the field is added by Forge

    public static boolean isConstant(@NonNull GenLayerBiome vanilla) {
        return vanilla.settings != null && vanilla.settings.fixedBiome >= 0;
    }

    protected static int selectWeightedRandom(long state, int[] type) {
        return type[nextInt(state, type.length)];
    }

    protected final int[][] types;

    public JavaFastLayerBiome(@NonNull GenLayerBiome vanilla) {
        super(vanilla.worldGenSeed);
        checkArg(!isConstant(vanilla), "cannot construct FastLayerBiome with fixed biome output!");

        BiomeManager.BiomeType[] types = BiomeManager.BiomeType.values();
        List<BiomeManager.BiomeEntry>[] typeBiomes = PUnsafe.getObject(vanilla, GENLAYERBIOME_BIOMES_OFFSET);
        checkState(types.length == typeBiomes.length, "biome types (%d) != typeBiomes (%d)", types.length, typeBiomes.length);

        this.types = Stream.of(types).map(type -> {
            List<BiomeManager.BiomeEntry> biomes = typeBiomes[type.ordinal()];
            boolean modded = BiomeManager.isTypeListModded(type);

            return biomes.stream().flatMapToInt(entry -> {
                        int biomeId = Biome.getIdForBiome(entry.biome);
                        return IntStream.range(0, modded ? entry.itemWeight : entry.itemWeight / 10).map(i -> biomeId);
                    })
                    .toArray();
        }).toArray(int[][]::new);
    }

    @Override
    public int translate0(int x, int z, int rawValue) {
        int value = rawValue & 0xFF;
        int extra = (rawValue & 0xF00) >> 8;

        if (isBiomeOceanic(value) || value == ID_MUSHROOM_ISLAND) {
            return value;
        }

        long state = start(this.seed, x, z);
        switch (value) {
            case 1:
                if (extra != 0) {
                    return nextInt(state, 3) == 0 ? ID_MESA_CLEAR_ROCK : ID_MESA_ROCK;
                } else {
                    return selectWeightedRandom(state, this.types[value - 1]);
                }
            case 2:
                if (extra != 0) {
                    return ID_JUNGLE;
                } else {
                    return selectWeightedRandom(state, this.types[value - 1]);
                }
            case 3:
                if (extra != 0) {
                    return ID_REDWOOD_TAIGA;
                } else {
                    return selectWeightedRandom(state, this.types[value - 1]);
                }
            case 4:
                return selectWeightedRandom(state, this.types[value - 1]);
            default:
                return ID_MUSHROOM_ISLAND;
        }
    }
}
