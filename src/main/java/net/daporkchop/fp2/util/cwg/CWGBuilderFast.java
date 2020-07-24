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

import com.flowpowered.noise.module.Module;
import io.github.opencubicchunks.cubicchunks.cubicgen.ConversionUtils;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.NoiseSource;
import lombok.NonNull;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * @author DaPorkchop_
 */
public class CWGBuilderFast extends CustomGeneratorSettings implements IBuilder { //extending CustomGeneratorSettings eliminates an indirection
    protected static final long NOISESOURCE_MODULE_OFFSET = PUnsafe.pork_getOffset(NoiseSource.class, "module");

    protected final Module selector;
    protected final Module low;
    protected final Module high;
    protected final Module randomHeight2d;
    protected final BiomeSource biomes;

    public CWGBuilderFast(@NonNull World world, @NonNull CustomGeneratorSettings conf, @NonNull BiomeSource biomes) {
        try {
            for (Field field : CustomGeneratorSettings.class.getDeclaredFields()) {
                field.set(this, field.get(conf));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Random rnd = new Random(world.getSeed());
        this.selector = PUnsafe.getObject(NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.selectorNoiseFrequencyX, conf.selectorNoiseFrequencyY, conf.selectorNoiseFrequencyZ)
                .octaves(conf.selectorNoiseOctaves)
                .create(), NOISESOURCE_MODULE_OFFSET);
        this.low = PUnsafe.getObject(NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.lowNoiseFrequencyX, conf.lowNoiseFrequencyY, conf.lowNoiseFrequencyZ)
                .octaves(conf.lowNoiseOctaves)
                .create(), NOISESOURCE_MODULE_OFFSET);
        this.high = PUnsafe.getObject(NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.highNoiseFrequencyX, conf.highNoiseFrequencyY, conf.highNoiseFrequencyZ)
                .octaves(conf.highNoiseOctaves)
                .create(), NOISESOURCE_MODULE_OFFSET);
        this.randomHeight2d = PUnsafe.getObject(NoiseSource.perlin()
                .seed(rnd.nextLong())
                .normalizeTo(-1, 1)
                .frequency(conf.depthNoiseFrequencyX, 0, conf.depthNoiseFrequencyZ)
                .octaves(conf.depthNoiseOctaves)
                .create(), NOISESOURCE_MODULE_OFFSET);

        this.biomes = biomes;
    }

    @Override
    public double get(int x, int y, int z) {
        double height = this.biomes.getHeight(x, y, z) * this.heightFactor + this.heightOffset;
        double variation = this.biomes.getVolatility(x, y, z) * (height > y ? this.specialHeightVariationFactorBelowAverageY : 1.0d) * this.heightVariationFactor + this.heightVariationOffset;
        double d = PMath.lerp(this.low(x, y, z), this.high(x, y, z), this.selector(x, y, z)) + this.randomHeight2d(x, y, z);
        d = d * variation + height;
        return d - Math.signum(variation) * y;
    }

    public double selector(double x, double y, double z) {
        return PMath.clamp(this.selector.getValue(x, y, z) * this.selectorNoiseFactor + this.selectorNoiseOffset, 0.0d, 1.0d);
    }

    public double low(double x, double y, double z) {
        return this.low.getValue(x, y, z) * this.lowNoiseFactor + this.lowNoiseOffset;
    }

    public double high(double x, double y, double z) {
        return this.high.getValue(x, y, z) * this.highNoiseFactor + this.highNoiseOffset;
    }

    public double randomHeight2d(double x, double y, double z) {
        double d = this.randomHeight2d.getValue(x, y, z) * this.depthNoiseFactor + this.depthNoiseOffset;
        if (d < 0.0d) {
            d *= -0.3d;
        }
        d = PMath.clamp(d * 3.0d - 2.0d, -2.0d, 1.0d);
        if (d < 0.0d) {
            d /= 2.0d * 2.0d * 1.4d;
        } else {
            d /= 8.0d;
        }
        return d * 0.2d * 17.0d / 64.0d;
    }
}
