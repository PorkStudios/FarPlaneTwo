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
import net.daporkchop.fp2.client.gl.command.IDrawCommand;
import net.daporkchop.fp2.client.gl.command.IMultipassDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.mode.common.client.bake.IMultipassBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.util.alloc.SequentialFixedSizeAllocator;
import net.daporkchop.fp2.util.datastructure.SimpleSet;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
public abstract class AbstractRenderIndex<POS extends IFarPos, B extends IBakeOutput, C extends IDrawCommand> extends AbstractRefCounted implements IRenderIndex<POS, B, C> {
    protected final IFarRenderStrategy<POS, ?, B, C> strategy;
    protected final ICullingStrategy<POS> cullingStrategy;

    protected final Allocator directMemoryAlloc = new DirectMemoryAllocator(true);

    protected final SimpleSet<POS> renderablePositions;
    protected final Level[] levels;

    public <T extends IFarTile> AbstractRenderIndex(@NonNull IFarRenderStrategy<POS, T, B, C> strategy) {
        this.strategy = strategy;
        this.cullingStrategy = this.strategy.cullingStrategy();
        this.renderablePositions = this.strategy.mode().directPosAccess().newPositionSet();

        this.levels = uncheckedCast(Array.newInstance(Level.class, MAX_LODS));
        for (int level = 0; level < MAX_LODS; level++) {
            this.levels[level] = this.createLevel(level);
        }
    }

    protected abstract Level createLevel(int level);

    @Override
    public IRenderIndex<POS, B, C> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.renderablePositions.release();
        for (Level level : this.levels) {
            level.close();
        }
    }

    @Override
    public void update(@NonNull Iterable<Map.Entry<POS, Optional<B>>> dataUpdates, @NonNull Iterable<Map.Entry<POS, Boolean>> renderableUpdates) {
        dataUpdates.forEach(update -> {
            POS pos = update.getKey();
            this.levels[pos.level()].put(pos, update.getValue().orElse(null));
        });

        renderableUpdates.forEach(update -> {
            POS pos = update.getKey();
            this.levels[pos.level()].updateSelectable(pos, update.getValue());
        });
    }

    @Override
    public void select(@NonNull IFrustum frustum, float partialTicks) {
        for (Level level : this.levels) {
            level.select(frustum, partialTicks);
        }
    }

    @Override
    public boolean hasAnyTilesForLevel(int level) {
        return !this.levels[level].positionsToSlots.isEmpty();
    }

    @Override
    public void draw(int level, int pass) {
        checkIndex(RENDER_PASS_COUNT, pass);

        if (FP2_DEBUG && !FP2Config.global().debug().levelZeroRendering() && level == 0) { //debug mode: skip level-0 rendering if needed
            return;
        }

        this.levels[level].draw(pass);
    }

    /**
     * @author DaPorkchop_
     */
    protected abstract class Level implements AutoCloseable {
        protected final IFarDirectPosAccess<POS> directPosAccess;
        protected final int level;

        protected final Allocator slotAllocator;
        protected final Object2LongMap<POS> positionsToSlots = new Object2LongOpenHashMap<>();

        protected long capacity = -1L;

        protected final long positionSize;
        protected final GLBuffer positionsBuffer = new GLBuffer(GL_STREAM_DRAW);
        protected long positionsAddr;

        protected long storageHandlesAddr;

        protected final IMultipassBakeOutputStorage<B, C> storage;
        protected final IMultipassDrawCommandBuffer<C> commandBuffer;

        protected final VertexArrayObject[] vaos = new VertexArrayObject[RENDER_PASS_COUNT];

        protected final C[] tempCommands;

        protected boolean dirty = false;

        public Level(int level, @NonNull Allocator.GrowFunction growFunction) {
            this.level = level;

            this.directPosAccess = AbstractRenderIndex.this.strategy.mode().directPosAccess();
            this.positionSize = PMath.roundUp(this.directPosAccess.posSize(), EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT);

            this.positionsToSlots.defaultReturnValue(-1L);

            this.commandBuffer = AbstractRenderIndex.this.strategy.createCommandBuffer();
            this.storage = AbstractRenderIndex.this.strategy.createBakeOutputStorage();

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.vaos[pass] = new VertexArrayObject();
                try (VertexArrayObject vao = this.vaos[pass].bindForChange()) {
                    this.directPosAccess.configureVAO(vao, this.positionsBuffer, toInt(this.positionSize), 0, 1);
                    this.storage.configureVAO(vao, pass);
                }
            }

            this.tempCommands = uncheckedCast(Array.newInstance(this.commandBuffer.commandClass(), RENDER_PASS_COUNT));
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.tempCommands[pass] = this.commandBuffer.newCommand();
            }

            this.slotAllocator = new SequentialFixedSizeAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> {
                checkArg(capacity > this.capacity, "newCapacity (%d) must be greater than current capacity (%d)", capacity, this.capacity);
                this.capacity = capacity;

                //resize memory blocks
                this.positionsAddr = AbstractRenderIndex.this.directMemoryAlloc.realloc(this.positionsAddr, capacity * this.positionSize);
                this.storageHandlesAddr = AbstractRenderIndex.this.directMemoryAlloc.realloc(this.storageHandlesAddr, capacity * Integer.BYTES);
                this.commandBuffer.resize(toInt(capacity));

                this.dirty = true;
            }), growFunction);
        }

        public void put(@NonNull POS pos, B output) {
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
                if (slot >= 0L) { //slot already exists, delete old data so we can replace it
                    this.delete(slot);
                } else { //slot doesn't exist, allocate a new one for this position
                    this.positionsToSlots.put(pos, slot = this.slotAllocator.alloc(1L));

                    //initialize slot fields
                    this.directPosAccess.storePos(pos, this.positionsAddr + slot * this.positionSize);
                }

                //add bake output to storage
                //  doing this now that any old render data's been deleted minimizes resource wastage (e.g. allowing VBO space to be re-used by the new data)
                int handle = this.storage.add(output);

                //save handle
                PUnsafe.putInt(this.storageHandlesAddr + slot * (long) Integer.BYTES, handle);

                //if the node is selectable, set its render outputs
                if (pos.level() == 0 || AbstractRenderIndex.this.renderablePositions.contains(pos)) {
                    this.addDrawCommands(slot);
                }
            }

            this.dirty = true;
        }

        public void updateSelectable(@NonNull POS pos, boolean selectable) {
            long slot = this.positionsToSlots.getLong(pos);

            if (selectable) {
                if (AbstractRenderIndex.this.renderablePositions.add(pos) //we made the tile be renderable, so now we should update the render commands
                    && pos.level() != 0 && slot >= 0L) {
                    this.addDrawCommands(slot);
                }
            } else {
                if (AbstractRenderIndex.this.renderablePositions.remove(pos) //we made the tile be non-renderable, so now we should delete the render commands
                    && pos.level() != 0 && slot >= 0L) {
                    this.eraseDrawCommands(slot);
                }
            }
        }

        protected void delete(long slot) {
            //delete the bake output from the storage using the saved handle
            this.storage.delete(PUnsafe.getInt(this.storageHandlesAddr + slot * (long) Integer.BYTES));

            //erase draw commands from the command buffer
            this.eraseDrawCommands(slot);
        }

        protected void addDrawCommands(long slot) {
            //initialize draw commands from bake output storage
            this.storage.toDrawCommands(PUnsafe.getInt(this.storageHandlesAddr + slot * (long) Integer.BYTES), this.tempCommands);

            //store in command buffer
            this.commandBuffer.store(this.tempCommands, toInt(slot, "slot"));
        }

        protected void eraseDrawCommands(long slot) {
            this.commandBuffer.clearRange(toInt(slot, "slot"), 1);
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

            try (VertexArrayObject vao = this.vaos[pass].bind()) {
                this.commandBuffer.draw(pass);
            }
        }

        protected void upload() {
            if (this.dirty) { //re-upload all data if needed
                this.dirty = false;

                try (GLBuffer positionsBuffer = this.positionsBuffer.bind(GL_ARRAY_BUFFER)) {
                    positionsBuffer.upload(this.positionsAddr, this.capacity * this.positionSize);
                }
            }
        }

        @Override
        public void close() {
            //free all direct memory allocations
            AbstractRenderIndex.this.directMemoryAlloc.free(this.positionsAddr);
            AbstractRenderIndex.this.directMemoryAlloc.free(this.storageHandlesAddr);

            //delete all opengl objects
            Stream.of(this.vaos).forEach(VertexArrayObject::delete);
            this.positionsBuffer.delete();
            this.commandBuffer.release();
        }
    }
}
