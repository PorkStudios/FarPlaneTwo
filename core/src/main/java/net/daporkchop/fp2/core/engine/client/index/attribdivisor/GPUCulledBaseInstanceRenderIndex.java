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

import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.client.shader.ShaderRegistration;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndexType;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.MultiBindHelper;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Render index implementation using MultiDrawIndirect with an attribute divisor for the tile position which does frustum culling on tiles on the GPU.
 *
 * @author DaPorkchop_
 */
public final class GPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractGPUCulledBaseInstanceRenderIndex<VertexType, AbstractGPUCulledBaseInstanceRenderIndex.Level> {
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

    private static final String TILE_POSITIONS_SSBO_NAME = "B_TilePositions"; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.comp
    private static final int TILE_POSITIONS_SSBO_BINDING = 0;

    private static final String RAW_DRAW_LISTS_SSBO_NAME = "B_RawDrawLists"; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.comp
    private static final int RAW_DRAW_LISTS_SSBO_BINDING = TILE_POSITIONS_SSBO_BINDING + 1;

    private static final String CULLED_DRAW_LISTS_SSBO_NAME = "B_CulledDrawLists"; //synced with resources/assets/fp2/shaders/comp/indirect_tile_frustum_culling.comp
    private static final int CULLED_DRAW_LISTS_SSBO_BINDING = RAW_DRAW_LISTS_SSBO_BINDING + 1;

    private static final int COUNT_SELECTED_COUNTER_BINDING = 0;

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static final class CullingShaderVariant {
        final boolean countSelected;
        final boolean indirectCount;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_COUNT_SELECTED", this.countSelected);
            builder.put("FP2_INDIRECT_COUNT", this.indirectCount);
            if (this.countSelected) {
                builder.put("COUNT_SELECTED_COUNTER_BINDING", COUNT_SELECTED_COUNTER_BINDING);
            }
            return builder.build();
        }

        public static List<CullingShaderVariant> allVariants(@NonNull OpenGL gl) {
            List<CullingShaderVariant> result = new ArrayList<>(3);
            result.add(new CullingShaderVariant(false, false));
            if (gl.supports(REQUIRED_EXTENSIONS_COUNT_SELECTED)) {
                result.add(new CullingShaderVariant(true, false));
                if (gl.supports(REQUIRED_EXTENSIONS_INDIRECT_COUNT)) {
                    result.add(new CullingShaderVariant(true, true));
                }
            }
            return result;
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static final class RegisterShaders extends ShaderRegistration {
        public RegisterShaders() {
            super(REQUIRED_EXTENSIONS);
        }

        @Override
        public void registerShaders(@NonNull GlobalRenderer globalRenderer, @NonNull ReloadableShaderRegistry shaderRegistry, @NonNull ShaderMacros shaderMacros, @NonNull FP2Client client, @NonNull OpenGL gl) {
            for (CullingShaderVariant variant : CullingShaderVariant.allVariants(gl)) {
                shaderRegistry.createCompute(variant, shaderMacros.withDefined(variant.defines()), null)
                        .addShader(ShaderType.COMPUTE, Identifier.from(FP2.MODID, "shaders/comp/indirect_tile_frustum_culling.comp"))
                        .addUBO(CAMERA_STATE_UNIFORMS_UBO_BINDING, CAMERA_STATE_UNIFORMS_UBO_NAME)
                        .addSSBO(TILE_POSITIONS_SSBO_BINDING, TILE_POSITIONS_SSBO_NAME)
                        .addSSBO(RAW_DRAW_LISTS_SSBO_BINDING, RAW_DRAW_LISTS_SSBO_NAME)
                        .addSSBO(CULLED_DRAW_LISTS_SSBO_BINDING, CULLED_DRAW_LISTS_SSBO_NAME)
                        .addUBO(VANILLA_RENDERABILITY_UBO_BINDING, VANILLA_RENDERABILITY_UBO_NAME)
                        .addSSBO(VANILLA_RENDERABILITY_SSBO_BINDING, VANILLA_RENDERABILITY_SSBO_NAME)
                        .build();
            }
        }
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

    private final ReloadableShaderProgram<ComputeShaderProgram> cullingShader;

    private final MultiBindHelper bindHelper_positions_raw_culled;

    public GPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
        super(gl.checkSupported(REQUIRED_EXTENSIONS), bakeStorage, alloc, globalRenderer, cameraStateUniformsBuffer);

        try {
            this.cullingShader = globalRenderer.shaderRegistry.get(new CullingShaderVariant(this.countSelectedBuffer != null, this.useIndirectCount));

            this.bindHelper_positions_raw_culled = MultiBindHelper.builder(gl)
                    .bindBufferBase(IndexedBufferTarget.SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING)
                    .bindBufferBase(IndexedBufferTarget.SHADER_STORAGE_BUFFER, RAW_DRAW_LISTS_SSBO_BINDING)
                    .bindBufferBase(IndexedBufferTarget.SHADER_STORAGE_BUFFER, CULLED_DRAW_LISTS_SSBO_BINDING)
                    .build();
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored = this.bindHelper_positions_raw_culled) {
            super.close();
        }
    }

    @Override
    protected Level createLevel(int levelIndex) {
        return new Level(this.gl, this.alloc);
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

        if (this.countSelectedBuffer != null) { //if selected tile counting is supported, we need to preserve the atomic counter binding
            builder.indexedBuffer(IndexedBufferTarget.ATOMIC_COUNTER_BUFFER, COUNT_SELECTED_COUNTER_BINDING);
        }
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        this.renderPosTable.flush();

        //bind camera state uniforms
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());

        //bind terrain rendering blocked tracker, so that level-0 tiles can be skipped if they overlap with vanilla terrain
        blockedTracker.bindGlBuffers(this.gl, VANILLA_RENDERABILITY_UBO_BINDING, VANILLA_RENDERABILITY_SSBO_BINDING);

        if (this.countSelectedBuffer != null) { //if selected tile counting is supported, reset all the counters to 0
            this.countSelectedBuffer.clearBufferDataZero();
        }

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
            levelInstance.culledDrawListsGPU.invalidateHint();

            if (this.countSelectedBuffer != null) { //if we're using MultiDraw with indirect counts, bind the atomic counter
                this.gl.glBindBufferRange(GL_ATOMIC_COUNTER_BUFFER, COUNT_SELECTED_COUNTER_BINDING, this.countSelectedBuffer.id(), level * COUNT_SELECTED_BUFFER_LEVEL_STRIDE, COUNT_SELECTED_BUFFER_LEVEL_STRIDE);
            }

            //bind the tile positions array and the rawDrawLists+culledDrawLists for each render pass
            this.bindHelper_positions_raw_culled.dispatcher().invokeExact(
                    this.gl,
                    tilePosArray.bufferSSBO().id(),
                    levelInstance.rawDrawListsGPU.id(),
                    levelInstance.culledDrawListsGPU.id());

            //cull the tiles!
            this.gl.glDispatchCompute(capacity / CULLING_SHADER_WORK_GROUP_SIZE, 1, 1);
        }

        //maybe begin downloading the debug statistics data
        this.enqueueDebugStatisticsUpdate(true);
    }
}
