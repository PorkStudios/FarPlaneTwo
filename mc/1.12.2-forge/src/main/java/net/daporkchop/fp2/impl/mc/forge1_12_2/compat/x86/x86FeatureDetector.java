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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.x86;

import net.daporkchop.lib.natives.Feature;
import net.daporkchop.lib.natives.FeatureBuilder;

/**
 * Detects hardware features supported by the current x86 CPU at runtime.
 *
 * @author DaPorkchop_
 */
public interface x86FeatureDetector extends Feature<x86FeatureDetector> {
    x86FeatureDetector INSTANCE = FeatureBuilder.<x86FeatureDetector>create(x86FeatureDetector.class)
            .addNative(Native_x86FeatureDetector.initAndGetClassName())
            .addJava("net.daporkchop.fp2.compat.x86.Java_x86FeatureDetector")
            .build(true);

    /**
     * @return the name of the best SIMD extension supported by the current CPU, or {@code ""} if unknown or the current CPU isn't x86
     */
    String maxSupportedVectorExtension();
}
