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
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.listener.FramebufferResizeListener;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.compute.ComputeIndirectDrawCommandsCompressor;
import net.daporkchop.fp2.core.client.render.compute.ComputeTextureCopier;
import net.daporkchop.fp2.core.client.render.compute.ComputeTextureMipmapGenerator;
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
import net.daporkchop.fp2.core.util.listener.ListenerList;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayObject;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.VertexMode;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.texture.GLTexture2D;
import net.daporkchop.fp2.gl.texture.TextureMagFilter;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.fp2.gl.texture.TextureMinFilter;
import net.daporkchop.fp2.gl.texture.framebuffer.FramebufferAttachment;
import net.daporkchop.fp2.gl.texture.framebuffer.GLFramebuffer;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public final class GPUOcclusionCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractGPUCulledBaseInstanceRenderIndex<VertexType, GPUOcclusionCulledBaseInstanceRenderIndex.Level> implements FramebufferResizeListener {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = AbstractGPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS_INDIRECT_COUNT
            .addAll(ComputeTextureMipmapGenerator.REQUIRED_EXTENSIONS)
            .add(GLExtension.GL_ARB_shader_image_load_store); //early fragment tests

    private static final String TILE_POSITIONS_SSBO_NAME = "B_TilePositions"; //synced with resources/assets/fp2/shaders/vert/occlusion_culled_cube.vert
    private static final int TILE_POSITIONS_SSBO_BINDING = 0;

    private static final String DST_SELECTED_TILES_SSBO_NAME = "B_DstSelectedTiles"; //synced with resources/assets/fp2/shaders/frag/occlusion_culled_cube.frag
    private static final int DST_SELECTED_TILES_SSBO_BINDING = TILE_POSITIONS_SSBO_BINDING + 1;

    private static final String SRC_VISIBLE_TILES_SSBO_NAME = "B_SrcVisibleTiles"; //synced with resources/assets/fp2/shaders/frag/occlusion_culled_cube.frag
    private static final int SRC_VISIBLE_TILES_SSBO_BINDING = DST_SELECTED_TILES_SSBO_BINDING + 1;

    private static final String DEPTH_TEXTURE_SAMPLER2D_NAME = "u_depthTexture";
    private static final int DEPTH_TEXTURE_SAMPLER2D_BINDING = 6;

    private static final Object OCCLUSION_CULLED_CUBE_KEY = "occlusion_culled_cube";
    private static final Object TILE_VISIBILITY_TEST_KEY = "tile_visibility_test";

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static final class TileOcclusionTestVariant {
        final boolean reverseZEnabled;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_REVERSEZ_ENABLED", this.reverseZEnabled);
            return builder.build();
        }

        public static List<TileOcclusionTestVariant> allVariants(@NonNull OpenGL gl) {
            List<TileOcclusionTestVariant> result = new ArrayList<>(2);
            result.add(new TileOcclusionTestVariant(false));
            result.add(new TileOcclusionTestVariant(true));
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
            shaderRegistry.createDraw(OCCLUSION_CULLED_CUBE_KEY, shaderMacros, null)
                    .addShader(ShaderType.VERTEX, Identifier.from(FP2.MODID, "shaders/vert/occlusion_culled_cube.vert"))
                    .addShader(ShaderType.FRAGMENT, Identifier.from(FP2.MODID, "shaders/frag/occlusion_culled_cube.frag"))
                    .addUBO(CAMERA_STATE_UNIFORMS_UBO_BINDING, CAMERA_STATE_UNIFORMS_UBO_NAME)
                    .addSSBO(TILE_POSITIONS_SSBO_BINDING, TILE_POSITIONS_SSBO_NAME)
                    .addSSBO(DST_SELECTED_TILES_SSBO_BINDING, DST_SELECTED_TILES_SSBO_NAME)
                    .build();

            shaderRegistry.createCompute(TILE_VISIBILITY_TEST_KEY, shaderMacros, null)
                    .addShader(ShaderType.COMPUTE, Identifier.from(FP2.MODID, "shaders/comp/tile_visibility_test.comp"))
                    .addUBO(CAMERA_STATE_UNIFORMS_UBO_BINDING, CAMERA_STATE_UNIFORMS_UBO_NAME)
                    .addSSBO(TILE_POSITIONS_SSBO_BINDING, TILE_POSITIONS_SSBO_NAME)
                    .addSSBO(DST_SELECTED_TILES_SSBO_BINDING, DST_SELECTED_TILES_SSBO_NAME)
                    .addUBO(VANILLA_RENDERABILITY_UBO_BINDING, VANILLA_RENDERABILITY_UBO_NAME)
                    .addSSBO(VANILLA_RENDERABILITY_SSBO_BINDING, VANILLA_RENDERABILITY_SSBO_NAME)
                    .build();

            for (val variant : TileOcclusionTestVariant.allVariants(gl)) {
                shaderRegistry.createCompute(variant, shaderMacros.withDefined(variant.defines()), null)
                        .addShader(ShaderType.COMPUTE, Identifier.from(FP2.MODID, "shaders/comp/tile_occlusion_test.comp"))
                        .addSampler(DEPTH_TEXTURE_SAMPLER2D_BINDING, DEPTH_TEXTURE_SAMPLER2D_NAME)
                        .addUBO(CAMERA_STATE_UNIFORMS_UBO_BINDING, CAMERA_STATE_UNIFORMS_UBO_NAME)
                        .addSSBO(TILE_POSITIONS_SSBO_BINDING, TILE_POSITIONS_SSBO_NAME)
                        .addSSBO(SRC_VISIBLE_TILES_SSBO_BINDING, SRC_VISIBLE_TILES_SSBO_NAME)
                        .addSSBO(DST_SELECTED_TILES_SSBO_BINDING, DST_SELECTED_TILES_SSBO_NAME)
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
            return new GPUOcclusionCulledBaseInstanceRenderIndex<>(FP2Core.fp2().client(), gl, bakeStorage, alloc, globalRenderer, cameraStateUniformsBuffer);
        }
    }

    private final ReloadableShaderRegistry shaderRegistry;

    private final ReloadableShaderProgram<DrawShaderProgram> occlusionCulledCubeShader;
    private final ReloadableShaderProgram<ComputeShaderProgram> tileVisibilityTestShader;

    private final ComputeTextureCopier textureCopier;
    private final ComputeTextureMipmapGenerator mipmapGenerator;
    private final ComputeIndirectDrawCommandsCompressor drawCommandsCompressor;

    private GLTexture2D depthTexture_depth;
    private GLTexture2D depthTexture_color;
    private final GLFramebuffer copyFramebuffer;

    private final VertexArrayObject cubeVao;
    private final GLBuffer cubeIndexBuffer;
    private final int cubeMeshVertices;
    private final IndexType cubeMeshIndexType;

    private final ListenerList<FramebufferResizeListener>.Handle framebufferResizeListenerHandle;

    public GPUOcclusionCulledBaseInstanceRenderIndex(FP2Client client, OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
        super(gl.checkSupported(REQUIRED_EXTENSIONS), bakeStorage, alloc, globalRenderer, cameraStateUniformsBuffer);

        try {
            this.shaderRegistry = globalRenderer.shaderRegistry;

            this.occlusionCulledCubeShader = globalRenderer.shaderRegistry.get(OCCLUSION_CULLED_CUBE_KEY);
            this.tileVisibilityTestShader = globalRenderer.shaderRegistry.get(TILE_VISIBILITY_TEST_KEY);

            this.textureCopier = new ComputeTextureCopier(gl, globalRenderer);
            this.mipmapGenerator = new ComputeTextureMipmapGenerator(gl, globalRenderer);
            this.drawCommandsCompressor = new ComputeIndirectDrawCommandsCompressor(gl, globalRenderer, VertexMode.INDICES);

            this.copyFramebuffer = GLFramebuffer.create(gl);

            this.cubeIndexBuffer = GLBuffer.createFunctionallyImmutable(gl, (ByteBuffer) ByteBuffer.allocateDirect(3 * 2 * 6)
                    .put(new byte[]{
                            //-X
                            0b000, 0b010, 0b110,
                            0b000, 0b110, 0b100,
                            //+X
                            0b001, 0b111, 0b011,
                            0b001, 0b101, 0b111,
                            //-Y
                            0b000, 0b101, 0b001,
                            0b000, 0b100, 0b101,
                            //+Y
                            0b010, 0b011, 0b111,
                            0b010, 0b111, 0b110,
                            //-Z
                            0b000, 0b001, 0b011,
                            0b000, 0b011, 0b010,
                            //+Z
                            0b100, 0b111, 0b101,
                            0b100, 0b110, 0b111,
                    }).flip(), BufferUsage.STATIC_DRAW, 0);
            this.cubeVao = VertexArrayObject.builder(gl).elementBuffer(this.cubeIndexBuffer).build();

            this.cubeMeshVertices = (int) this.cubeIndexBuffer.capacity();
            this.cubeMeshIndexType = IndexType.UNSIGNED_BYTE;

            this.framebufferResizeListenerHandle = client.addFramebufferResizeListener(this);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored = PResourceUtil.lazyCloseAll(
                this.framebufferResizeListenerHandle,
                this.cubeVao,
                this.cubeIndexBuffer,
                this.textureCopier,
                this.mipmapGenerator,
                this.drawCommandsCompressor,
                this.copyFramebuffer,
                this.depthTexture_depth,
                this.depthTexture_color)) {
            super.close();
        }
    }

    @Override
    protected Level createLevel(int levelIndex) {
        return new Level(this.gl, this.alloc);
    }

    @Override
    public void onFramebufferResize(int width, int height) {
        PResourceUtil.closeAll(this.depthTexture_depth, this.depthTexture_color);

        //allocate a new texture for the depth values
        this.depthTexture_depth = GLTexture2D.create(this.gl, TextureInternalFormat.DEPTH_COMPONENT_32F, 1, width, height);
        this.depthTexture_color = GLTexture2D.create(this.gl, TextureInternalFormat.R32F, GLTexture2D.requiredLevels(width, height), width, height);

        this.depthTexture_color.filter(TextureMinFilter.NEAREST_MIPMAP_NEAREST, TextureMagFilter.NEAREST);
        this.depthTexture_color.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.depthTexture_color.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        //attach the new depth texture to the framebuffer
        this.copyFramebuffer.attachTexture(FramebufferAttachment.DEPTH_ATTACHMENT, this.depthTexture_depth, 0);
        this.copyFramebuffer.checkComplete();
    }

    @Override
    public void preservedSelectState(StatePreserver.Builder builder) {
        super.preservedSelectState(builder);

        builder.activeProgram();
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        this.renderPosTable.flush();

        //bind camera state uniforms
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());

        //bind terrain rendering blocked tracker, so that level-0 tiles can be skipped if they overlap with vanilla terrain
        blockedTracker.bindGlBuffers(this.gl, VANILLA_RENDERABILITY_UBO_BINDING, VANILLA_RENDERABILITY_SSBO_BINDING);

        val cullingShaderProgram = this.tileVisibilityTestShader.get();
        val uniformSetter = cullingShaderProgram.bindUnsafe(); // active program binding will be restored by StatePreserver

        //configure frustum uniforms
        frustum.configureClippingPlanes(uniformSetter, new IFrustum.UniformLocations(cullingShaderProgram));

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);
            if (levelInstance.capacityTiles == 0) {
                //skip empty tiles
                continue;
            }

            levelInstance.flushRawDrawLists();
            levelInstance.selectedMask_visibility.invalidateHint();

            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING, this.renderPosTable.vertexBuffer(level).bufferSSBO().id());
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, DST_SELECTED_TILES_SSBO_BINDING, levelInstance.selectedMask_visibility.id());

            //cull the tiles!
            this.gl.glDispatchCompute(levelInstance.capacityTiles / CULLING_SHADER_WORK_GROUP_SIZE, 1, 1);
        }
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder);

        this.textureCopier.configureModifiedState(builder);
        this.mipmapGenerator.configureModifiedState(builder);
        this.drawCommandsCompressor.configureModifiedState(builder);

        builder.fixedFunctionDrawState()
                .activeProgram()
                .vao();
    }

    @Override
    public void preDraw() {
        super.preDraw();

        val reversedZ = FP2Core.fp2().client().renderManager().reversedZ();
        boolean reversedZEnabled = reversedZ != null && reversedZ.isActive();

        //TODO: this needs to run after we've rendered all the tiles from the previous frame
        int oldDrawFramebuffer = this.gl.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int oldReadFramebuffer = this.gl.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        try {
            this.gl.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.copyFramebuffer.id());
            this.gl.glBindFramebuffer(GL_READ_FRAMEBUFFER, oldDrawFramebuffer);

            //blit the active depth buffer into the depth texture
            this.gl.glBlitFramebuffer(
                    //oldDrawFramebuffer, this.copyFramebuffer.id(),
                    0, 0, this.depthTexture_depth.width(), this.depthTexture_depth.height(),
                    0, 0, this.depthTexture_depth.width(), this.depthTexture_depth.height(),
                    GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        } finally {
            this.gl.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawFramebuffer);
            this.gl.glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadFramebuffer);
        }

        //copy the depth buffer (again!!) into a texture with a color format so that we can generate mipmaps from it
        this.textureCopier.copyTextureLevel(
                this.depthTexture_depth, 0,
                this.depthTexture_color, 0);

        //generate mipmaps
        this.mipmapGenerator.generateMipmaps(
                this.depthTexture_color, 0,
                this.depthTexture_color, 1,
                this.depthTexture_color.levels() - 1,
                reversedZEnabled ? ComputeTextureMipmapGenerator.MipmapMode.MIN : ComputeTextureMipmapGenerator.MipmapMode.MAX);

        //for now we will perform just a single occlusion culling pass
        val tileOcclusionTestProgram = this.shaderRegistry.get(new TileOcclusionTestVariant(reversedZEnabled)).get();
        tileOcclusionTestProgram.bindUnsafe(); //active program binding will be restored by StatePreserver

        //bind camera state uniforms
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());

        this.depthTexture_color.bindToUnitUnsafe(DEPTH_TEXTURE_SAMPLER2D_BINDING);

        //ensure that the occlusion test shader reads the correct values from the visibility test shader
        //  same goes for the depth texture mipmaps
        this.gl.glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);
            if (levelInstance.capacityTiles == 0) {
                //skip empty detail levels
                continue;
            }

            levelInstance.selectedMask_firstPass.invalidateHint();

            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING, this.renderPosTable.vertexBuffer(level).bufferSSBO().id());
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, SRC_VISIBLE_TILES_SSBO_BINDING, levelInstance.selectedMask_visibility.id());
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, DST_SELECTED_TILES_SSBO_BINDING, levelInstance.selectedMask_firstPass.id());

            //cull the tiles!
            this.gl.glDispatchCompute(levelInstance.capacityTiles / CULLING_SHADER_WORK_GROUP_SIZE, 1, 1);
        }

        this.countSelectedBuffer.clearBufferDataZero();

        //ensure that the draw commands compressor reads the correct values from the previous shader invocation
        this.gl.glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);
            if (levelInstance.capacityTiles == 0) {
                //skip empty detail levels
                continue;
            }

            levelInstance.culledDrawListsGPU.invalidateHint();

            this.drawCommandsCompressor.compressCommands(
                    levelInstance.capacityTiles, RENDER_PASS_COUNT,
                    levelInstance.selectedMask_firstPass,
                    levelInstance.rawDrawListsGPU,
                    levelInstance.culledDrawListsGPU,
                    this.countSelectedBuffer, (long) level * COUNT_SELECTED_BUFFER_LEVEL_STRIDE + COUNT_SELECTED_BUFFER_PASSES_OFFSET);
        }

        this.enqueueDebugStatisticsUpdate(false);
    }

    @Override
    public void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        super.draw(mode, level, pass, shader, uniformSetter);
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Level extends AbstractGPUCulledBaseInstanceRenderIndex.Level {
        protected GLBuffer selectedMask_visibility;
        protected GLBuffer selectedMask_firstPass;

        public Level(OpenGL gl, DirectMemoryAllocator alloc) {
            super(gl, alloc);

            try {
                //no-op for now
            } catch (Throwable t) {
                throw PResourceUtil.closeSuppressed(t, this);
            }
        }

        @Override
        public void close() {
            try (val ignored = PResourceUtil.lazyCloseAll(
                    this.selectedMask_visibility,
                    this.selectedMask_firstPass)) {
                super.close();
            }
        }

        @Override
        protected void handleCapacityChanged(int oldCapacityTiles, int oldCapacityCommands, long oldCapacityBytes, int newCapacityTiles, int newCapacityCommands, long newCapacityBytes) {
            super.handleCapacityChanged(oldCapacityTiles, oldCapacityCommands, oldCapacityBytes, newCapacityTiles, newCapacityCommands, newCapacityBytes);

            PResourceUtil.close(this.selectedMask_visibility);
            this.selectedMask_visibility = GLBuffer.createFunctionallyImmutable(this.gl, (long) newCapacityTiles * Integer.BYTES, BufferUsage.STREAM_COPY, 0);

            PResourceUtil.close(this.selectedMask_firstPass);
            this.selectedMask_firstPass = GLBuffer.createFunctionallyImmutable(this.gl, (long) newCapacityTiles * Integer.BYTES, BufferUsage.STREAM_COPY, 0);
        }
    }
}
