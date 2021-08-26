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

package net.daporkchop.fp2.mode.common.client.index;

import lombok.NonNull;
import net.daporkchop.fp2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.client.VanillaRenderabilityTracker;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.command.IDrawCommand;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.util.alloc.Allocator;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * Implementation of {@link AbstractRenderIndex} which does frustum culling on the CPU.
 *
 * @author DaPorkchop_
 */
public class CPUCulledRenderIndex<POS extends IFarPos, C extends IDrawCommand> extends AbstractRenderIndex<POS, C, CPUCulledRenderIndex<POS, C>, CPUCulledRenderIndex.Level<POS, C>> {
    public <T extends IFarTile> CPUCulledRenderIndex(@NonNull IFarRenderMode<POS, T> mode, @NonNull IFarRenderStrategy<POS, T, C> strategy, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray, @NonNull ICullingStrategy<POS> cullingStrategy) {
        super(mode, strategy, vaoInitializer, elementArray, cullingStrategy);
    }

    @Override
    protected Level<POS, C> createLevel(int level, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        return new Level<>(this, vao -> {
            vaoInitializer.accept(vao);
            vao.putElementArray(elementArray);
        }, Allocator.GrowFunction.pow2(1L), level);
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Level<POS extends IFarPos, C extends IDrawCommand> extends AbstractRenderIndex.Level<POS, C, CPUCulledRenderIndex<POS, C>, Level<POS, C>> {
        protected final int level;

        public Level(@NonNull CPUCulledRenderIndex<POS, C> parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction, int level) {
            super(parent, vaoInitializer, growFunction);

            this.level = level;
        }

        @Override
        protected void select0(@NonNull IFrustum frustum, float partialTicks) {
            this.commandBuffer.select(this.cull(frustum));
        }

        protected IntPredicate cull(@NonNull IFrustum frustum) {
            if (this.level == 0) { //level-0 is tested for vanilla terrain intersection AND frustum intersection
                ICullingStrategy<POS> cullingStrategy = this.parent.cullingStrategy;
                VanillaRenderabilityTracker vanillaRenderabilityTracker = ((IMixinRenderGlobal) MC.renderGlobal).fp2_vanillaRenderabilityTracker();
                return slot -> {
                    long posAddr = this.positionsAddr + slot * this.positionSize;
                    return !cullingStrategy.blockedByVanilla(vanillaRenderabilityTracker, posAddr) && this.directPosAccess.inFrustum(posAddr, frustum);
                };
            } else { //all other levels are only tested for frustum intersection
                return slot -> this.directPosAccess.inFrustum(this.positionsAddr + slot * this.positionSize, frustum);
            }
        }
    }
}
