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

package net.daporkchop.fp2.compat.vanilla.biome.layer;

import lombok.NonNull;
import net.daporkchop.lib.natives.Feature;
import net.daporkchop.lib.natives.FeatureBuilder;
import net.minecraft.world.gen.layer.GenLayer;

/**
 * Provides faster alternatives to the standard Minecraft {@link GenLayer}s.
 *
 * @author DaPorkchop_
 */
public interface FastLayerProvider extends Feature<FastLayerProvider> {

    /**
     * Converts the given {@link GenLayer}s to their {@link FastLayer} equivalents.
     * <p>
     * Note that if you have multiple {@link GenLayer}s to convert, you should convert them all at once with a single invocation of this method, rather than
     * converting them each individually. Doing so may provide a not insignificant performance boost.
     *
     * @param inputs the {@link GenLayer}s
     * @return the converted {@link FastLayer}s, in the same order as the inputs were provided in
     */
    FastLayer[] makeFast(@NonNull GenLayer... inputs);
}
