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

package net.daporkchop.fp2.core.engine.client;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.state.CameraState;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.render.state.DrawState;
import net.daporkchop.fp2.core.client.render.state.DrawStateUniforms;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.engine.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.engine.client.bake.VoxelBaker;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.bake.storage.PerLevelBakeStorage;
import net.daporkchop.fp2.core.engine.client.bake.storage.SimpleBakeStorage;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndexType;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.attribute.texture.TextureTarget;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.buffer.upload.ScratchCopyBufferUploader;
import net.daporkchop.fp2.gl.buffer.upload.UnsynchronizedMapBufferUploader;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderer<VertexType extends AttributeStruct> extends AbstractReleasable {
    protected final FP2Core fp2;
    protected final LevelRenderer levelRenderer;
    protected final OpenGL gl;

    protected final IFarClientContext context;

    protected final AttributeFormat<VertexType> vertexFormat;
    protected final IndexFormat indexFormat;

    protected final DirectMemoryAllocator alloc = new DirectMemoryAllocator(false);
    protected final BufferUploader bufferUploader;

    protected final UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer;
    protected final UniformBuffer<DrawStateUniforms> drawStateUniformsBuffer;

    protected final IRenderBaker<VertexType> baker;
    protected final BakeStorage<VertexType> bakeStorage;
    protected final RenderIndex<VertexType> renderIndex;
    protected final BakeManager<VertexType> bakeManager;

    protected final DrawMode drawMode = DrawMode.QUADS; //TODO: this shouldn't be hardcoded

    protected final StatePreserver statePreserverSelect;
    protected final StatePreserver statePreserverDraw;

    @SuppressWarnings("unchecked")
    public AbstractFarRenderer(@NonNull IFarClientContext context) {
        try {
            this.fp2 = context.fp2();
            this.context = context;

            this.levelRenderer = context.level().renderer();
            this.gl = this.levelRenderer.gl();

            this.vertexFormat = (AttributeFormat<VertexType>) this.fp2.client().globalRenderer().voxelVertexAttributesFormat;
            this.indexFormat = this.fp2.client().globalRenderer().unsignedShortIndexFormat;

            this.bufferUploader = this.gl.supports(UnsynchronizedMapBufferUploader.REQUIRED_EXTENSIONS) //TODO
                    ? new UnsynchronizedMapBufferUploader(this.gl, 8 << 20) //8 MiB
                    : new ScratchCopyBufferUploader(this.gl);

            this.cameraStateUniformsBuffer = this.fp2.client().globalRenderer().cameraStateUniformsFormat.createUniformBuffer();
            this.drawStateUniformsBuffer = this.fp2.client().globalRenderer().drawStateUniformsFormat.createUniformBuffer();

            this.baker = this.createBaker();
            this.bakeStorage = this.createBakeStorage();
            this.renderIndex = this.createRenderIndex();
            this.bakeManager = new BakeManager<>(this, context.tileCache(), this.baker);

            {
                StatePreserver.Builder statePreserverBuilder = StatePreserver.builder(this.gl);
                this.renderIndex.preservedSelectState(statePreserverBuilder);
                this.statePreserverSelect = statePreserverBuilder.build();
            }

            {
                StatePreserver.Builder statePreserverBuilder = StatePreserver.builder(this.gl)
                        .activeProgram()
                        .vao()
                        .fixedFunctionDrawState()
                        .texture(TextureTarget.TEXTURE_2D, this.fp2.client().terrainTextureUnit())
                        .texture(TextureTarget.TEXTURE_2D, this.fp2.client().lightmapTextureUnit())
                        .indexedBuffer(IndexedBufferTarget.UNIFORM_BUFFER, RenderConstants.CAMERA_STATE_UNIFORMS_UBO_BINDING)
                        .indexedBuffer(IndexedBufferTarget.UNIFORM_BUFFER, RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING)
                        .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING)
                        .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING);
                this.renderIndex.preservedDrawState(statePreserverBuilder);
                this.statePreserverDraw = statePreserverBuilder.build();
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    protected void doRelease() {
        PResourceUtil.closeAll(
                this.bakeManager,
                this.renderIndex,
                this.bakeStorage,
                this.drawStateUniformsBuffer,
                this.cameraStateUniformsBuffer,
                this.bufferUploader,
                this.alloc);
    }

    protected abstract IRenderBaker<VertexType> createBaker();

    protected BakeStorage<VertexType> createBakeStorage() {
        return new PerLevelBakeStorage<>(this.gl, this.bufferUploader, this.vertexFormat, this.indexFormat,
                level -> new SimpleBakeStorage<>(this.gl, this.bufferUploader, this.vertexFormat, this.indexFormat));
    }

    protected abstract RenderIndex<VertexType> createRenderIndex();

    /**
     * Called before rendering a frame to prepare the render system for drawing the frame.
     *
     * @param frustum the current view frustum
     */
    public void prepare(@NonNull CameraState cameraState, @NonNull IFrustum frustum) {
        this.bufferUploader.tick();
        this.bakeManager.tick(this.bufferUploader, this.bakeStorage, this.renderIndex);

        //update camera state uniforms
        try (val uniforms = this.cameraStateUniformsBuffer.update()) {
            cameraState.configureUniforms(uniforms);
        }

        try (val ignored = this.statePreserverSelect.backup()) { //back up opengl state prior to selecting
            this.renderIndex.select(frustum, this.levelRenderer.blockedTracker());
        }
    }

    /**
     * Renders a frame.
     */
    public void render(@NonNull CameraState cameraState, @NonNull DrawState drawState) {
        //update draw state uniforms
        try (val uniforms = this.drawStateUniformsBuffer.update()) {
            drawState.configureUniforms(uniforms);

            if (FP2_DEBUG) {
                uniforms.debug_colorMode(this.fp2.globalConfig().debug().debugColors().ordinal());
            }
        }

        try (val ignored = this.statePreserverDraw.backup()) { //back up opengl state prior to rendering
            this.preRender();

            //in order to properly render overlapping layers while ensuring that low-detail levels always get placed on top of high-detail ones, we'll need to do the following:
            //- for each detail level:
            //  - render both the SOLID and CUTOUT passes at once, using the stencil to ensure that previously rendered SOLID and CUTOUT terrain at higher detail levels is left untouched
            //- render the TRANSPARENT pass at all detail levels at once, using the stencil to not only prevent low-detail from rendering over high-detail, but also fp2 transparent water
            //  from rendering over vanilla water

            for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
                this.renderSolid(level);
                this.renderCutout(level);
            }

            this.renderTransparent();

            this.postRender();
        }
    }

    private void preRender() {
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, RenderConstants.CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING, this.drawStateUniformsBuffer.buffer().id());

        val textureUVs = this.levelRenderer.textureUVs();
        this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING, textureUVs.listsBuffer().bufferSSBO().id());
        this.gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING, textureUVs.quadsBuffer().bufferSSBO().id());

        if (!FP2_DEBUG || this.fp2.globalConfig().debug().backfaceCulling()) {
            this.gl.glEnable(GL_CULL_FACE);
        }

        this.gl.glEnable(GL_DEPTH_TEST);
        this.gl.glDepthFunc(this.fp2.globalConfig().compatibility().reversedZ() ? GL_GREATER : GL_LESS);

        this.gl.glEnable(GL_STENCIL_TEST);
        this.gl.glStencilMask(0xFF);

        //clear stencil buffer to 0x7F
        this.gl.glClearStencil(0x7F);
        this.gl.glClear(GL_STENCIL_BUFFER_BIT);

        this.renderIndex.preDraw();
    }

    private void postRender() {
        this.renderIndex.postDraw();
    }

    private void renderSolid(int level) {
        //GlStateManager.disableAlpha();

        this.gl.glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE); //TODO: last two args should be swapped, i think
        this.gl.glStencilFunc(GL_LEQUAL, level, 0x7F);

        val shader = this.fp2.client().globalRenderer().blockShaderProgram.get();
        val uniformSetter = shader.bindUnsafe();
        this.renderIndex.draw(this.drawMode, level, RenderConstants.LAYER_SOLID, shader, uniformSetter);

        //GlStateManager.enableAlpha();
    }

    private void renderCutout(int level) {
        //MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, MC.gameSettings.mipmapLevels > 0);

        this.gl.glStencilOp(GL_KEEP, GL_REPLACE, GL_REPLACE);
        this.gl.glStencilFunc(GL_LEQUAL, level, 0x7F);

        val shader = this.fp2.client().globalRenderer().blockCutoutShaderProgram.get();
        val uniformSetter = shader.bindUnsafe();
        this.renderIndex.draw(this.drawMode, level, RenderConstants.LAYER_CUTOUT, shader, uniformSetter);

        //MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    private void renderTransparent() {
        this.renderTransparentStencilPass();
        this.renderTransparentFragmentPass();
    }

    private void renderTransparentStencilPass() {
        this.gl.glColorMask(false, false, false, false);
        this.gl.glDepthMask(false);

        val shader = this.fp2.client().globalRenderer().blockStencilShaderProgram.get();
        val uniformSetter = shader.bindUnsafe();

        this.gl.glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE); //TODO: last two args should be swapped, i think
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            this.gl.glStencilFunc(GL_GEQUAL, 0x80 | (EngineConstants.MAX_LODS - level), 0xFF);
            this.renderIndex.draw(this.drawMode, level, RenderConstants.LAYER_TRANSPARENT, shader, uniformSetter);
        }

        this.gl.glDepthMask(true);
        this.gl.glColorMask(true, true, true, true);
    }

    private void renderTransparentFragmentPass() {
        this.gl.glEnable(GL_BLEND);
        this.gl.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        //GlStateManager.alphaFunc(GL_GREATER, 0.1f);

        val shader = this.fp2.client().globalRenderer().blockShaderProgram.get();
        val uniformSetter = shader.bindUnsafe();

        this.gl.glStencilMask(0);
        this.gl.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            this.gl.glStencilFunc(GL_EQUAL, 0x80 | (EngineConstants.MAX_LODS - level), 0xFF);
            this.renderIndex.draw(this.drawMode, level, RenderConstants.LAYER_TRANSPARENT, shader, uniformSetter);
        }

        this.gl.glDisable(GL_BLEND);
    }

    public DebugStats.Renderer stats() {
        return this.bakeStorage.stats().add(this.renderIndex.stats());
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShaderMultidraw extends AbstractFarRenderer<VoxelLocalAttributes> {
        public ShaderMultidraw(@NonNull IFarClientContext context) {
            super(context);
        }

        @Override
        protected IRenderBaker<VoxelLocalAttributes> createBaker() {
            return new VoxelBaker(this.context);
        }

        @Override
        protected RenderIndex<VoxelLocalAttributes> createRenderIndex() {
            for (val implementation : RenderIndexType.values()) {
                if (!implementation.enabled(this.fp2.globalConfig())) {
                    this.fp2.log().debug("Render index implementation " + implementation + " is disabled by config");
                } else if (!this.gl.supports(implementation.requiredExtensions())) {
                    //TODO: show a warning message ingame, somehow
                    this.fp2.log().warn("Render index implementation " + implementation + " is enabled, but your OpenGL implementation doesn't support it! " + this.gl.unsupportedMsg(implementation.requiredExtensions()));
                } else {
                    return implementation.createRenderIndex(this.gl, this.bakeStorage, this.alloc, this.fp2.client().globalRenderer(), this.cameraStateUniformsBuffer);
                }
            }

            throw new UnsupportedOperationException("No render index implementations are supported! (see log)");
        }
    }
}
