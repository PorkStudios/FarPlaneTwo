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

import it.unimi.dsi.fastutil.longs.LongIterator;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommand;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.util.alloc.Allocator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Implementation of {@link AbstractRenderIndex} which does frustum culling on the CPU.
 *
 * @author DaPorkchop_
 */
public class CPUCulledRenderIndex<POS extends IFarPos, C extends IDrawIndirectCommand> extends AbstractRenderIndex<POS, C, CPUCulledRenderIndex<POS, C>, CPUCulledRenderIndex.Level<POS, C>> {
    public <T extends IFarTile> CPUCulledRenderIndex(@NonNull IFarRenderMode<POS, T> mode, @NonNull IFarRenderStrategy<POS, T, C> strategy, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        super(mode, strategy, vaoInitializer, elementArray);
    }

    @Override
    protected Level<POS, C> createLevel(@NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        return new Level<>(this, vao -> {
            vaoInitializer.accept(vao);

            vao.putElementArray(elementArray);
        }, Allocator.GrowFunction.pow2(1L));
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Level<POS extends IFarPos, C extends IDrawIndirectCommand> extends AbstractRenderIndex.Level<POS, C, CPUCulledRenderIndex<POS, C>, Level<POS, C>> {
        protected ForkJoinTask<Void> cullFuture;

        public Level(@NonNull CPUCulledRenderIndex<POS, C> parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction) {
            super(parent, vaoInitializer, growFunction);
        }

        @Override
        protected IDrawIndirectCommandBuffer<C> createCommandBuffer(@NonNull CPUCulledRenderIndex<POS, C> parent) {
            //the command buffer can be in CPU memory, as it's accessed primarily by the CPU
            return parent.strategy.createCommandBufferFactory().commandBufferGPU(parent.directMemoryAlloc, false);
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

        protected void cull(@NonNull IFrustum frustum) {
            C[] commands = this.tempCommands;
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
            }
        }

        @Override
        protected void draw0(int pass) {
            if (this.cullFuture != null) { //join the culling task if needed to ensure it's complete before we proceed
                this.cullFuture.join();
                this.cullFuture = null;
            }

            //draw the buffered draw commands. we assume a memory barrier will be placed on GL_DRAW_INDIRECT_BUFFER, and that the buffer's contents will
            //  not have been changed since select0() ran.
            this.commandBuffer.draw(pass, RENDER_PASS_COUNT, this.capacity);
        }
    }
}
