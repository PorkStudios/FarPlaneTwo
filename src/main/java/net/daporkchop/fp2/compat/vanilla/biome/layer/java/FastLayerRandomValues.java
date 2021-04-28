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
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 * @see GenLayerRandomValues
 */
public class FastLayerRandomValues extends AbstractFastLayer {
    protected final int limit;

    public FastLayerRandomValues(long seed, int limit) {
        super(seed);

        this.limit = positive(limit, "limit");
    }

    public FastLayerRandomValues(long seed) {
        this(seed, 256);
    }

    @Override
    public void init(@NonNull IFastLayer[] children) {
        //no-op
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        return nextInt(start(this.seed, x, z), this.limit);
    }
}
