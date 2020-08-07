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

package net.daporkchop.fp2.util.compat.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.Map;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public final class CWGContext {
    public static final int BIOME_CACHE_SIZE = T_VOXELS + 2;

    @NonNull
    protected final Map<Biome, IBiomeBlockReplacer[]> biomeBlockReplacers;
    @NonNull
    protected final BiomeProvider biomeProvider;
    @NonNull
    protected final BiomeSource biomeSource;
    @NonNull
    protected final CWGBuilderFast terrainBuilder;

    public final Biome[] biomeCache = new Biome[BIOME_CACHE_SIZE * BIOME_CACHE_SIZE];

    public Biome[] getBiomes(int baseX, int baseZ, int level) {
        if (level == 0) { //base level, simply use vanilla system
            return this.biomeProvider.getBiomes(this.biomeCache, baseX - 1, baseZ - 1, BIOME_CACHE_SIZE, BIOME_CACHE_SIZE, false);
        } else { //not the base level, scale it all up
            //TODO: optimized method for generating biomes at low resolution?
            //TODO: this is really slow because it uses the vanilla biome cache
            Biome[] biomes = this.biomeCache;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = -1; x <= T_VOXELS; x++) {
                for (int z = -1; z <= T_VOXELS; z++) {
                    pos.setPos(baseX + (x << level), 0, baseZ + (z << level));
                    biomes[(z + 1) * BIOME_CACHE_SIZE + (x + 1)] = this.biomeProvider.getBiome(pos, Biomes.PLAINS);
                }
            }
            return biomes;
        }
    }
}
