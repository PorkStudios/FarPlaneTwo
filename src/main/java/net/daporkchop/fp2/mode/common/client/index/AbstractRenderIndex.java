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

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommand;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBuffer;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.util.alloc.SequentialFixedSizeAllocator;
import net.daporkchop.fp2.util.datastructure.SimpleSet;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.util.GenericMatcher;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;
import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractRenderIndex<POS extends IFarPos, C extends IDrawIndirectCommand, I extends AbstractRenderIndex<POS, C, I, L>, L extends AbstractRenderIndex.Level<POS, C, I, L>> extends AbstractReleasable {
    protected final IFarDirectPosAccess<POS> directPosAccess;
    protected final IFarRenderStrategy<POS, ?, C> strategy;

    protected final Allocator directMemoryAlloc = new DirectMemoryAllocator(true);

    protected final SimpleSet<POS> renderablePositions;
    protected final L[] levels;

    public <T extends IFarTile> AbstractRenderIndex(@NonNull IFarRenderMode<POS, T> mode, @NonNull IFarRenderStrategy<POS, T, C> strategy, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        this.directPosAccess = mode.directPosAccess();
        this.strategy = strategy;

        this.renderablePositions = this.directPosAccess.newPositionSet();

        this.levels = uncheckedCast(Array.newInstance(GenericMatcher.find(this.getClass(), AbstractRenderIndex.class, "L"), MAX_LODS));
        PArrays.fill(this.levels, () -> this.createLevel(vaoInitializer, elementArray));
    }

    protected abstract L createLevel(@NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray);

    /**
     * Executes multiple updates in bulk.
     * <p>
     * For data updates, each position may be mapped to one of three different things:<br>
     * - an {@link Optional} containing a non-empty {@link BakeOutput}, in which case the bake output will be executed and inserted at the position<br>
     * - an empty {@link Optional}, in which case the position will be removed
     *
     * @param dataUpdates       the data updates to be applied
     * @param renderableUpdates the updates to tile renderability be applied
     */
    public void update(@NonNull Iterable<Map.Entry<POS, Optional<BakeOutput>>> dataUpdates, @NonNull Iterable<Map.Entry<POS, Boolean>> renderableUpdates) {
        dataUpdates.forEach(update -> {
            POS pos = update.getKey();
            this.levels[pos.level()].put(pos, update.getValue().orElse(null));
        });

        renderableUpdates.forEach(update -> {
            POS pos = update.getKey();
            this.levels[pos.level()].updateSelectable(pos, update.getValue());
        });
    }

    /**
     * Should be called before issuing any draw commands.
     * <p>
     * This will determine which tiles need to be rendered for the current frame.
     */
    public void select(@NonNull IFrustum frustum, float partialTicks) {
        for (L level : this.levels) {
            level.select(frustum, partialTicks);
        }
    }

    public boolean hasAnyTilesForLevel(int level) {
        return !this.levels[level].positionsToSlots.isEmpty();
    }

    /**
     * Draws a single render pass at the given level.
     *
     * @param level the level to render
     * @param pass  the pass to render
     */
    public void draw(int level, int pass) {
        checkIndex(RENDER_PASS_COUNT, pass);

        if (FP2_DEBUG && FP2Config.debug.skipLevel0 && level == 0) { //debug mode: skip level-0 rendering if needed
            return;
        }

        this.levels[level].draw(pass);
    }

    @Override
    protected void doRelease() {
        this.renderablePositions.release();
    }

    /**
     * @author DaPorkchop_
     */
    protected abstract static class Level<POS extends IFarPos, C extends IDrawIndirectCommand, I extends AbstractRenderIndex<POS, C, I, L>, L extends Level<POS, C, I, L>> extends AbstractReleasable implements Allocator.SequentialHeapManager {
        protected final I parent;

        protected final IFarDirectPosAccess<POS> directPosAccess;
        protected final VertexArrayObject vao = new VertexArrayObject();

        protected final Allocator slotAllocator;
        protected final Object2LongMap<POS> positionsToSlots = new Object2LongOpenHashMap<>();

        protected long capacity = -1L;

        protected final long positionSize;
        protected final GLBuffer positionsBuffer = new GLBuffer(GL_STREAM_DRAW);
        protected long positionsAddr;

        protected final long dataSize;
        protected long datasAddr;

        protected final IDrawIndirectCommandBuffer<C> commandBuffer;

        protected final C[] tempCommands;

        protected boolean dirty = false;

        public Level(@NonNull I parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction) {
            this.parent = parent;
            this.commandBuffer = this.createCommandBuffer(this.parent);

            this.directPosAccess = this.parent.directPosAccess;
            this.positionSize = PMath.roundUp(this.directPosAccess.posSize(), EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT);
            this.dataSize = this.parent.strategy.renderDataSize();

            this.positionsToSlots.defaultReturnValue(-1L);

            this.tempCommands = uncheckedCast(Array.newInstance(this.commandBuffer.commandClass(), RENDER_PASS_COUNT));
            for (int i = 0; i < RENDER_PASS_COUNT; i++) {
                this.tempCommands[i] = this.commandBuffer.newCommand();
            }

            try (VertexArrayObject vao = this.vao.bindForChange()) {
                this.directPosAccess.configureVAO(vao, this.positionsBuffer, toInt(this.positionSize), 0, 1);
                vaoInitializer.accept(vao);
            }

            this.slotAllocator = new SequentialFixedSizeAllocator(1L, this, growFunction);
        }

        protected abstract IDrawIndirectCommandBuffer<C> createCommandBuffer(@NonNull I parent);

        public void put(@NonNull POS pos, BakeOutput output) {
            long slot = this.positionsToSlots.getLong(pos);

            if (output == null) { //removal
                if (slot >= 0L) { //slot already exists, delete old data and then free the slot
                    this.positionsToSlots.remove(pos);
                    this.delete(slot);

                    this.slotAllocator.free(slot);
                } else { //slot doesn't exist, do nothing
                    return;
                }
            } else { //insertion
                checkArg(output.size == this.dataSize, "bake output has %d bytes, we require %d!", output.size, this.dataSize);

                if (slot >= 0L) { //slot already exists, delete old data so we can replace it
                    this.delete(slot);
                } else { //slot doesn't exist, allocate a new one for this position
                    this.positionsToSlots.put(pos, slot = this.slotAllocator.alloc(1L));

                    //initialize slot fields
                    this.directPosAccess.storePos(pos, this.positionsAddr + slot * this.positionSize);
                }

                //execute bake output
                //  doing this now that any old render data's been deleted minimizes resource wastage (e.g. allowing VBO space to be re-used by the new data)
                output.execute();

                //copy renderData to storage buffer
                PUnsafe.copyMemory(output.renderData, this.datasAddr + slot * this.dataSize, this.dataSize);

                //if the node is selectable, set its render outputs
                if (pos.level() == 0 || this.parent.renderablePositions.contains(pos)) {
                    this.addDrawCommands(slot);
                }
            }

            this.dirty = true;
        }

        public void updateSelectable(@NonNull POS pos, boolean selectable) {
            long slot = this.positionsToSlots.getLong(pos);

            if (selectable) {
                if (this.parent.renderablePositions.add(pos) //we made the tile be renderable, so now we should update the render commands
                    && pos.level() != 0 && slot >= 0L) {
                    this.addDrawCommands(slot);
                }
            } else {
                if (this.parent.renderablePositions.remove(pos) //we made the tile be non-renderable, so now we should delete the render commands
                    && pos.level() != 0 && slot >= 0L) {
                    this.eraseDrawCommands(slot);
                }
            }
        }

        protected void delete(long slot) {
            this.parent.strategy.deleteRenderData(this.datasAddr + slot * this.dataSize);
            this.eraseDrawCommands(slot);
        }

        protected void addDrawCommands(long slot) {
            //initialize draw commands from renderData
            this.parent.strategy.toDrawCommands(this.datasAddr + slot * this.dataSize, uncheckedCast(this.tempCommands));

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                C command = this.tempCommands[pass];
                command.baseInstance(toInt(slot, "slot"));
                this.commandBuffer.store(command, slot * RENDER_PASS_COUNT + pass);
            }
        }

        protected void eraseDrawCommands(long slot) {
            this.commandBuffer.clearRange(slot * RENDER_PASS_COUNT, RENDER_PASS_COUNT);
        }

        public void select(@NonNull IFrustum frustum, float partialTicks) {
            if (this.positionsToSlots.isEmpty()) { //nothing to do
                return;
            }

            this.upload();

            this.select0(frustum, partialTicks);
        }

        protected abstract void select0(@NonNull IFrustum frustum, float partialTicks);

        public void draw(int pass) {
            if (this.positionsToSlots.isEmpty()) { //nothing to do
                return;
            }

            try (VertexArrayObject vao = this.vao.bind()) {
                this.draw0(pass);
            }
        }

        protected abstract void draw0(int pass);

        protected void upload() {
            if (this.dirty) { //re-upload all data if needed
                this.dirty = false;

                try (GLBuffer positionsBuffer = this.positionsBuffer.bind(GL_ARRAY_BUFFER)) {
                    positionsBuffer.upload(this.positionsAddr, this.capacity * this.positionSize);
                }
            }
        }

        @Override
        public void brk(long capacity) {
            checkState(this.capacity == -1L, "heap already initialized?!?");
            this.capacity = capacity;

            //allocate memory blocks
            this.positionsAddr = this.parent.directMemoryAlloc.alloc(capacity * this.positionSize);
            this.datasAddr = this.parent.directMemoryAlloc.alloc(capacity * this.dataSize);
            this.commandBuffer.resize(capacity * RENDER_PASS_COUNT);

            this.dirty = true;
        }

        @Override
        public void sbrk(long newCapacity) {
            checkArg(newCapacity > this.capacity, "newCapacity (%d) must be greater than current capacity (%d)", newCapacity, this.capacity);
            this.capacity = newCapacity;

            //resize memory blocks
            this.positionsAddr = this.parent.directMemoryAlloc.realloc(this.positionsAddr, newCapacity * this.positionSize);
            this.datasAddr = this.parent.directMemoryAlloc.realloc(this.datasAddr, newCapacity * this.dataSize);
            this.commandBuffer.resize(newCapacity * RENDER_PASS_COUNT);

            this.dirty = true;
        }

        @Override
        protected void doRelease() {
            //free all direct memory allocations
            this.parent.directMemoryAlloc.free(this.positionsAddr);
            this.parent.directMemoryAlloc.free(this.datasAddr);

            //delete all opengl objects
            this.vao.delete();
            this.positionsBuffer.delete();
            this.commandBuffer.release();
        }
    }
}
