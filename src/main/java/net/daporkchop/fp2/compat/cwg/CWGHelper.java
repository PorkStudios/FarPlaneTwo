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

package net.daporkchop.fp2.compat.cwg;

import com.flowpowered.noise.Utils;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.replacer.SwampWaterWithLilypadReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.List;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class CWGHelper {
    protected static final long BIOMEBLOCKREPLACERS_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeBlockReplacers");
    protected static final long BIOMEGEN_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeGen");

    public static Map<Biome, List<IBiomeBlockReplacer>> getReplacerMap(@NonNull BiomeSource biomeSource) {
        return PUnsafe.getObject(biomeSource, BIOMEBLOCKREPLACERS_OFFSET);
    }

    public static BiomeProvider getBiomeGen(@NonNull BiomeSource biomeSource) {
        return PUnsafe.getObject(biomeSource, BIOMEGEN_OFFSET);
    }

    public static IBiomeBlockReplacer[][] blockReplacerMapToArray(@NonNull Map<Biome, List<IBiomeBlockReplacer>> replacerMap) {
        int maxBiomeId = replacerMap.keySet().stream().mapToInt(Biome::getIdForBiome).max().orElse(0);
        IBiomeBlockReplacer[][] biomeReplacers = new IBiomeBlockReplacer[maxBiomeId + 1][];
        replacerMap.forEach((biome, list) -> biomeReplacers[Biome.getIdForBiome(biome)]
                = list.stream().filter(r -> !(r instanceof SwampWaterWithLilypadReplacer)).toArray(IBiomeBlockReplacer[]::new));
        return biomeReplacers;
    }
}
