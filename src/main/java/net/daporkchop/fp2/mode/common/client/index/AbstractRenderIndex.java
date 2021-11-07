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

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.common.util.alloc.SequentialFixedSizeAllocator;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.debug.util.DebugStats;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeBuffer;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.command.DrawCommand;
import net.daporkchop.fp2.gl.command.DrawCommandBuffer;
import net.daporkchop.fp2.gl.binding.DrawBinding;
import net.daporkchop.fp2.gl.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.binding.DrawMode;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.fp2.util.datastructure.SimpleSet;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractRenderIndex<POS extends IFarPos, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends AbstractRefCounted implements IRenderIndex<POS, BO, DB, DC> {
    protected final IFarRenderStrategy<POS, ?, BO, DB, DC> strategy;
    protected final ICullingStrategy<POS> cullingStrategy;

    protected final Allocator directMemoryAlloc = new DirectMemoryAllocator(true);

    protected final SimpleSet<POS> renderablePositions;
    protected final Level[] levels;

    public <T extends IFarTile> AbstractRenderIndex(@NonNull IFarRenderStrategy<POS, T, BO, DB, DC> strategy) {
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
    public IRenderIndex<POS, BO, DB, DC> retain() throws AlreadyReleasedException {
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
    public void update(@NonNull Iterable<Map.Entry<POS, Optional<BO>>> dataUpdates, @NonNull Iterable<Map.Entry<POS, Boolean>> renderableUpdates) {
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
    public void draw(int level, int pass, @NonNull DrawShaderProgram shader) {
        checkIndex(RENDER_PASS_COUNT, pass);

        if (FP2_DEBUG && !FP2Config.global().debug().levelZeroRendering() && level == 0) { //debug mode: skip level-0 rendering if needed
            return;
        }

        this.levels[level].draw(pass, shader);
    }

    @DebugOnly
    @Override
    public DebugStats.Renderer stats() {
        return Stream.of(this.levels)
                .map(Level::stats)
                .reduce(DebugStats.Renderer.builder().bakedTiles(this.renderablePositions.count()).build(), DebugStats.Renderer::add);
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

        protected long storageHandlesAddr;

        protected final IBakeOutputStorage<BO, DB, DC> storage;
        protected final List<DB> bindings;
        protected final DrawCommandBuffer<DC>[] commandBuffers = uncheckedCast(new DrawCommandBuffer[RENDER_PASS_COUNT]);

        protected boolean dirty = false;

        public Level(int level, @NonNull Allocator.GrowFunction growFunction) {
            this.level = level;

            this.directPosAccess = AbstractRenderIndex.this.strategy.mode().directPosAccess();

            this.positionsToSlots.defaultReturnValue(-1L);

            this.storage = AbstractRenderIndex.this.strategy.createBakeOutputStorage();

            this.bindings = IntStream.range(0, RENDER_PASS_COUNT)
                    .mapToObj(pass -> {
                        DrawBindingBuilder<DB> builder = this.storage.createDrawBinding(AbstractRenderIndex.this.strategy.drawLayout(), pass);
                        return builder.build();
                    })
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass] = AbstractRenderIndex.this.strategy.createCommandBuffer(this.bindings.get(pass));
            }

            this.slotAllocator = new SequentialFixedSizeAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> {
                checkArg(capacity > this.capacity, "newCapacity (%d) must be greater than current capacity (%d)", capacity, this.capacity);
                this.capacity = capacity;

                //resize memory blocks
                this.storageHandlesAddr = AbstractRenderIndex.this.directMemoryAlloc.realloc(this.storageHandlesAddr, capacity * INT_SIZE);
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    this.commandBuffers[pass].resize(toInt(capacity));
                }

                this.dirty = true;
            }), growFunction);
        }

        public void put(@NonNull POS pos, BO output) {
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
            DC[] commands = this.storage.toDrawCommands(PUnsafe.getInt(this.storageHandlesAddr + slot * (long) Integer.BYTES));

            //store in command buffer
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass].set(toInt(slot), commands[pass]);
            }
        }

        protected void eraseDrawCommands(long slot) {
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass].clear(toInt(slot));
            }
        }

        public void select(@NonNull IFrustum frustum, float partialTicks) {
            if (this.positionsToSlots.isEmpty()) { //nothing to do
                return;
            }

            this.upload();

            this.select0(frustum, partialTicks);
        }

        protected abstract void select0(@NonNull IFrustum frustum, float partialTicks);

        public void draw(int pass, @NonNull DrawShaderProgram shader) {
            if (this.positionsToSlots.isEmpty()) { //nothing to do
                return;
            }

            this.commandBuffers[pass].execute(DrawMode.QUADS, shader);
        }

        protected void upload() {
            if (this.dirty) { //re-upload all data if needed
                this.dirty = false;
            }
        }

        @Override
        public void close() {
            //free all direct memory allocations
            AbstractRenderIndex.this.directMemoryAlloc.free(this.storageHandlesAddr);

            //delete all gl objects
            Stream.of(this.commandBuffers).forEach(DrawCommandBuffer::close);
            this.bindings.forEach(DB::close);
            this.storage.release();
        }

        @DebugOnly
        protected DebugStats.Renderer stats() {
            return DebugStats.Renderer.builder()
                    .bakedTilesWithData(this.positionsToSlots.size())
                    .build()
                    .add(this.storage.stats());
        }
    }
}
