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
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.state.CameraState;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.render.state.DrawState;
import net.daporkchop.fp2.core.client.render.state.DrawStateUniforms;
import net.daporkchop.fp2.core.client.render.textureuvs.gpu.GpuQuadLists;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.client.shader.ShaderRegistration;
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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.texture.TextureTarget;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.buffer.upload.ImmediateBufferUploader;
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
        final @NonNull GpuQuadLists.QuadsTechnique quadsTechnique;
        final boolean cutout;
        final boolean stencil;

        public ImmutableMap<String, Object> defines() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            builder.put("FP2_TILE_POS_TECHNIQUE", this.posTechnique.ordinal());
            builder.put("FP2_TEXTURE_UVS_TECHNIQUE", this.quadsTechnique.ordinal());
            if (this.cutout) {
                builder.put("FP2_CUTOUT", true);
            }
            return builder.build();
        }

        public ReloadableShaderProgram.SetupFunction<DrawShaderProgram.Builder> setupFunction(FP2Client client, GlobalRenderer globalRenderer) {
            return builder -> {
                builder.addUBO(RenderConstants.CAMERA_STATE_UNIFORMS_UBO_BINDING, RenderConstants.CAMERA_STATE_UNIFORMS_UBO_NAME);
                builder.addUBO(RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING, RenderConstants.DRAW_STATE_UNIFORMS_UBO_NAME);
                builder.addUBO(RenderConstants.TILE_POS_ARRAY_UBO_BINDING, RenderConstants.TILE_POS_ARRAY_UBO_NAME);
                builder.addSampler(client.terrainTextureUnit(), RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME);
                builder.addSampler(client.lightmapTextureUnit(), RenderConstants.LIGHTMAP_SAMPLER_NAME);
                builder.vertexAttributesWithPrefix("a_", globalRenderer.voxelVertexAttributesFormat);
                builder.vertexAttributesWithPrefix("a_", globalRenderer.voxelInstancedAttributesFormat);

                switch (this.quadsTechnique) {
                    case SSBO:
                        builder.addSSBO(RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME);
                        builder.addSSBO(RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME);
                        break;
                    case BUFFER_TEXTURE:
                        builder.addSampler(RenderConstants.TEXTURE_UVS_LISTS_SAMPLERBUFFER_BINDING, RenderConstants.TEXTURE_UVS_LISTS_SAMPLERBUFFER_NAME);
                        builder.addSampler(RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_BINDING, RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_NAME);
                        builder.addSampler(RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_BINDING, RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_NAME);
                        break;
                    case TEXTURE_2D:
                        builder.addSampler(RenderConstants.TEXTURE_UVS_LISTS_SAMPLER2D_BINDING, RenderConstants.TEXTURE_UVS_LISTS_SAMPLER2D_NAME);
                        builder.addSampler(RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLER2D_BINDING, RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLER2D_NAME);
                        builder.addSampler(RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLER2D_BINDING, RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLER2D_NAME);
                        break;
                    default:
                        throw new IllegalArgumentException(this.quadsTechnique.name());
                }
            };
        }

        public static List<DrawShaderVariant> allVariants(@NonNull OpenGL gl) {
            RenderIndex.PosTechnique[] posTechniques = RenderIndex.PosTechnique.values();
            GpuQuadLists.QuadsTechnique[] quadsTechniques = GpuQuadLists.QuadsTechnique.values();

            List<DrawShaderVariant> result = new ArrayList<>(posTechniques.length * quadsTechniques.length * 4);
            for (val posTechnique : posTechniques) {
                if (!gl.supports(posTechnique.requiredExtensions())) {
                    continue;
                }

                for (val quadsTechnique : quadsTechniques) {
                    if (!gl.supports(quadsTechnique.requiredExtensions())) {
                        continue;
                    }

                    for (int i = 0b00; i <= 0b11; i++) {
                        boolean cutout = (i & 0b01) != 0;
                        boolean stencil = (i & 0b10) != 0;

                        result.add(new DrawShaderVariant(posTechnique, quadsTechnique, cutout, stencil));
                    }
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
            super(GLExtensionSet.empty());
        }

        @Override
        public void registerShaders(@NonNull GlobalRenderer globalRenderer, @NonNull ReloadableShaderRegistry shaderRegistry, @NonNull ShaderMacros shaderMacros, @NonNull FP2Client client, @NonNull OpenGL gl) {
            for (DrawShaderVariant variant : DrawShaderVariant.allVariants(gl)) {
                val builder = shaderRegistry.createDraw(variant, shaderMacros.withDefined(variant.defines()), variant.setupFunction(client, globalRenderer));
                builder.addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"));
                if (!variant.stencil) {
                    builder.addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/block.frag"));
                }
                builder.build();
            }
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
    protected final GpuQuadLists.QuadsTechnique textureQuadsTechnique;
    protected final DrawMode drawMode;

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

            RenderIndexType renderIndexType = this.chooseRenderIndexType();

            this.vertexFormat = (AttributeFormat<VertexType>) globalRenderer.voxelVertexAttributesFormat;
            this.indexFormat = renderIndexType.absoluteIndices()
                    ? globalRenderer.unsignedIntIndexFormat
                    : globalRenderer.unsignedShortIndexFormat;

            //If GL_ARB_compatibility is supported, we can draw using quads instead of triangles.
            //Based on tests, this seems to very slightly improve performance - modern GPUs seem to be able to split indexed quads into triangles
            //  in hardware, and it reduces the size of our index buffers.
            //TODO: Would be nice to make this configurable, though.
            this.drawMode = this.gl.supports(GLExtension.GL_ARB_compatibility)
                    ? DrawMode.QUADS
                    : DrawMode.TRIANGLES;

            //TODO: figure out why i slapped a TODO on this
            this.bufferUploader = this.gl.supports(UnsynchronizedMapBufferUploader.REQUIRED_EXTENSIONS) //TODO
                    ? new UnsynchronizedMapBufferUploader(this.gl, 8 << 20) //8 MiB
                    : this.gl.supports(ScratchCopyBufferUploader.REQUIRED_EXTENSIONS)
                    ? new ScratchCopyBufferUploader(this.gl)
                    : ImmediateBufferUploader.instance();

            this.cameraStateUniformsBuffer = globalRenderer.cameraStateUniformsFormat.createUniformBuffer();
            this.drawStateUniformsBuffer = globalRenderer.drawStateUniformsFormat.createUniformBuffer();

            this.baker = this.createBaker(context, this.drawMode);
            this.bakeStorage = new PerLevelBakeStorage<>(this.gl, this.bufferUploader, this.vertexFormat, this.indexFormat, renderIndexType.absoluteIndices(),
                    (level, absoluteIndices) -> new SimpleBakeStorage<>(this.gl, this.bufferUploader, this.vertexFormat, this.indexFormat, absoluteIndices));
            this.renderIndex = renderIndexType.createRenderIndex(this.gl, this.bakeStorage, this.alloc, globalRenderer, this.cameraStateUniformsBuffer);
            this.bakeManager = new BakeManager<>(this, context.tileCache(), this.baker);

            this.tilePosTechnique = this.renderIndex.posTechnique();
            this.textureQuadsTechnique = this.levelRenderer.textureUVs().gpuQuadLists().quadsTechnique;

            this.blockShaderProgram = globalRenderer.shaderRegistry.get(new DrawShaderVariant(this.tilePosTechnique, this.textureQuadsTechnique, false, false));
            this.blockCutoutShaderProgram = globalRenderer.shaderRegistry.get(new DrawShaderVariant(this.tilePosTechnique, this.textureQuadsTechnique, true, false));
            this.blockStencilShaderProgram = globalRenderer.shaderRegistry.get(new DrawShaderVariant(this.tilePosTechnique, this.textureQuadsTechnique, false, true));

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
                        .indexedBuffer(IndexedBufferTarget.UNIFORM_BUFFER, RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING);
                this.levelRenderer.textureUVs().gpuQuadLists().preservedBindState(statePreserverBuilder);
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

    private RenderIndexType chooseRenderIndexType() {
        for (val implementation : RenderIndexType.getTypes(this.fp2)) {
            if (!implementation.enabled(this.fp2.globalConfig())) {
                this.fp2.log().debug("Render index implementation " + implementation + " is disabled by config");
            } else if (!this.gl.supports(implementation.requiredExtensions())) {
                //TODO: show a warning message ingame, somehow
                this.fp2.log().warn("Render index implementation " + implementation + " is enabled, but your OpenGL implementation doesn't support it! " + this.gl.unsupportedMsg(implementation.requiredExtensions()));
            } else {
                this.fp2.log().info("Using " + implementation);
                return implementation;
            }
        }

        throw new UnsupportedOperationException("No render index implementations are supported! (see log)");
    }

    protected abstract IRenderBaker<VertexType> createBaker(@NonNull IFarClientContext context, @NonNull DrawMode drawMode);

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
        //bind uniform buffers
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, RenderConstants.CAMERA_STATE_UNIFORMS_UBO_BINDING, this.cameraStateUniformsBuffer.buffer().id());
        this.gl.glBindBufferBase(GL_UNIFORM_BUFFER, RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING, this.drawStateUniformsBuffer.buffer().id());

        //bind texture UVs
        this.levelRenderer.textureUVs().gpuQuadLists().bind(this.gl);

        if (!FP2_DEBUG || this.fp2.globalConfig().debug().backfaceCulling()) {
            this.gl.glEnable(GL_CULL_FACE);
        }

        val reversedZ = this.fp2.client().renderManager().reversedZ();
        this.gl.glEnable(GL_DEPTH_TEST);
        this.gl.glDepthFunc(reversedZ != null && reversedZ.isActive() ? GL_GREATER : GL_LESS);

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
        protected IRenderBaker<VoxelLocalAttributes> createBaker(@NonNull IFarClientContext context, @NonNull DrawMode drawMode) {
            return new VoxelBaker(context, drawMode);
        }
    }
}
