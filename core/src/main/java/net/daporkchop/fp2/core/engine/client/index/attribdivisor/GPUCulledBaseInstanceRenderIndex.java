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

package net.daporkchop.fp2.core.engine.client.index.attribdivisor;

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.AbstractRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndexType;
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
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.util.list.DirectDrawElementsIndirectCommandList;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Render index implementation using MultiDrawIndirect with an attribute divisor for the tile position which does frustum culling on tiles on the GPU.
 *
 * @author DaPorkchop_
 */
public class GPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractRenderIndex.WithTilePosAttrib<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = WithTilePosAttrib.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_multi_draw_indirect) //glMultiDrawElementsIndirect()
            .addAll(ComputeShaderProgram.REQUIRED_EXTENSIONS)
            .addAll(TerrainRenderingBlockedTracker.REQUIRED_EXTENSIONS_GPU)
            .add(GLExtension.GL_ARB_shader_storage_buffer_object)
            .add(GLExtension.GL_ARB_shader_image_load_store); //glMemoryBarrier()

    /*
     * Implementation overview:
     *
     * On the OpenGL side, we use three buffers:
     * - A buffer containing a list of all baked tile positions (undefined contents for unloaded tiles)
     * - rawDrawList, a buffer containing RENDER_PASS_COUNT indirect draw commands per tile position (zero for unloaded or hidden tiles, or when the tile
     *   has no data for the corresponding render pass). This is managed by the CPU, and is updated every time the tile data changes.
     * - culledDrawList, buffer containing RENDER_PASS_COUNT indirect draw commands per tile position. On each frame, we populate this based on rawDrawList
     *   (omitting commands when the corresponding tile position is outside the view frustum) and then use it as the source for indirect multidraw commands.
     *   It is never accessed directly by the CPU.
     *
     * On each frame, we do the following:
     * 1. Upload the tile positions and raw draw lists, if changed
     * 2. Invalidate culledDrawList
     * 3. Fill the culledDrawList buffer using a compute shader
     */

    private static final String TILE_POSITIONS_SSBO_NAME = "B_TilePositions"; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.glsl
    private static final int TILE_POSITIONS_SSBO_BINDING = 0;

    private static final String RAW_DRAW_LISTS_SSBO_NAME = "B_RawDrawLists"; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.glsl
    private static final int RAW_DRAW_LISTS_SSBO_BINDING = TILE_POSITIONS_SSBO_BINDING + 1;

    private static final String CULLED_DRAW_LISTS_SSBO_NAME = "B_CulledDrawLists"; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.glsl
    private static final int CULLED_DRAW_LISTS_SSBO_BINDING = RAW_DRAW_LISTS_SSBO_BINDING + 1;

    private static final String CULLING_SHADER_KEY = "indirect_tile_frustum_culling";
    private static final int CULLING_SHADER_WORK_GROUP_SIZE = 256; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.glsl

    public static void registerShaders(GlobalRenderer globalRenderer) {
        globalRenderer.shaderRegistry.createCompute(CULLING_SHADER_KEY, globalRenderer.shaderMacros, null)
                .addShader(ShaderType.COMPUTE, Identifier.from(FP2.MODID, "shaders/comp/indirect_tile_frustum_culling.comp"))
                .addUBO(CAMERA_STATE_UNIFORMS_UBO_BINDING, CAMERA_STATE_UNIFORMS_UBO_NAME)
                .addSSBO(TILE_POSITIONS_SSBO_BINDING, TILE_POSITIONS_SSBO_NAME)
                .addSSBO(RAW_DRAW_LISTS_SSBO_BINDING, RAW_DRAW_LISTS_SSBO_NAME)
                .addSSBO(CULLED_DRAW_LISTS_SSBO_BINDING, CULLED_DRAW_LISTS_SSBO_NAME)
                .addUBO(VANILLA_RENDERABILITY_UBO_BINDING, VANILLA_RENDERABILITY_UBO_NAME)
                .addSSBO(VANILLA_RENDERABILITY_SSBO_BINDING, VANILLA_RENDERABILITY_SSBO_NAME)
                .build();
    }

    /**
     * @author DaPorkchop_
     */
    public static final class Type extends RenderIndexType {
        public Type() {
            super(REQUIRED_EXTENSIONS);
        }

        @Override
        public boolean enabled(@NonNull FP2Config config) {
            return config.performance().gpuFrustumCulling();
        }

        @Override
        public <VertexType extends AttributeStruct> RenderIndex<VertexType> createRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
            return new GPUCulledBaseInstanceRenderIndex<>(gl, bakeStorage, alloc, globalRenderer, cameraStateUniformsBuffer);
        }
    }

    private final UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer;

    private final LevelArray<Level> levels;

    private final ReloadableShaderProgram<ComputeShaderProgram> cullingShader;

    public GPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer,
                                            UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
        super(gl, bakeStorage, alloc, globalRenderer);

        try {
            this.cameraStateUniformsBuffer = cameraStateUniformsBuffer;

            this.cullingShader = globalRenderer.shaderRegistry.get(CULLING_SHADER_KEY);
            //this.workGroupSize = this.cullingShader.get().workGroupSize().invocations();

            this.levels = new LevelArray<>(level -> new Level(gl, alloc));
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored = this.levels) {
            super.close();
        }
    }

    @Override
    protected RenderPosTable constructRenderPosTable(AttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        return new PerLevelRenderPosTable(level -> new SimpleRenderPosTable(this.gl, sharedVertexFormat, this.alloc, (oldCapacity, increment) -> {
            val newCapacity = Allocator.GrowFunction.sqrt2(CULLING_SHADER_WORK_GROUP_SIZE).grow(oldCapacity, increment);
            this.levels.get(level).capacityChanged(Math.toIntExact(newCapacity));
            return newCapacity;
        }));
    }

    @Override
    protected void recomputeTile(@NonNull TilePos pos) {
        Level levelInstance = this.levels.get(pos.level());
        BakeStorage.Location[] locations = this.bakeStorage.find(pos);
        if (locations == null || this.hiddenPositions.contains(pos)) {
            //the position doesn't have any render data data associated with it or is hidden, and therefore can be entirely omitted from the index
            int baseInstance = this.renderPosTable.remove(pos);

            if (baseInstance >= 0) {
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    levelInstance.rawDrawListsCPU.get(pass).setZero(baseInstance);
                }
                levelInstance.rawDrawListsDirty = true;
            }
        } else {
            //configure the render commands which will be used when selecting this
            int baseInstance = this.renderPosTable.add(pos);

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
                } else {
                    //otherwise, the draw command will be filled with zeroes
                }
                levelInstance.rawDrawListsCPU.get(pass).set(baseInstance, command);
            }
            levelInstance.rawDrawListsDirty = true;
        }
    }

    @Override
    public void preservedSelectState(StatePreserver.Builder builder) {
        super.preservedSelectState(builder
                .activeProgram()
                .indexedBuffer(IndexedBufferTarget.UNIFORM_BUFFER, CAMERA_STATE_UNIFORMS_UBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, RAW_DRAW_LISTS_SSBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, CULLED_DRAW_LISTS_SSBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.UNIFORM_BUFFER, VANILLA_RENDERABILITY_UBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, VANILLA_RENDERABILITY_SSBO_BINDING));
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        this.renderPosTable.flush();

        //bind camera state uniforms
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());

        //bind terrain rendering blocked tracker, so that level-0 tiles can be skipped if they overlap with vanilla terrain
        blockedTracker.bindGlBuffers(this.gl, VANILLA_RENDERABILITY_UBO_BINDING, VANILLA_RENDERABILITY_SSBO_BINDING);

        val cullingShaderProgram = this.cullingShader.get();
        val uniformSetter = cullingShaderProgram.bindUnsafe(); // active program binding will be restored by StatePreserver

        //configure frustum uniforms
        frustum.configureClippingPlanes(uniformSetter, new IFrustum.UniformLocations(cullingShaderProgram));

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);
            val tilePosArray = this.renderPosTable.vertexBuffer(level);
            int capacity = tilePosArray.capacity(); // TODO: maybe use some other mechanism to decide on this rather than capacity (as capacity can't shrink back down)
            assert (capacity % CULLING_SHADER_WORK_GROUP_SIZE) == 0 : "capacity not divisible by work group size! level=" + level + ", capacity=" + capacity + ", work group size=" + CULLING_SHADER_WORK_GROUP_SIZE;

            if (capacity == 0) {
                //skip empty detail levels
                continue;
            }

            levelInstance.flushRawDrawLists();
            levelInstance.orphanCulledDrawLists();

            //dispatch the compute shader

            //bind the tile positions array and the rawDrawLists+culledDrawLists for each render pass
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING, tilePosArray.bufferSSBO().id());
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, RAW_DRAW_LISTS_SSBO_BINDING, levelInstance.rawDrawListsGPU.id());
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, CULLED_DRAW_LISTS_SSBO_BINDING, levelInstance.culledDrawListsGPU.id());

            //cull the tiles!
            this.gl.glDispatchCompute(capacity / CULLING_SHADER_WORK_GROUP_SIZE, 1, 1);
        }
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder
                .vao()
                .buffer(BufferTarget.DRAW_INDIRECT_BUFFER));
    }

    @Override
    public void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        val levelInstance = this.levels.get(level);
        if (levelInstance.capacityTiles != 0) {
            this.gl.glBindVertexArray(this.vaos.get(level, pass).id());
            this.gl.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, levelInstance.culledDrawListsGPU.id());
            this.gl.glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
            this.gl.glMultiDrawElementsIndirect(
                    mode.mode(),
                    this.bakeStorage.indexFormat.type().type(),
                    (long) pass * levelInstance.capacityTiles * DrawElementsIndirectCommand._SIZE,
                    levelInstance.capacityTiles,
                    0);
        }
    }

    @Override
    public PosTechnique posTechnique() {
        return PosTechnique.VERTEX_ATTRIBUTE;
    }

    @Override
    public DebugStats.Renderer stats() {
        return DebugStats.Renderer.builder()
                // TODO: some way of feeding this data back from the GPU
                .selectedTiles(-1L)
                .indexedTiles(-1L)
                .build();
    }

    /**
     * Represents the render state at a single detail level.
     *
     * @author DaPorkchop_
     */
    private static final class Level implements AutoCloseable {
        final PassArray<DirectDrawElementsIndirectCommandList> rawDrawListsCPU;
        final GLMutableBuffer rawDrawListsGPU;
        final GLMutableBuffer culledDrawListsGPU;
        int capacityTiles;

        boolean rawDrawListsDirty;

        public Level(OpenGL gl, DirectMemoryAllocator alloc) {
            try {
                this.rawDrawListsCPU = new PassArray<>(pass -> new DirectDrawElementsIndirectCommandList(alloc));

                this.rawDrawListsGPU = GLMutableBuffer.create(gl);
                this.culledDrawListsGPU = GLMutableBuffer.create(gl);
            } catch (Throwable t) {
                throw PResourceUtil.closeSuppressed(t, this);
            }
        }

        public void capacityChanged(int newCapacityTiles) {
            int newCapacityCommands = Math.multiplyExact(newCapacityTiles, RENDER_PASS_COUNT);

            int oldCapacityTiles = this.capacityTiles;
            int oldCapacityCommands = oldCapacityTiles * RENDER_PASS_COUNT;
            checkState(newCapacityCommands > oldCapacityCommands, "newCapacityTiles=%s, oldCapacityTiles=%s", newCapacityTiles, oldCapacityTiles);
            this.capacityTiles = newCapacityTiles;

            //extend each of the rawDrawLists by the number of tiles added
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.rawDrawListsCPU.get(pass).appendZero(newCapacityTiles - oldCapacityTiles);
            }

            //resize both of the GPU-side buffers to RENDER_PASS_COUNT times the number of tiles added
            this.rawDrawListsGPU.capacity((long) newCapacityCommands * DrawElementsIndirectCommand._SIZE, BufferUsage.STREAM_DRAW);
            this.culledDrawListsGPU.capacity((long) newCapacityCommands * DrawElementsIndirectCommand._SIZE, BufferUsage.STREAM_COPY);

            //mark the raw draw lists as dirty so that they get re-uploaded on the next render pass (the GPU-side buffer currently contains undefined data)
            this.rawDrawListsDirty = true;
        }

        public void flushRawDrawLists() {
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

        public void orphanCulledDrawLists() {
            if (this.capacityTiles == 0) {
                return;
            }

            //orphan the GPU-side rawDrawLists
            this.culledDrawListsGPU.invalidateHint();
        }

        @Override
        public void close() {
            PResourceUtil.closeAll(
                    this.rawDrawListsCPU,
                    this.rawDrawListsGPU,
                    this.culledDrawListsGPU);
        }
    }
}
