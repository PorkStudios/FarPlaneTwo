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
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.NIOBufferUtil;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.listener.FramebufferResizeListener;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.compute.ComputeIndirectDrawCommandsCompressor;
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
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.VertexMode;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.texture.GLTexture2D;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.fp2.gl.texture.framebuffer.FramebufferAttachment;
import net.daporkchop.fp2.gl.texture.framebuffer.GLFramebuffer;
import net.daporkchop.fp2.gl.util.AnyMemoryRegion;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.nio.ByteBuffer;

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

    private static final String VISIBLE_TILES_SSBO_NAME = "B_VisibleTiles"; //synced with resources/assets/fp2/shaders/frag/occlusion_culled_cube.frag
    private static final int VISIBLE_TILES_SSBO_BINDING = TILE_POSITIONS_SSBO_BINDING + 1;

    private static final Object OCCLUSION_CULLED_CUBE_KEY = "occlusion_culled_cube";

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
                    .addSSBO(VISIBLE_TILES_SSBO_BINDING, VISIBLE_TILES_SSBO_NAME)
                    .build();
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

    private final ReloadableShaderProgram<DrawShaderProgram> occlusionCulledCubeShader;

    //protected final ComputeTextureMipmapGenerator mipmapGenerator;
    private final ComputeIndirectDrawCommandsCompressor drawCommandsCompressor;

    private GLTexture2D depthTexture;
    private final GLFramebuffer copyFramebuffer;

    private final VertexArrayObject cubeVao;
    private final GLBuffer cubeIndexBuffer;
    private final int cubeMeshVertices;
    private final IndexType cubeMeshIndexType;

    private final ListenerList<FramebufferResizeListener>.Handle framebufferResizeListenerHandle;

    public GPUOcclusionCulledBaseInstanceRenderIndex(FP2Client client, OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
        super(gl.checkSupported(REQUIRED_EXTENSIONS), bakeStorage, alloc, globalRenderer, cameraStateUniformsBuffer);

        try {
            this.occlusionCulledCubeShader = globalRenderer.shaderRegistry.get(OCCLUSION_CULLED_CUBE_KEY);

            //this.mipmapGenerator = new ComputeTextureMipmapGenerator(gl, globalRenderer);
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
                //this.mipmapGenerator,
                this.drawCommandsCompressor,
                this.copyFramebuffer,
                this.depthTexture)) {
            super.close();
        }
    }

    @Override
    protected Level createLevel(int levelIndex) {
        return new Level(this.gl, this.alloc);
    }

    @Override
    public void onFramebufferResize(int width, int height) {
        PResourceUtil.close(this.depthTexture);
        this.depthTexture = null;

        //allocate a new texture for the depth values
        this.depthTexture = GLTexture2D.create(this.gl, TextureInternalFormat.DEPTH_COMPONENT_32F, GLTexture2D.requiredLevels(width, height), width, height);

        //attach the new depth texture to the framebuffer
        this.copyFramebuffer.attachTexture(FramebufferAttachment.DEPTH_ATTACHMENT, this.depthTexture, 0);
        this.copyFramebuffer.checkComplete();
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        this.renderPosTable.flush();

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);
            if (levelInstance.capacityTiles == 0) {
                //skip empty tiles
                continue;
            }

            levelInstance.flushRawDrawLists();
        }
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder);

        //this.mipmapGenerator.configureModifiedState(builder);
        this.drawCommandsCompressor.configureModifiedState(builder);

        builder.fixedFunctionDrawState()
                .activeProgram()
                .vao();
    }

    @Override
    public void preDraw() {
        super.preDraw();

        /*//TODO: this needs to run after we've rendered all the tiles from the previous frame
        int oldDrawFramebuffer = this.gl.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            this.gl.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.copyFramebuffer.id());

            //blit the active depth buffer into the depth texture
            this.gl.glBlitNamedFramebuffer(
                    oldDrawFramebuffer, this.copyFramebuffer.id(),
                    0, 0, this.depthTexture.width(), this.depthTexture.height(),
                    0, 0, this.depthTexture.width(), this.depthTexture.height(),
                    GL_DEPTH_BUFFER_BIT, GL_NEAREST);

            //generate mipmaps
            this.mipmapGenerator.generateMipmaps(
                    this.depthTexture, 0,
                    this.depthTexture, 1,
                    this.depthTexture.levels() - 1,
                    ComputeTextureMipmapGenerator.MipmapMode.MIN); //TODO: the mode should be inverted if reversed-Z is active
        } finally {
            this.gl.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawFramebuffer);
        }*/

        //for now we will perform just a single occlusion culling pass
        val occlusionCulledCubeProgram = this.occlusionCulledCubeShader.get();
        occlusionCulledCubeProgram.bindUnsafe(); //active program binding will be restored by StatePreserver

        //bind camera state uniforms
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());

        this.cubeVao.bindUnsafe();

        this.gl.glEnable(GL_CULL_FACE);
        this.gl.glColorMask(false, false, false, false);
        this.gl.glDepthMask(false);

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);
            val tilePosArray = this.renderPosTable.vertexBuffer(level);
            int capacity = tilePosArray.capacity(); // TODO: maybe use some other mechanism to decide on this rather than capacity (as capacity can't shrink back down)

            if (capacity == 0) {
                //skip empty detail levels
                continue;
            }

            levelInstance.renderedTilesFirstPassBuffer.clearBufferDataZero();

            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, TILE_POSITIONS_SSBO_BINDING, tilePosArray.bufferSSBO().id());
            this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, VISIBLE_TILES_SSBO_BINDING, levelInstance.renderedTilesFirstPassBuffer.id());

            this.gl.glDrawElementsInstanced(GL_TRIANGLES, this.cubeMeshVertices, this.cubeMeshIndexType.type(), 0L, capacity);
        }

        this.gl.glDepthMask(true);
        this.gl.glColorMask(true, true, true, true);
        this.gl.glDisable(GL_CULL_FACE);

        this.countSelectedBuffer.clearBufferDataZero();
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            val levelInstance = this.levels.get(level);

            if (levelInstance.capacityTiles == 0) {
                //skip empty detail levels
                continue;
            }

            levelInstance.culledDrawListsGPU.invalidateHint();

            this.drawCommandsCompressor.compressCommands(
                    levelInstance.capacityTiles, RENDER_PASS_COUNT,
                    levelInstance.renderedTilesFirstPassBuffer,
                    levelInstance.rawDrawListsGPU,
                    levelInstance.culledDrawListsGPU,
                    this.countSelectedBuffer, (long) level * COUNT_SELECTED_BUFFER_LEVEL_STRIDE + COUNT_SELECTED_BUFFER_PASSES_OFFSET);
        }

        this.enqueueDebugStatisticsUpdate(false);
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Level extends AbstractGPUCulledBaseInstanceRenderIndex.Level {
        protected GLBuffer renderedTilesLastFrameBuffer;
        protected GLBuffer renderedTilesFirstPassBuffer;
        protected GLBuffer renderedTilesSecondPassBuffer;

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
                    this.renderedTilesLastFrameBuffer,
                    this.renderedTilesFirstPassBuffer,
                    this.renderedTilesSecondPassBuffer)) {
                super.close();
            }
        }

        @Override
        protected void handleCapacityChanged(int oldCapacityTiles, int oldCapacityCommands, long oldCapacityBytes, int newCapacityTiles, int newCapacityCommands, long newCapacityBytes) {
            super.handleCapacityChanged(oldCapacityTiles, oldCapacityCommands, oldCapacityBytes, newCapacityTiles, newCapacityCommands, newCapacityBytes);

            //for now, when adjusting the capacity we simply forget all the old visibility information by clearing everything to
            //  zero, this could be enhanced in the future
            PResourceUtil.close(this.renderedTilesLastFrameBuffer);
            this.renderedTilesLastFrameBuffer = GLBuffer.createFunctionallyImmutable(this.gl, (long) newCapacityTiles * Integer.BYTES, BufferUsage.STREAM_COPY, 0);
            this.renderedTilesLastFrameBuffer.clearBufferDataZero();

            PResourceUtil.close(this.renderedTilesFirstPassBuffer);
            this.renderedTilesFirstPassBuffer = GLBuffer.createFunctionallyImmutable(this.gl, (long) newCapacityTiles * Integer.BYTES, BufferUsage.STREAM_COPY, 0);

            PResourceUtil.close(this.renderedTilesSecondPassBuffer);
            this.renderedTilesSecondPassBuffer = GLBuffer.createFunctionallyImmutable(this.gl, (long) newCapacityTiles * Integer.BYTES, BufferUsage.STREAM_COPY, 0);
        }
    }
}
