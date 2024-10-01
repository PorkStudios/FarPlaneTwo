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

package net.daporkchop.fp2.core.client.render;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.render.state.DrawStateUniforms;
import net.daporkchop.fp2.core.client.render.textureuvs.gpu.GpuQuadLists;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.client.shader.ShaderRegistration;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.EnumSet;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * Client-side container which stores global render objects.
 *
 * @author DaPorkchop_
 */
public final class GlobalRenderer {
    @Getter
    private final FP2Core fp2;
    @Getter
    private final FP2Client client;
    @Getter
    private final OpenGL gl;

    public final ReloadableShaderRegistry shaderRegistry;

    public final AttributeFormat<CameraStateUniforms> cameraStateUniformsFormat;
    public final AttributeFormat<DrawStateUniforms> drawStateUniformsFormat;
    public final AttributeFormat<IFrustum.ClippingPlanes> frustumClippingPlanesUBOFormat;

    public final AttributeFormat<VoxelGlobalAttributes> voxelInstancedAttributesFormat;
    public final AttributeFormat<VoxelLocalAttributes> voxelVertexAttributesFormat;

    public final IndexFormat unsignedShortIndexFormat;
    public final IndexFormat unsignedIntIndexFormat;

    public final AttributeFormat<TextureUVs.QuadListAttribute> uvQuadListSSBOFormat;
    public final AttributeFormat<TextureUVs.PackedBakedQuadAttribute> uvPackedQuadSSBOFormat;

    public final ShaderMacros shaderMacros;

    public GlobalRenderer(@NonNull FP2Core fp2, @NonNull OpenGL gl) {
        try {
            this.fp2 = fp2;
            this.client = fp2.client();
            this.gl = gl;

            this.shaderRegistry = new ReloadableShaderRegistry(fp2);

            this.cameraStateUniformsFormat = AttributeFormat.get(gl, CameraStateUniforms.class, AttributeTarget.UBO);
            this.drawStateUniformsFormat = AttributeFormat.get(gl, DrawStateUniforms.class, AttributeTarget.UBO);
            this.frustumClippingPlanesUBOFormat = AttributeFormat.get(gl, IFrustum.ClippingPlanes.class, AttributeTarget.UBO);

            //determine whether we want the tile position attribute format to support the SSBO target
            val tilePositionsArrayTargets = gl.supports(GLExtension.GL_ARB_shader_storage_buffer_object)
                    ? EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE, AttributeTarget.SSBO)
                    : EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE);
            this.voxelInstancedAttributesFormat = AttributeFormat.get(gl, VoxelGlobalAttributes.class, tilePositionsArrayTargets);

            this.voxelVertexAttributesFormat = AttributeFormat.get(gl, VoxelLocalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);

            this.unsignedShortIndexFormat = IndexFormat.get(IndexType.UNSIGNED_SHORT);
            this.unsignedIntIndexFormat = IndexFormat.get(IndexType.UNSIGNED_INT);

            if (gl.supports(GpuQuadLists.QuadsTechnique.SSBO.requiredExtensions())) {
                this.uvQuadListSSBOFormat = AttributeFormat.get(gl, TextureUVs.QuadListAttribute.class, AttributeTarget.SSBO);
                this.uvPackedQuadSSBOFormat = AttributeFormat.get(gl, TextureUVs.PackedBakedQuadAttribute.class, AttributeTarget.SSBO);
            } else {
                this.uvQuadListSSBOFormat = null;
                this.uvPackedQuadSSBOFormat = null;
            }

            ShaderMacros.Builder shaderMacrosBuilder = ShaderMacros.builder()
                    .define("T_SHIFT", EngineConstants.T_SHIFT)
                    .define("RENDER_PASS_COUNT", RenderConstants.RENDER_PASS_COUNT)
                    .define("MAX_CLIPPING_PLANES", IFrustum.MAX_CLIPPING_PLANES)
                    .define("FP2_DEBUG", FP2_DEBUG)

                    .define("CAMERA_STATE_UNIFORMS_UBO_NAME", RenderConstants.CAMERA_STATE_UNIFORMS_UBO_NAME)
                    .define("CAMERA_STATE_UNIFORMS_UBO_LAYOUT", this.cameraStateUniformsFormat.interfaceBlockLayoutName())
                    .define("DRAW_STATE_UNIFORMS_UBO_NAME", RenderConstants.DRAW_STATE_UNIFORMS_UBO_NAME)
                    .define("DRAW_STATE_UNIFORMS_UBO_LAYOUT", this.drawStateUniformsFormat.interfaceBlockLayoutName())
                    .define("TEXTURE_ATLAS_SAMPLER_NAME", RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                    .define("LIGHTMAP_SAMPLER_NAME", RenderConstants.LIGHTMAP_SAMPLER_NAME)
                    .define("TILE_POS_ARRAY_UBO_NAME", RenderConstants.TILE_POS_ARRAY_UBO_NAME)
                    .define("TILE_POS_ARRAY_UBO_LAYOUT", "std140")
                    .define("TILE_POS_ARRAY_UBO_ELEMENTS", RenderConstants.tilePosArrayUBOElements(gl));

            if (gl.supports(GpuQuadLists.QuadsTechnique.SSBO.requiredExtensions())) {
                shaderMacrosBuilder
                        .define("TEXTURE_UVS_LISTS_SSBO_NAME", RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME)
                        .define("TEXTURE_UVS_LISTS_SSBO_LAYOUT", this.uvQuadListSSBOFormat.interfaceBlockLayoutName())
                        .define("TEXTURE_UVS_QUADS_SSBO_NAME", RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME)
                        .define("TEXTURE_UVS_QUADS_SSBO_LAYOUT", this.uvPackedQuadSSBOFormat.interfaceBlockLayoutName());
            }

            this.shaderMacros = shaderMacrosBuilder.build();

            val log = fp2.log();
            for (val shaderRegistration : ShaderRegistration.getTypes(fp2)) {
                val requiredExtensions = shaderRegistration.requiredExtensions();
                if (gl.supports(requiredExtensions)) {
                    log.debug("Registering shaders from %s", shaderRegistration);
                    shaderRegistration.registerShaders(this, this.shaderRegistry, this.shaderMacros, this.client, gl);
                } else {
                    log.debug("Not registering shaders from %s: %s", shaderRegistration, gl.unsupportedMsg(requiredExtensions));
                }
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this::close);
        }
    }

    public void close() {
        PResourceUtil.close(this.shaderRegistry);
    }
}
