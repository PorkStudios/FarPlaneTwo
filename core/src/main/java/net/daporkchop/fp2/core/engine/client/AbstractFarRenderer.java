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

import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.state.CameraState;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.render.state.DrawState;
import net.daporkchop.fp2.core.client.render.state.DrawStateUniforms;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderPrograms;
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
import net.daporkchop.fp2.core.engine.client.index.attribdivisor.CPUCulledUniformRenderIndex;
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
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderer<VertexType extends AttributeStruct> extends AbstractReleasable {
    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static final class DrawShaderVariant {
        final @NonNull RenderIndex.PosTechnique posTechnique;
        final boolean cutout;
        final boolean stencil;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_TILE_POS_TECHNIQUE", this.posTechnique.ordinal());
            if (this.cutout) {
                builder.put("FP2_CUTOUT", true);
            }
            return builder.build();
        }

        public static List<DrawShaderVariant> allVariants(@NonNull OpenGL gl) {
            List<DrawShaderVariant> result = new ArrayList<>();
            for (RenderIndex.PosTechnique posTechnique : RenderIndex.PosTechnique.values()) {
                if (!gl.supports(posTechnique.requiredExtensions())) {
                    continue;
                }

                for (int i = 0b00; i <= 0b11; i++) {
                    boolean cutout = (i & 0b01) != 0;
                    boolean stencil = (i & 0b10) != 0;

                    result.add(new DrawShaderVariant(posTechnique, cutout, stencil));
                }
            }
            return result;
        }
    }

    public static void registerShaders(GlobalRenderer globalRenderer, FP2Core fp2) {
        ReloadableShaderProgram.SetupFunction<DrawShaderProgram.Builder> shaderSetup = builder -> builder
                .addUBO(RenderConstants.CAMERA_STATE_UNIFORMS_UBO_BINDING, RenderConstants.CAMERA_STATE_UNIFORMS_UBO_NAME)
                .addUBO(RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING, RenderConstants.DRAW_STATE_UNIFORMS_UBO_NAME)
                .addUBO(RenderConstants.TILE_POS_ARRAY_UBO_BINDING, RenderConstants.TILE_POS_ARRAY_UBO_NAME)
                .addSSBO(RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME)
                .addSSBO(RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME)
                .addSampler(fp2.client().terrainTextureUnit(), RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                .addSampler(fp2.client().lightmapTextureUnit(), RenderConstants.LIGHTMAP_SAMPLER_NAME)
                .vertexAttributesWithPrefix("a_", globalRenderer.voxelVertexAttributesFormat)
                .vertexAttributesWithPrefix("a_", globalRenderer.voxelInstancedAttributesFormat);

        for (DrawShaderVariant variant : DrawShaderVariant.allVariants(fp2.client().gl())) {
            val builder = globalRenderer.shaderRegistry.createDraw(variant, globalRenderer.shaderMacros.toBuilder().defineAll(variant.defines()).build(), shaderSetup);
            builder.addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"));
            if (!variant.stencil) {
                builder.addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/block.frag"));
            }
            builder.build();
        }
    }

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

    protected final ReloadableShaderProgram<DrawShaderProgram> blockShaderProgram;
    protected final ReloadableShaderProgram<DrawShaderProgram> blockCutoutShaderProgram;
    protected final ReloadableShaderProgram<DrawShaderProgram> blockStencilShaderProgram;

    protected final IRenderBaker<VertexType> baker;
    protected final BakeStorage<VertexType> bakeStorage;
    protected final RenderIndex<VertexType> renderIndex;
    protected final BakeManager<VertexType> bakeManager;

    protected final RenderIndex.PosTechnique tilePosTechnique;
    protected final DrawMode drawMode = DrawMode.QUADS; //TODO: this shouldn't be hardcoded

    protected final StatePreserver statePreserverSelect;
    protected final StatePreserver statePreserverDraw;

    @SuppressWarnings("unchecked")
    public AbstractFarRenderer(@NonNull IFarClientContext context) {
        try {
            this.fp2 = context.fp2();
            this.context = context;

            FP2Client client = this.fp2.client();
            GlobalRenderer globalRenderer = client.globalRenderer();

            this.levelRenderer = context.level().renderer();
            this.gl = client.gl();

            this.vertexFormat = (AttributeFormat<VertexType>) globalRenderer.voxelVertexAttributesFormat;
            this.indexFormat = globalRenderer.unsignedShortIndexFormat;

            this.bufferUploader = this.gl.supports(UnsynchronizedMapBufferUploader.REQUIRED_EXTENSIONS) //TODO
                    ? new UnsynchronizedMapBufferUploader(this.gl, 8 << 20) //8 MiB
                    : new ScratchCopyBufferUploader(this.gl);

            this.cameraStateUniformsBuffer = globalRenderer.cameraStateUniformsFormat.createUniformBuffer();
            this.drawStateUniformsBuffer = globalRenderer.drawStateUniformsFormat.createUniformBuffer();

            this.baker = this.createBaker();
            this.bakeStorage = this.createBakeStorage();
            this.renderIndex = this.createRenderIndex();
            this.bakeManager = new BakeManager<>(this, context.tileCache(), this.baker);

            this.tilePosTechnique = this.renderIndex.posTechnique();

            this.blockShaderProgram = globalRenderer.shaderRegistry.get(new DrawShaderVariant(this.tilePosTechnique, false, false));
            this.blockCutoutShaderProgram = globalRenderer.shaderRegistry.get(new DrawShaderVariant(this.tilePosTechnique, true, false));
            this.blockStencilShaderProgram = globalRenderer.shaderRegistry.get(new DrawShaderVariant(this.tilePosTechnique, false, true));

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
                        .texture(TextureTarget.TEXTURE_2D, client.terrainTextureUnit())
                        .texture(TextureTarget.TEXTURE_2D, client.lightmapTextureUnit())
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

        val shader = this.blockShaderProgram.get();
        val uniformSetter = shader.bindUnsafe();
        this.renderIndex.draw(this.drawMode, level, RenderConstants.LAYER_SOLID, shader, uniformSetter);

        //GlStateManager.enableAlpha();
    }

    private void renderCutout(int level) {
        //MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, MC.gameSettings.mipmapLevels > 0);

        this.gl.glStencilOp(GL_KEEP, GL_REPLACE, GL_REPLACE);
        this.gl.glStencilFunc(GL_LEQUAL, level, 0x7F);

        val shader = this.blockCutoutShaderProgram.get();
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

        val shader = this.blockStencilShaderProgram.get();
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

        val shader = this.blockShaderProgram.get();
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
