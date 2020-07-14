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

package net.daporkchop.fp2.util.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.NoiseSource;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;

import java.util.Random;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class CWGUtil {
    protected static final long BIOMEBLOCKREPLACERS_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeBlockReplacers");
    protected static final long BIOMEGEN_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeGen");

    public static Ref<CWGContext> tlCWGCtx(@NonNull World world) {
        return ThreadRef.soft(() -> {
            CustomGeneratorSettings conf = CustomGeneratorSettings.getFromWorld(world);

            BiomeSource biomeSource = new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), new BiomeProvider(world.getWorldInfo()), 2);

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
            IBuilder height = ((IBuilder) biomeSource::getHeight)
                    .mul(conf.heightFactor)
                    .add(conf.heightOffset);
            double specialVariationFactor = conf.specialHeightVariationFactorBelowAverageY;
            IBuilder volatility = ((IBuilder) biomeSource::getVolatility)
                    .mul((x, y, z) -> height.get(x, y, z) > y ? specialVariationFactor : 1)
                    .mul(conf.heightVariationFactor)
                    .add(conf.heightVariationOffset);
            IBuilder terrainBuilder = selector
                    .lerp(low, high).add(randomHeight2d).mul(volatility).add(height)
                    .sub(volatility.signum().mul((x, y, z) -> y));

            return new CWGContext(PUnsafe.getObject(biomeSource, BIOMEBLOCKREPLACERS_OFFSET), PUnsafe.getObject(biomeSource, BIOMEGEN_OFFSET), biomeSource, terrainBuilder);
        });
    }

    public static int getHeight(@NonNull IBuilder builder, int x, int z) {
        return floorI(builder.get(x, 0, z));
    }
}
