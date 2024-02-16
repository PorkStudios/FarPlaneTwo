/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.world.gen.layer;

import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.TLIntCache1_12;
import net.daporkchop.lib.common.misc.threadlocal.TL;
import net.minecraft.world.gen.layer.IntCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * @author DaPorkchop_
 */
@Mixin(IntCache.class)
public abstract class MixinIntCache1_12 {
    private static final TL<TLIntCache1_12> FP2_TL = TL.initializedWith(TLIntCache1_12::new);

    /**
     * @author DaPorkchop_
     * @reason the original code sucks
     */
    @Overwrite
    public static int[] getIntCache(int size) {
        return FP2_TL.get().getIntCache(size);
    }

    /**
     * @author DaPorkchop_
     * @reason the original code sucks
     */
    @Overwrite
    public static void resetIntCache() {
        FP2_TL.get().resetIntCache();
    }

    /**
     * @author DaPorkchop_
     * @reason the original code sucks
     */
    @Overwrite
    public static String getCacheSizes() {
        return FP2_TL.get().getCacheSizes();
    }
}
