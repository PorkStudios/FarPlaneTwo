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
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.command.IDrawCommand;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.util.alloc.Allocator;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link AbstractRenderIndex} which does frustum culling on the CPU.
 *
 * @author DaPorkchop_
 */
public class CPUCulledRenderIndex<POS extends IFarPos, C extends IDrawCommand> extends AbstractRenderIndex<POS, C, CPUCulledRenderIndex<POS, C>, CPUCulledRenderIndex.Level<POS, C>> {
    public <T extends IFarTile> CPUCulledRenderIndex(@NonNull IFarRenderMode<POS, T> mode, @NonNull IFarRenderStrategy<POS, T, C> strategy, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        super(mode, strategy, vaoInitializer, elementArray);
    }

    @Override
    protected Level<POS, C> createLevel(int level, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        Consumer<VertexArrayObject> theVaoInitializer = vao -> {
            vaoInitializer.accept(vao);
            vao.putElementArray(elementArray);
        };
        Allocator.GrowFunction growFunction = Allocator.GrowFunction.pow2(1L);

        return level == 0
                ? new LevelZero<>(this, theVaoInitializer, growFunction)
                : new LevelNonZero<>(this, theVaoInitializer, growFunction);
    }

    /**
     * @author DaPorkchop_
     */
    protected static abstract class Level<POS extends IFarPos, C extends IDrawCommand> extends AbstractRenderIndex.Level<POS, C, CPUCulledRenderIndex<POS, C>, Level<POS, C>> {
        protected ForkJoinTask<Void> cullFuture;

        public Level(@NonNull CPUCulledRenderIndex<POS, C> parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction) {
            super(parent, vaoInitializer, growFunction);
        }

        @Override
        protected void select0(@NonNull IFrustum frustum, float partialTicks) {
            checkState(this.cullFuture == null, "cullFuture is non-null?!?");
            if (FP2Config.performance.asyncFrustumCulling) { //schedule async culling task using the provided frustum
                this.cullFuture = ForkJoinPool.commonPool().submit(() -> this.cull(frustum), null);
            } else { //cull immediately in the client thread
                this.cull(frustum);
            }
        }

        protected abstract void cull(@NonNull IFrustum frustum);
    }

    /**
     * @author DaPorkchop_
     */
    protected static class LevelZero<POS extends IFarPos, C extends IDrawCommand> extends Level<POS, C> {
        public LevelZero(@NonNull CPUCulledRenderIndex<POS, C> parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction) {
            super(parent, vaoInitializer, growFunction);
        }

        @Override
        protected void cull(@NonNull IFrustum frustum) {
            /*C[] commands = this.tempCommands;
            checkState(commands.length == RENDER_PASS_COUNT);

            VanillaRenderabilityTracker vanillaRenderabilityTracker = ((IMixinRenderGlobal) MC.renderGlobal).fp2_vanillaRenderabilityTracker();

            for (Iterator<Object2LongMap.Entry<POS>> itr = this.positionsToSlots.object2LongEntrySet().iterator(); itr.hasNext(); ) {
                Object2LongMap.Entry<POS> entry = itr.next();
                VoxelPos pos = (VoxelPos) entry.getKey(); //TODO: this
                long slot = entry.getLongValue();

                //load all commands
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    this.commandBuffer.load(commands[pass], slot * RENDER_PASS_COUNT + pass);
                }

                //check frustum intersection and update instanceCount for all commands accordingly
                boolean inFrustum = !vanillaRenderabilityTracker.vanillaBlocksFP2RenderingAtLevel0(pos.x(), pos.y(), pos.z())
                                    && this.directPosAccess.inFrustum(this.positionsAddr + slot * this.positionSize, frustum);
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    C command = commands[pass];
                    command.instanceCount(inFrustum && !command.isEmpty() ? 1 : 0);
                }

                //store commands again
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    this.commandBuffer.store(commands[pass], slot * RENDER_PASS_COUNT + pass);
                }
            }*/
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class LevelNonZero<POS extends IFarPos, C extends IDrawCommand> extends Level<POS, C> {
        public LevelNonZero(@NonNull CPUCulledRenderIndex<POS, C> parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction) {
            super(parent, vaoInitializer, growFunction);
        }

        @Override
        protected void cull(@NonNull IFrustum frustum) {
            /*C[] commands = this.tempCommands;
            checkState(commands.length == RENDER_PASS_COUNT);

            for (LongIterator itr = this.positionsToSlots.values().iterator(); itr.hasNext(); ) {
                long slot = itr.nextLong();

                //load all commands
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    this.commandBuffer.load(commands[pass], slot * RENDER_PASS_COUNT + pass);
                }

                //check frustum intersection and update instanceCount for all commands accordingly
                boolean inFrustum = this.directPosAccess.inFrustum(this.positionsAddr + slot * this.positionSize, frustum);
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    C command = commands[pass];
                    command.instanceCount(inFrustum && !command.isEmpty() ? 1 : 0);
                }

                //store commands again
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    this.commandBuffer.store(commands[pass], slot * RENDER_PASS_COUNT + pass);
                }
            }*/
        }
    }
}
