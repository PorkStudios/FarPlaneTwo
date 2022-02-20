/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.client.renderer;

import net.daporkchop.fp2.core.client.MatrixHelper;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer1_16 {
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/matrix/MatrixStack;)V",
            at = @At("HEAD"),
            require = 1, allow = 1)
    private void fp2_renderLevel_enableReversedZ(CallbackInfo ci) {
        fp2().client().enableReverseZ();
    }

    @Redirect(method = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(Lnet/minecraft/client/renderer/ActiveRenderInfo;FZ)Lnet/minecraft/util/math/vector/Matrix4f;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Matrix4f;perspective(DFFF)Lnet/minecraft/util/math/vector/Matrix4f;"),
            require = 1, allow = 1)
    private Matrix4f fp2_getProjectionMatrix_useReversedZ(double fov, float aspect, float zNear, float zFar) {
        if (fp2().client().isReverseZ()) {
            //use reversed-z projection instead of regular perspective projection
            ArrayAllocator<float[]> alloc = GlobalAllocators.ALLOC_FLOAT.get();

            float[] matrix = alloc.atLeast(MatrixHelper.MAT4_ELEMENTS);
            try {
                //generate matrix into array
                MatrixHelper.reversedZ(matrix, (float) fov, aspect, zNear);

                //load into Matrix4f
                return new Matrix4f(matrix);
            } finally {
                alloc.release(matrix);
            }
        } else {
            //use regular perspective projection
            return Matrix4f.perspective(fov, aspect, zNear, zFar);
        }
    }
}
