/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.common.client.index;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.core.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.list.DrawListBuilder;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.util.GenericMatcher;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.core.mode.common.client.RenderConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractRenderIndex<BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand, DL extends DrawList<DC>> extends AbstractRefCounted implements IRenderIndex<BO, DB, DC> {
    protected final IFarRenderStrategy<BO, DB, DC> strategy;
    protected final ICullingStrategy cullingStrategy;

    protected final Allocator directMemoryAlloc = new DirectMemoryAllocator(true);

    protected final Set<TilePos> renderablePositions;
    protected final Level[] levels;

    public AbstractRenderIndex(@NonNull IFarRenderStrategy<BO, DB, DC> strategy) {
        this.strategy = strategy;
        this.cullingStrategy = this.strategy.cullingStrategy();
        this.renderablePositions = DirectTilePosAccess.newPositionSet();

        this.levels = uncheckedCast(Array.newInstance(Level.class, EngineConstants.MAX_LODS));
        for (int level = 0; level < this.levels.length; level++) {
            this.levels[level] = this.createLevel(level);
        }
    }

    protected abstract Level createLevel(int level);

    @Override
    public IRenderIndex<BO, DB, DC> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        for (Level level : this.levels) {
            level.close();
        }
    }

    @Override
    public void update(@NonNull Iterable<Map.Entry<TilePos, Optional<BO>>> dataUpdates, @NonNull Iterable<Map.Entry<TilePos, Boolean>> renderableUpdates) {
        dataUpdates.forEach(update -> {
            TilePos pos = update.getKey();
            this.levels[pos.level()].put(pos, update.getValue().orElse(null));
        });

        renderableUpdates.forEach(update -> {
            TilePos pos = update.getKey();
            this.levels[pos.level()].updateSelectable(pos, update.getValue());
        });
    }

    @Override
    public void select(@NonNull IFrustum frustum) {
        for (Level level : this.levels) {
            level.select(frustum);
        }
    }

    @Override
    public void draw(@NonNull CommandBufferBuilder builder, int level, int pass, @NonNull DrawShaderProgram shader) {
        checkIndex(RENDER_PASS_COUNT, pass);

        builder.conditional(
                () -> {
                    if (FP2_DEBUG && !fp2().globalConfig().debug().levelZeroRendering() && level == 0) { //debug mode: skip level-0 rendering if needed
                        return false;
                    }

                    return !this.levels[level].positionsToHandles.isEmpty();
                },
                levelBuilder -> this.levels[level].draw(levelBuilder, pass, shader));
    }

    @Override
    public DebugStats.Renderer stats() {
        return Stream.of(this.levels)
                .map(Level::stats)
                .reduce(DebugStats.Renderer.builder().bakedTiles(this.renderablePositions.size()).build(), DebugStats.Renderer::add);
    }

    /**
     * @author DaPorkchop_
     */
    protected abstract class Level implements AutoCloseable {
        protected final int level;

        protected final Object2IntMap<TilePos> positionsToHandles = new Object2IntOpenHashMap<>();

        protected long positionsAddr;

        protected int capacity = -1;

        protected final IBakeOutputStorage<BO, DB, DC> storage;
        protected final List<DB> bindings;
        protected final DL[] commandBuffers = uncheckedCast(Array.newInstance(GenericMatcher.find(AbstractRenderIndex.this.getClass(), AbstractRenderIndex.class, "DL"), RENDER_PASS_COUNT));

        protected final Allocator.GrowFunction growFunction;

        protected boolean dirty = false;

        public Level(int level, @NonNull Allocator.GrowFunction growFunction) {
            this.level = level;
            this.growFunction = growFunction;

            this.positionsToHandles.defaultReturnValue(-1);

            this.storage = AbstractRenderIndex.this.strategy.createBakeOutputStorage();

            this.bindings = IntStream.range(0, RENDER_PASS_COUNT)
                    .mapToObj(pass -> {
                        DrawBindingBuilder<DB> builder = this.storage.createDrawBinding(AbstractRenderIndex.this.strategy.drawLayout(), pass);
                        builder = AbstractRenderIndex.this.strategy.configureDrawBinding(builder);
                        return builder.build();
                    })
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass] = this.buildCommandBuffer(AbstractRenderIndex.this.strategy.createCommandBuffer(this.bindings.get(pass)));
            }

            this.grow();
        }

        protected abstract DL buildCommandBuffer(@NonNull DrawListBuilder<DC> builder);

        public void grow() {
            this.capacity = toInt(this.growFunction.grow(this.capacity, 1));

            //resize memory blocks
            this.positionsAddr = AbstractRenderIndex.this.directMemoryAlloc.realloc(this.positionsAddr, this.capacity * DirectTilePosAccess.size());
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass].resize(this.capacity);
            }

            this.dirty = true;
        }

        public void put(@NonNull TilePos pos, BO output) {
            int handle = this.positionsToHandles.removeInt(pos);
            if (handle >= 0) { //the position was already inserted, remove it
                //delete the bake output from the storage using the saved handle
                this.storage.delete(handle);

                //erase draw commands from the command buffer
                this.eraseDrawCommands(handle);
            }

            if (output != null) { //insertion
                //add bake output to storage
                //  doing this now that any old render data's been deleted minimizes resource wastage (e.g. allowing VBO space to be re-used by the new data)
                handle = this.storage.add(output);

                while (handle >= this.capacity) { //the handle is bigger than the current limit
                    this.grow();
                }

                this.positionsToHandles.put(pos, handle);

                DirectTilePosAccess.store(pos, this.positionsAddr + handle * DirectTilePosAccess.size());

                //if the node is selectable, set its render outputs
                if (pos.level() == 0 || AbstractRenderIndex.this.renderablePositions.contains(pos)) {
                    this.addDrawCommands(handle);
                }
            }

            this.dirty = true;
        }

        public void updateSelectable(@NonNull TilePos pos, boolean selectable) {
            int handle = this.positionsToHandles.getInt(pos);

            if (selectable) {
                if (AbstractRenderIndex.this.renderablePositions.add(pos) //we made the tile be renderable, so now we should update the render commands
                    && pos.level() != 0 && handle >= 0) {
                    this.addDrawCommands(handle);
                }
            } else {
                if (AbstractRenderIndex.this.renderablePositions.remove(pos) //we made the tile be non-renderable, so now we should delete the render commands
                    && pos.level() != 0 && handle >= 0) {
                    this.eraseDrawCommands(handle);
                }
            }
        }

        protected void addDrawCommands(int handle) {
            //initialize draw commands from bake output storage
            DC[] commands = this.storage.toDrawCommands(handle);

            //store in command buffer
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass].set(handle, commands[pass]);
            }
        }

        protected void eraseDrawCommands(int handle) {
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandBuffers[pass].clear(handle);
            }
        }

        public void select(@NonNull IFrustum frustum) {
            if (this.positionsToHandles.isEmpty()) { //nothing to do
                return;
            }

            this.upload();

            this.select0(frustum);
        }

        protected abstract void select0(@NonNull IFrustum frustum);

        public void draw(@NonNull CommandBufferBuilder builder, int pass, @NonNull DrawShaderProgram shader) {
            this.draw(builder, shader, DrawMode.QUADS, this.commandBuffers[pass], pass);
        }

        protected abstract void draw(@NonNull CommandBufferBuilder builder, @NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DL list, int pass);

        protected void upload() {
            if (this.dirty) { //re-upload all data if needed
                this.dirty = false;
            }
        }

        @Override
        public void close() {
            //free all direct memory allocations
            AbstractRenderIndex.this.directMemoryAlloc.free(this.positionsAddr);

            //delete all gl objects
            Stream.of(this.commandBuffers).forEach(DrawList::close);
            this.bindings.forEach(DB::close);
            this.storage.release();
        }

        protected DebugStats.Renderer stats() {
            return DebugStats.Renderer.builder()
                    .bakedTilesWithData(this.positionsToHandles.size())
                    .build()
                    .add(this.storage.stats());
        }
    }
}
