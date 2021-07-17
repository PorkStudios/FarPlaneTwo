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

package net.daporkchop.fp2.asm.core.world.gen.layer;

import net.minecraft.world.gen.layer.GenLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author DaPorkchop_
 */
@Mixin(GenLayer.class)
public abstract class MixinGenLayer {
    @Shadow
    public long chunkSeed;

    @Shadow
    public long worldGenSeed;

    /**
     * Major optimization to {@link #nextInt(int)} when {@code max} is a power of two.
     *
     * @author DaPorkchop_
     */
    @Overwrite
    protected int nextInt(int max) {
        int i;
        if ((max & (max - 1)) == 0) { //max is a power of two
            i = (int) (this.chunkSeed >> 24L) & (max - 1);
        } else { //max is NOT a power of two, fall back to slow implementation using modulo
            i = (int) ((this.chunkSeed >> 24L) % max);
            //equivalent to if (i < 0) { i += max; }
            i += (i >> 31) & max;
        }

        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += this.worldGenSeed;
        return i;
    }
}
