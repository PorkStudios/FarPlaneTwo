/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.compat.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.CustomCubicMod;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

import java.util.List;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class CWGHelper {
    public static final boolean CWG_V6 = new DefaultArtifactVersion(CustomCubicMod.class.getPackage().getImplementationVersion()).compareTo(new DefaultArtifactVersion("1.12.2-0.0.169.0-SNAPSHOT")) <= 0;
           // && !"%%VERSION%%".equals(Constants.getModVersion("cubicgen").get().getVersionString());

    private static final long BIOMEBLOCKREPLACERS_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeBlockReplacers");
    private static final long BIOMEGEN_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeGen");

    public static Object[][] getReplacerMapToArray_V6(@NonNull BiomeSource biomeSource) {
        assert CWG_V6;
        Class<?> swampWaterWithLilypadReplacerClass = PorkUtil.classForName("io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.replacer.SwampWaterWithLilypadReplacer");

        Map<Biome, List<Object>> replacerMap = PUnsafe.getObject(biomeSource, BIOMEBLOCKREPLACERS_OFFSET);
        int maxBiomeId = replacerMap.keySet().stream().mapToInt(FastRegistry::getId).max().orElse(0);
        Object[][] biomeReplacers = new Object[maxBiomeId + 1][];
        replacerMap.forEach((biome, list) -> biomeReplacers[FastRegistry.getId(biome)]
                = list.stream().filter(r -> !swampWaterWithLilypadReplacerClass.isInstance(r)).toArray());
        return biomeReplacers;
    }

    public static long[][] getReplacerFlagsToArray_V7(@NonNull BiomeSource biomeSource) {
        assert !CWG_V6;

        Map<Biome, BiomeSource.ReplacerData> replacerMap = PUnsafe.getObject(biomeSource, BIOMEBLOCKREPLACERS_OFFSET);
        int maxBiomeId = replacerMap.keySet().stream().mapToInt(FastRegistry::getId).max().orElse(0);
        long[][] biomeReplacers = new long[maxBiomeId + 1][];
        replacerMap.forEach((biome, replacerData) -> biomeReplacers[FastRegistry.getId(biome)] = replacerData.replacerFlags);
        return biomeReplacers;
    }

    public static BiomeProvider getBiomeGen(@NonNull BiomeSource biomeSource) {
        return PUnsafe.getObject(biomeSource, BIOMEGEN_OFFSET);
    }
}
