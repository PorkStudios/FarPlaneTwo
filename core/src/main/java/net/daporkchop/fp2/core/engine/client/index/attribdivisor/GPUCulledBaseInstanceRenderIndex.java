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

import lombok.val;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.GlobalUniformAttributes;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.postable.PerLevelRenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.RenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.SimpleRenderPosTable;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewUniformBuffer;
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
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.primitive.lambda.IntIntObjFunction;

import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Render index implementation using MultiDrawIndirect with an attribute divisor for the tile position which does frustum culling on tiles on the GPU.
 *
 * @author DaPorkchop_
 */
public class GPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractMultiDrawIndirectRenderIndex<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = AbstractMultiDrawIndirectRenderIndex.REQUIRED_EXTENSIONS
            .addAll(ComputeShaderProgram.REQUIRED_EXTENSIONS)
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

    private static final String CULLING_SHADER_KEY = "tile_frustum_culling";

    public static void registerShaders(GlobalRenderer globalRenderer) {
        globalRenderer.shaderRegistry.createCompute(CULLING_SHADER_KEY, globalRenderer.shaderMacros, null)
                .addShader(ShaderType.COMPUTE, Identifier.from(FP2.MODID, "shaders/comp/indirect_tile_frustum_culling.comp"))
                .addUBO(GLOBAL_UNIFORMS_UBO_BINDING, GLOBAL_UNIFORMS_UBO_NAME)
                .addSSBO(TILE_POSITIONS_SSBO_BINDING, TILE_POSITIONS_SSBO_NAME)
                .addSSBOs(INDIRECT_DRAWS_SSBO_FIRST_BINDING, RENDER_PASS_COUNT, INDIRECT_DRAWS_SSBO_NAME)
                .build();
    }

    private final NewUniformBuffer<GlobalUniformAttributes> globalUniformBuffer;

    private final LevelPassArray<DirectCommandList> rawDrawListsCPU;
    private final LevelPassArray<GLMutableBuffer> rawDrawListsGPU;
    private final LevelPassArray<GLMutableBuffer> culledDrawListsGPU;

    private final ReloadableShaderProgram<ComputeShaderProgram> cullingShader;
    private final int tilesCulledPerWorkGroup = 16 * 16; //TODO: a nicer way to acquire this without hardcoding it

    public GPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat, GlobalRenderer globalRenderer,
                                            NewUniformBuffer<GlobalUniformAttributes> globalUniformBuffer) {
        super(gl, bakeStorage, alloc, sharedVertexFormat);

        try {
            this.globalUniformBuffer = globalUniformBuffer;

            this.cullingShader = globalRenderer.shaderRegistry.get(CULLING_SHADER_KEY);
            //this.tilesCulledPerWorkGroup = this.cullingShader.get().workGroupSize().invocations();

            this.rawDrawListsCPU = new LevelPassArray<>((level, pass) -> new DirectCommandList(alloc));

            IntIntObjFunction<GLMutableBuffer> drawListCreatorGPU = (level, pass) -> GLMutableBuffer.create(gl);
            this.rawDrawListsGPU = new LevelPassArray<>(drawListCreatorGPU);
            this.culledDrawListsGPU = new LevelPassArray<>(drawListCreatorGPU);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored0 = this.rawDrawListsCPU;
             val ignored1 = this.rawDrawListsGPU;
             val ignored2 = this.culledDrawListsGPU) {
            super.close();
        }
    }

    @Override
    protected RenderPosTable constructRenderPosTable(NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        val step = this.tilesCulledPerWorkGroup; //TODO: this will always be uninitialized if i stop hardcoding the value
        val growFunction = Allocator.GrowFunction.sqrt2(step);
        return new PerLevelRenderPosTable(level -> new SimpleRenderPosTable(this.gl, sharedVertexFormat, this.alloc, growFunction));
    }

    private Consumer<TilePos> recomputeTile() {
        return pos -> {
            BakeStorage.Location[] locations = this.bakeStorage.find(pos);
            if (locations == null || this.hiddenPositions.contains(pos)) {
                //the position doesn't have any render data data associated with it or is hidden, and therefore can be entirely omitted from the index
                int baseInstance = this.renderPosTable.remove(pos);

                if (baseInstance >= 0) {
                    for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                        val rawDrawListCPU = this.rawDrawListsCPU.get(pos.level(), pass);
                        if (rawDrawListCPU.size > baseInstance) {
                            rawDrawListCPU.set(baseInstance, new DrawElementsIndirectCommand());
                        }
                    }
                }
            } else {
                //configure the render commands which will be used when selecting this
                int baseInstance = this.renderPosTable.add(pos);

                DrawElementsIndirectCommand[] commands = new DrawElementsIndirectCommand[RENDER_PASS_COUNT];
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    BakeStorage.Location location = locations[pass];
                    if (location != null) {
                        val command = commands[pass] = new DrawElementsIndirectCommand();
                        command.count = location.count;
                        command.baseInstance = baseInstance;
                        command.firstIndex = location.firstIndex;
                        command.baseVertex = location.baseVertex;
                        command.instanceCount = 1;
                    }
                }

                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    val rawDrawListCPU = this.rawDrawListsCPU.get(pos.level(), pass);
                    // grow the cpu list
                    // TODO: this should be done automatically when the RenderPosTable is resized
                    while (rawDrawListCPU.size < PMath.roundUp(baseInstance + 1, this.tilesCulledPerWorkGroup)) {
                        rawDrawListCPU.add(new DrawElementsIndirectCommand());
                    }
                    rawDrawListCPU.set(baseInstance, commands[pass] == null ? new DrawElementsIndirectCommand() : commands[pass]);
                }
            }
        };
    }

    @Override
    public void notifyTilesChanged(Set<TilePos> changedPositions) {
        changedPositions.forEach(this.recomputeTile());
    }

    @Override
    public void updateHidden(Set<TilePos> hidden, Set<TilePos> shown) {
        super.updateHidden(hidden, shown);

        //recompute the draw commands for all affected tiles, so that all tiles which got hidden get excluded from the index
        hidden.forEach(this.recomputeTile());
        shown.forEach(this.recomputeTile());
    }

    @Override
    public void preservedSelectState(StatePreserver.Builder builder) {
        super.preservedSelectState(builder
                .activeProgram()
                .indexedBuffer(IndexedBufferTarget.UNIFORM_BUFFER, GLOBAL_UNIFORMS_UBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING)
                .indexedBuffers(IndexedBufferTarget.SHADER_STORAGE_BUFFER, INDIRECT_DRAWS_SSBO_FIRST_BINDING, RENDER_PASS_COUNT));
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        this.renderPosTable.flush();

        //upload the rawDrawList at each detail level
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            int capacity = this.renderPosTable.vertexBuffer(level).capacity(); // TODO: maybe use some other mechanism to decide on this rather than capacity (as capacity can't shrink back down)
            assert (capacity % this.tilesCulledPerWorkGroup) == 0 : "capacity not divisible by work group size! level=" + level + ", capacity=" + capacity + ", work group size=" + this.tilesCulledPerWorkGroup;

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                val rawDrawListCPU = this.rawDrawListsCPU.get(level, pass);
                val rawDrawListGPU = this.rawDrawListsGPU.get(level, pass);
                rawDrawListGPU.capacity(capacity * DrawElementsIndirectCommand._SIZE, BufferUsage.STREAM_DRAW);
                rawDrawListGPU.bufferSubData(0L, rawDrawListCPU.byteBufferView());
            }
        }

        //orphan the culledDrawList at each detail level
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            int capacity = this.renderPosTable.vertexBuffer(level).capacity();

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.culledDrawListsGPU.get(level, pass).capacity(capacity * DrawElementsIndirectCommand._SIZE, BufferUsage.STREAM_COPY);
            }
        }

        //copy the rawDrawList into the culledDrawList
        //TODO: i'd like to avoid this copy and instead simply bind both lists to the shader
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            int capacity = this.renderPosTable.vertexBuffer(level).capacity();

            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.culledDrawListsGPU.get(level, pass).copyRange(this.rawDrawListsGPU.get(level, pass), 0L, 0L, capacity * DrawElementsIndirectCommand._SIZE);
            }
        }

        //bind global uniforms
        // TODO: This will still contain the uniforms from the previous frame. Since we only use it to get the camera position into the shader, maybe we could
        //       add a different mechanism to get the live camera position before we know the full render uniform state?
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, GLOBAL_UNIFORMS_UBO_BINDING, this.globalUniformBuffer.buffer().id());

        //TODO: i also need to bind the terrain rendering blocked tracker

        val cullingShaderProgram = this.cullingShader.get();
        val uniformSetter = cullingShaderProgram.bindUnsafe(); // active program binding will be restored by StatePreserver

        //configure frustum uniforms
        frustum.configureClippingPlanes(uniformSetter, new IFrustum.UniformLocations(cullingShaderProgram));

        //dispatch the compute shaders
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            NewAttributeBuffer<VoxelGlobalAttributes> tilePosArray = this.renderPosTable.vertexBuffer(level);
            int capacity = tilePosArray.capacity();

            //bind the tile positions array and the culledDrawList for each render pass
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING, tilePosArray.bufferSSBO().id());
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, INDIRECT_DRAWS_SSBO_FIRST_BINDING + pass, this.culledDrawListsGPU.get(level, pass).id());
            }

            //cull the tiles!
            this.gl.glDispatchCompute(capacity / this.tilesCulledPerWorkGroup, 1, 1);
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
        val list = this.rawDrawListsCPU.get(level, pass);
        if (list.size != 0) {
            this.gl.glBindVertexArray(this.vaos.get(level, pass).id());
            this.gl.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.culledDrawListsGPU.get(level, pass).id());
            this.gl.glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
            this.gl.glMultiDrawElementsIndirect(mode.mode(), this.bakeStorage.indexFormat.type().type(), 0L, list.size, 0);
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
}
