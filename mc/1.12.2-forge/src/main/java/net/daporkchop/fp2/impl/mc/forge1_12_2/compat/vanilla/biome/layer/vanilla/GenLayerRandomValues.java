/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.vanilla;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.GenLayerFunctions;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerRandomValues;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRandomValues;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * {@link GenLayer} implementation which generates psuedorandom values between {@code 0} and a given maximum bound (exclusive).
 *
 * @author DaPorkchop_
 */
@Getter
public class GenLayerRandomValues extends GenLayer implements GenLayerFunctions.IGenLayerFunctions {
    protected final int limit;

    public GenLayerRandomValues(long seed, int limit) {
        super(seed);

        this.limit = positive(limit, "limit");
    }

    public GenLayerRandomValues(long seed) {
        this(seed, 256);
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] arr = IntCache.getIntCache(areaWidth * areaHeight);
        for (int dy = 0; dy < areaHeight; ++dy) {
            for (int dx = 0; dx < areaWidth; ++dx) {
                this.initChunkSeed(areaX + dx, areaY + dy);
                arr[dy * areaWidth + dx] = this.nextInt(this.limit);
            }
        }
        return arr;
    }

    @Override
    public GenLayer[] fp2_getParents() {
        return new GenLayer[0];
    }

    @Override
    public GenLayer fp2_cloneLayer(GenLayer @NonNull [] clonedParents) {
        return new GenLayerRandomValues(0L, this.limit);
    }

    @Override
    public IFastLayer fp2_makeFast(@NonNull FastLayerProvider provider, IFastLayer @NonNull [] parents) {
        return provider.isNative()
                ? new JavaFastLayerRandomValues(((ATGenLayer1_12) this).getWorldGenSeed(), this.limit)
                : new NativeFastLayerRandomValues(((ATGenLayer1_12) this).getWorldGenSeed(), this.limit);
    }
}
