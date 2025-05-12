/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.client.index.attribdivisor;

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.NIOBufferUtil;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.AbstractRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.postable.PerLevelRenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.RenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.SimpleRenderPosTable;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.buffer.BufferAccess;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.download.AsynchronousSmallBufferDownloader;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.util.list.DirectDrawElementsIndirectCommandList;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.closeable.QuietCloseable;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Arrays;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractGPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct, LEVEL extends AbstractGPUCulledBaseInstanceRenderIndex.Level> extends AbstractRenderIndex.WithTilePosAttrib<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = WithTilePosAttrib.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_multi_draw_indirect) //glMultiDrawElementsIndirect()
            .addAll(ComputeShaderProgram.REQUIRED_EXTENSIONS)
            .addAll(TerrainRenderingBlockedTracker.REQUIRED_EXTENSIONS_GPU)
            .add(GLExtension.GL_ARB_shader_storage_buffer_object)
            .add(GLExtension.GL_ARB_shader_image_load_store); //glMemoryBarrier()

    protected static final GLExtensionSet REQUIRED_EXTENSIONS_COUNT_SELECTED = REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_shader_atomic_counters);

    protected static final GLExtensionSet REQUIRED_EXTENSIONS_INDIRECT_COUNT = REQUIRED_EXTENSIONS_COUNT_SELECTED
            .add(GLExtension.GL_ARB_indirect_parameters);

    //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.comp
    //synced with resources/assets/fp2/shaders/comp/tile_visibility_and_occlusion_test.comp
    protected static final int CULLING_SHADER_WORK_GROUP_SIZE = 256;

    protected static final long COUNT_SELECTED_BUFFER_LEVEL_STRIDE = (1 + RENDER_PASS_COUNT) * Integer.BYTES;
    protected static final long COUNT_SELECTED_BUFFER_TILES_OFFSET = 0L;
    protected static final long COUNT_SELECTED_BUFFER_PASSES_OFFSET = Integer.BYTES;

    /**
     * A reference to the camera state uniforms buffer.
     */
    protected final UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer;

    protected final LevelArray<LEVEL> levels;

    /**
     * A buffer containing the atomic counters indicating the number of tiles and number of draw commands selected per level and per pass.
     */
    protected final GLBuffer countSelectedBuffer;
    protected final boolean useIndirectCount;
    protected final AsynchronousSmallBufferDownloader debugStatisticsDownloader;

    /**
     * The total number of draw commands which are currently renderable (i.e. non-empty draw commands whose tile isn't hidden).
     */
    protected int nonEmptyCommandCount;

    /**
     * The total number of tiles which are currently renderable (i.e. tiles which aren't hidden and which contain at least one non-empty draw command).
     */
    protected int nonEmptyTileCount;

    protected Stats latestDebugStats;

    public AbstractGPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer, String implName) {
        super(gl.checkSupported(REQUIRED_EXTENSIONS), bakeStorage, alloc, globalRenderer);

        try {
            this.cameraStateUniformsBuffer = cameraStateUniformsBuffer;

            this.levels = new LevelArray<>(this::createLevel);

            boolean canCountSelected = gl.supports(REQUIRED_EXTENSIONS_COUNT_SELECTED);
            boolean useIndirectCount = canCountSelected && gl.supports(REQUIRED_EXTENSIONS_INDIRECT_COUNT);
            boolean useStatisticsDownloader = canCountSelected && gl.supports(AsynchronousSmallBufferDownloader.REQUIRED_EXTENSIONS);
            if (canCountSelected && (useIndirectCount || useStatisticsDownloader)) {
                //if we can count the number of selected tiles per detail level and at least one of the features which uses that information is also
                //  supported, then we'll enable selected tile counting
                this.countSelectedBuffer = GLBuffer.createFunctionallyImmutable(gl, (long) EngineConstants.MAX_LODS * COUNT_SELECTED_BUFFER_LEVEL_STRIDE, BufferUsage.STREAM_COPY, 0);
                this.useIndirectCount = useIndirectCount;
                this.debugStatisticsDownloader = useStatisticsDownloader ? new AsynchronousSmallBufferDownloader(gl, (int) this.countSelectedBuffer.capacity()) : null;
            } else {
                this.countSelectedBuffer = null;
                this.useIndirectCount = false;
                this.debugStatisticsDownloader = null;
            }

            this.latestDebugStats = new Stats(-1, -1, 0, -1, -1, implName + ", " + (this.useIndirectCount ? "glMultiDrawElementsIndirectCount" : "glMultiDrawElementsIndirect"));
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored = PResourceUtil.lazyCloseAll(
                this.levels,
                this.countSelectedBuffer,
                this.debugStatisticsDownloader)) {
            super.close();
        }
    }

    @Override
    protected final RenderPosTable constructRenderPosTable(AttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        return new PerLevelRenderPosTable(level -> new SimpleRenderPosTable(this.gl, sharedVertexFormat, this.alloc, (oldCapacity, increment) -> {
            val newCapacity = Allocator.GrowFunction.sqrt2(CULLING_SHADER_WORK_GROUP_SIZE).grow(oldCapacity, increment);
            this.levels.get(level).capacityChanged(Math.toIntExact(newCapacity));
            return newCapacity;
        }));
    }

    /**
     * Creates a new {@link LEVEL} instance for storing information about a specific detail level.
     *
     * @param levelIndex the detail level
     * @return a new {@link LEVEL} instance for the given detail level
     */
    protected abstract LEVEL createLevel(int levelIndex);

    @Override
    protected final void recomputeTile(@NonNull TilePos pos) {
        LEVEL levelInstance = this.levels.get(pos.level());
        BakeStorage.Location[] locations = this.bakeStorage.find(pos);

        if (locations == null || this.hiddenPositions.contains(pos)) {
            //the position doesn't have any render data data associated with it or is hidden, and therefore can be entirely omitted from the index
            int baseInstance = this.renderPosTable.remove(pos);

            if (baseInstance >= 0) {
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    levelInstance.rawDrawListsCPU.get(pass).setZero(baseInstance);
                }

                levelInstance.setNonEmptyCommandMask(this, baseInstance, (byte) 0);
                levelInstance.rawDrawListsDirty = true;
            }
        } else {
            //configure the render commands which will be used when selecting this
            int baseInstance = this.renderPosTable.add(pos);

            byte nonEmptyCommandMask = 0;
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                val location = locations[pass];
                val command = new DrawElementsIndirectCommand();
                if (location != null) {
                    //if the bake storage contains render data for the tile at this pass, configure the draw command accordingly
                    command.count = location.count;
                    command.baseInstance = baseInstance;
                    command.firstIndex = location.firstIndex;
                    command.baseVertex = location.baseVertex;
                    command.instanceCount = 1;

                    nonEmptyCommandMask |= (byte) (1 << pass);
                } else {
                    //otherwise, the draw command will be filled with zeroes
                }
                levelInstance.rawDrawListsCPU.get(pass).set(baseInstance, command);
            }

            levelInstance.setNonEmptyCommandMask(this, baseInstance, nonEmptyCommandMask);
            levelInstance.rawDrawListsDirty = true;
        }
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder);

        builder.vao();
        builder.buffer(BufferTarget.DRAW_INDIRECT_BUFFER);

        if (this.useIndirectCount) { //if we're using MultiDraw with indirect counts, we need to preserve the GL_PARAMETER_BUFFER binding
            builder.buffer(BufferTarget.PARAMETER_BUFFER);
        }
    }

    @Override
    public void preDraw(DrawArguments args) {
        super.preDraw(args);

        if (this.useIndirectCount) { //if we're using MultiDraw with indirect counts, we need to bind the GL_PARAMETER_BUFFER
            this.gl.glBindBuffer(GL_PARAMETER_BUFFER, this.countSelectedBuffer.id());
        }
    }

    @Override
    public void draw(DrawArguments args, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        val levelInstance = this.levels.get(level);
        if (levelInstance.capacityTiles != 0) {
            this.gl.glBindVertexArray(this.vaos.get(level, pass).id());
            this.gl.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, levelInstance.culledDrawListsGPU.id());
            this.gl.glMemoryBarrier(GL_COMMAND_BARRIER_BIT);

            val modeEnum = this.bakeStorage.drawMode.mode();
            val type = this.bakeStorage.indexFormat.type().type();
            val indirect = (long) pass * levelInstance.capacityTiles * DrawElementsIndirectCommand._SIZE;
            val drawCount = levelInstance.capacityTiles;
            val stride = 0;
            if (this.useIndirectCount) {
                //use indirect MultiDraw with indirect counts!
                //  glMemoryBarrier(GL_COMMAND_BARRIER_BIT) also works as a barrier on GL_PARAMETER_BUFFER, so our memory ordering is safe
                val indirectCount = (long) level * COUNT_SELECTED_BUFFER_LEVEL_STRIDE
                        + COUNT_SELECTED_BUFFER_PASSES_OFFSET
                        + (long) pass * Integer.BYTES;
                this.gl.glMultiDrawElementsIndirectCount(modeEnum, type, indirect, indirectCount, drawCount, stride);
            } else {
                //use ordinary indirect MultiDraw, with unselected tiles skipped by inserting empty commands on the GPU side
                this.gl.glMultiDrawElementsIndirect(modeEnum, type, indirect, drawCount, stride);
            }
        }
    }

    @Override
    public void postDraw(DrawArguments args) {
        super.postDraw(args);

        if (this.debugStatisticsDownloader != null) { //if debug statistics are supported, tick the statistics downloader to fetch the values from the latest frame
            this.debugStatisticsDownloader.tick();
        }
    }

    @Override
    public final PosTechnique posTechnique() {
        return PosTechnique.VERTEX_ATTRIBUTE;
    }

    @Override
    public final Stats stats() {
        //return the latest statistics (this is updated by the callback passed to the statistics downloader)
        return this.latestDebugStats;
    }

    protected final void enqueueDebugStatisticsUpdate(boolean includeSelectedTiles) {
        if (this.debugStatisticsDownloader != null) { //if debug statistics are supported, download the selected tile count to the CPU and use it to update the debug statistics
            int indexedTiles = this.renderPosTable.size();
            int hiddenTiles = this.hiddenPositions.size();
            int indexedCommands = this.nonEmptyCommandCount;
            this.debugStatisticsDownloader.downloadRange(this.countSelectedBuffer, 0L, (int) this.countSelectedBuffer.capacity(), data -> {
                //add up the number of selected tiles at each detail level
                int[] dataArray = NIOBufferUtil.toArray(data.asIntBuffer());
                assert (dataArray.length % (1 + RENDER_PASS_COUNT)) == 0 : "not a multiple of " + (1 + RENDER_PASS_COUNT) + ": " + dataArray.length;

                int selectedTiles = 0;
                int selectedCommands = 0;
                for (int i = 0; i < dataArray.length; i += 1 + RENDER_PASS_COUNT) {
                    selectedTiles += dataArray[i];

                    for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                        selectedCommands += dataArray[i + 1 + pass];
                    }
                }

                this.latestDebugStats = new Stats(
                        includeSelectedTiles ? selectedTiles : -1, indexedTiles, hiddenTiles,
                        selectedCommands, indexedCommands,
                        this.latestDebugStats.implName());
            });
        }
    }

    /**
     * Represents the render state at a single detail level.
     *
     * @author DaPorkchop_
     */
    protected static class Level implements QuietCloseable {
        protected final OpenGL gl;

        protected final PassArray<DirectDrawElementsIndirectCommandList> rawDrawListsCPU;
        protected GLBuffer rawDrawListsGPU;
        protected GLBuffer culledDrawListsGPU;

        //this is really only here for the purpose of updating the debug statistics, it lets us correctly update the total number of non-empty draw commands
        protected byte[] nonEmptyCommandMaskPerTile = PorkUtil.emptyByteArray();

        protected final int[] nonEmptyCommandCountPerPass = new int[RENDER_PASS_COUNT];
        protected int nonEmptyCommandCountTotal;
        protected int nonEmptyTileCountTotal;

        protected int capacityTiles;

        protected boolean rawDrawListsDirty;

        public Level(@NonNull OpenGL gl, @NonNull DirectMemoryAllocator alloc) {
            try {
                this.gl = gl;

                this.rawDrawListsCPU = new PassArray<>(pass -> new DirectDrawElementsIndirectCommandList(alloc));

                //don't bother allocating any buffers initially, since they're just going to be empty
                this.rawDrawListsGPU = null;
                this.culledDrawListsGPU = null;
            } catch (Throwable t) {
                throw PResourceUtil.closeSuppressed(t, this);
            }
        }

        @Override
        public void close() {
            PResourceUtil.closeAll(
                    this.rawDrawListsCPU,
                    this.rawDrawListsGPU,
                    this.culledDrawListsGPU);
        }

        public final void setNonEmptyCommandMask(AbstractGPUCulledBaseInstanceRenderIndex<?, ?> parent, int tileIndex, byte newNonEmptyCommandMask) {
            assert (newNonEmptyCommandMask & ((1 << RENDER_PASS_COUNT) - 1)) == 0 : newNonEmptyCommandMask;

            //swap the actual mask values
            byte oldNonEmptyCommandMask = this.nonEmptyCommandMaskPerTile[tileIndex];
            this.nonEmptyCommandMaskPerTile[tileIndex] = newNonEmptyCommandMask;

            //adjust the per-pass non-empty command count
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                int deltaNonEmptyCommandCountThisPass = 0;
                deltaNonEmptyCommandCountThisPass -= (oldNonEmptyCommandMask >> pass) & 1;
                deltaNonEmptyCommandCountThisPass += (newNonEmptyCommandMask >> pass) & 1;
                this.nonEmptyCommandCountPerPass[pass] += deltaNonEmptyCommandCountThisPass;
            }

            //adjust the total non-empty command count
            int deltaNonEmptyCommandCountTotal = 0;
            deltaNonEmptyCommandCountTotal -= Integer.bitCount(oldNonEmptyCommandMask);
            deltaNonEmptyCommandCountTotal += Integer.bitCount(newNonEmptyCommandMask);
            this.nonEmptyCommandCountTotal += deltaNonEmptyCommandCountTotal;
            parent.nonEmptyCommandCount += deltaNonEmptyCommandCountTotal;

            //adjust the total non-empty tile count
            int deltaNonEmptyTileCountTotal = 0;
            deltaNonEmptyTileCountTotal -= oldNonEmptyCommandMask != 0 ? 1 : 0;
            deltaNonEmptyTileCountTotal += newNonEmptyCommandMask != 0 ? 1 : 0;
            this.nonEmptyTileCountTotal += deltaNonEmptyTileCountTotal;
            parent.nonEmptyTileCount += deltaNonEmptyTileCountTotal;
        }

        public final void capacityChanged(int newCapacityTiles) {
            int newCapacityCommands = Math.multiplyExact(newCapacityTiles, RENDER_PASS_COUNT);

            int oldCapacityTiles = this.capacityTiles;
            int oldCapacityCommands = oldCapacityTiles * RENDER_PASS_COUNT;
            assert oldCapacityCommands < newCapacityCommands : oldCapacityCommands + " >= " + newCapacityCommands;

            long oldCapacityBytes = (long) oldCapacityCommands * DrawElementsIndirectCommand._SIZE;
            long newCapacityBytes = (long) newCapacityCommands * DrawElementsIndirectCommand._SIZE;
            this.capacityTiles = newCapacityTiles;

            //mark the raw draw lists as dirty so that they get re-uploaded on the next render pass (the GPU-side buffer currently contains undefined data)
            this.rawDrawListsDirty = true;

            this.handleCapacityChanged(oldCapacityTiles, oldCapacityCommands, oldCapacityBytes, newCapacityTiles, newCapacityCommands, newCapacityBytes);
        }

        protected void handleCapacityChanged(int oldCapacityTiles, int oldCapacityCommands, long oldCapacityBytes, int newCapacityTiles, int newCapacityCommands, long newCapacityBytes) {
            //extend each of the rawDrawLists by the number of tiles added
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.rawDrawListsCPU.get(pass).appendZero(newCapacityTiles - oldCapacityTiles);
            }

            //extend the command counts array to the new capacity
            this.nonEmptyCommandMaskPerTile = Arrays.copyOf(this.nonEmptyCommandMaskPerTile, newCapacityTiles);

            //resize both of the GPU-side buffers to the new capacity.
            //  we can simply allocate new storage since all the data's going to get uploaded later anyway
            PResourceUtil.close(this.rawDrawListsGPU);
            this.rawDrawListsGPU = GLBuffer.createFunctionallyImmutable(this.gl, newCapacityBytes, BufferUsage.STREAM_DRAW, GL_MAP_WRITE_BIT);

            PResourceUtil.close(this.culledDrawListsGPU);
            this.culledDrawListsGPU = GLBuffer.createFunctionallyImmutable(this.gl, newCapacityBytes, BufferUsage.STREAM_COPY, 0);
        }

        public final void flushRawDrawLists() {
            if (this.capacityTiles == 0 || !this.rawDrawListsDirty) {
                return;
            }

            //orphan the GPU-side rawDrawLists and fill it with new data
            try (val mapping = this.rawDrawListsGPU.mapRange(BufferAccess.WRITE_ONLY, GL_MAP_INVALIDATE_BUFFER_BIT, 0L, this.rawDrawListsGPU.capacity())) {
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    //this technically won't work if the buffer capacity is greater than Integer.MAX_VALUE, but i don't care because there are never going to be 2 GiB command
                    //  lists anyway (if there are, something has almost certainly gone horribly wrong)
                    mapping.buffer.put(this.rawDrawListsCPU.get(pass).byteBufferView());
                }
                assert !mapping.buffer.hasRemaining();
            }

            this.rawDrawListsDirty = false;
        }
    }
}
