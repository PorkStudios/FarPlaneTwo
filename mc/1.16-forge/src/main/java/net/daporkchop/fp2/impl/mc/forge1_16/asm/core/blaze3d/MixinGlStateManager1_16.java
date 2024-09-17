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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.blaze3d;

import com.mojang.blaze3d.platform.GlStateManager;
import net.daporkchop.fp2.core.client.render.ReversedZ;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager1_16 {
    @Unique
    private static ReversedZ fp2_getReversedZ() {
        if (!FP2Forge1_16.INITIALIZED) {
            return null;
        }

        return fp2()
                .client()
                .renderManager()
                .reversedZ();
    }

    @ModifyVariable(method = "Lcom/mojang/blaze3d/platform/GlStateManager;_depthFunc(I)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 1, allow = 1)
    private static int fp2_depthFunc_invertForReversedZ(int func) {
        ReversedZ reversedZ = fp2_getReversedZ();
        if (reversedZ != null) { //if reversed-z projection is enabled, flip function around
            return reversedZ.transformDepthFunc(func);
        }

        return func;
    }

    @ModifyVariable(method = "Lcom/mojang/blaze3d/platform/GlStateManager;_polygonOffset(FF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 1, allow = 1)
    private static float fp2_doPolygonOffset_invertFactorForReversedZ(float factor) {
        ReversedZ reversedZ = fp2_getReversedZ();
        if (reversedZ != null) { //if reversed-z projection is enabled, invert factor
            return reversedZ.transformPolygonOffsetFactor(factor);
        }

        return factor;
    }

    @ModifyVariable(method = "Lcom/mojang/blaze3d/platform/GlStateManager;_polygonOffset(FF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1,
            require = 1, allow = 1)
    private static float fp2_doPolygonOffset_invertUnitsForReversedZ(float units) {
        ReversedZ reversedZ = fp2_getReversedZ();
        if (reversedZ != null) { //if reversed-z projection is enabled, invert units
            return reversedZ.transformPolygonOffsetUnits(units);
        }

        return units;
    }
}
