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

import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.render.state.DrawStateUniforms;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.index.attribdivisor.GPUCulledBaseInstanceRenderIndex;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.EnumSet;

import static net.daporkchop.fp2.api.FP2.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * Client-side container which stores global render objects.
 *
 * @author DaPorkchop_
 */
public final class GlobalRenderer {
    public final ReloadableShaderRegistry shaderRegistry;

    public final AttributeFormat<CameraStateUniforms> cameraStateUniformsFormat;
    public final AttributeFormat<DrawStateUniforms> drawStateUniformsFormat;
    public final AttributeFormat<IFrustum.ClippingPlanes> frustumClippingPlanesUBOFormat;

    public final AttributeFormat<VoxelGlobalAttributes> voxelInstancedAttributesFormat;
    public final AttributeFormat<VoxelLocalAttributes> voxelVertexAttributesFormat;

    public final IndexFormat unsignedShortIndexFormat;

    public final AttributeFormat<TextureUVs.QuadListAttribute> uvQuadListSSBOFormat;
    public final AttributeFormat<TextureUVs.PackedBakedQuadAttribute> uvPackedQuadSSBOFormat;

    public final ShaderMacros shaderMacros;

    public GlobalRenderer(FP2Core fp2, OpenGL gl) {
        try {
            this.shaderRegistry = new ReloadableShaderRegistry(fp2);

            this.cameraStateUniformsFormat = AttributeFormat.get(gl, CameraStateUniforms.class, AttributeTarget.UBO);
            this.drawStateUniformsFormat = AttributeFormat.get(gl, DrawStateUniforms.class, AttributeTarget.UBO);
            this.frustumClippingPlanesUBOFormat = AttributeFormat.get(gl, IFrustum.ClippingPlanes.class, AttributeTarget.UBO);

            //determine whether we want the tile position attribute format to support the SSBO target
            boolean tilePositionArrayAsSsbo = false;
            tilePositionArrayAsSsbo |= gl.supports(GPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS);
            this.voxelInstancedAttributesFormat = AttributeFormat.get(gl, VoxelGlobalAttributes.class, tilePositionArrayAsSsbo
                    ? EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE, AttributeTarget.SSBO)
                    : EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE));

            this.voxelVertexAttributesFormat = AttributeFormat.get(gl, VoxelLocalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);

            this.unsignedShortIndexFormat = IndexFormat.get(IndexType.UNSIGNED_SHORT);

            this.uvQuadListSSBOFormat = AttributeFormat.get(gl, TextureUVs.QuadListAttribute.class, AttributeTarget.SSBO);
            this.uvPackedQuadSSBOFormat = AttributeFormat.get(gl, TextureUVs.PackedBakedQuadAttribute.class, AttributeTarget.SSBO);

            this.shaderMacros = ShaderMacros.builder()
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
                    .define("TEXTURE_UVS_LISTS_SSBO_NAME", RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME)
                    .define("TEXTURE_UVS_LISTS_SSBO_LAYOUT", this.uvQuadListSSBOFormat.interfaceBlockLayoutName())
                    .define("TEXTURE_UVS_QUADS_SSBO_NAME", RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME)
                    .define("TEXTURE_UVS_QUADS_SSBO_LAYOUT", this.uvPackedQuadSSBOFormat.interfaceBlockLayoutName())
                    .define("TILE_POS_ARRAY_UBO_NAME", RenderConstants.TILE_POS_ARRAY_UBO_NAME)
                    .define("TILE_POS_ARRAY_UBO_LAYOUT", "std140")
                    .define("TILE_POS_ARRAY_UBO_ELEMENTS", RenderConstants.tilePosArrayUBOElements(gl))
                    .build();

            AbstractFarRenderer.registerShaders(this, fp2);

            if (gl.supports(GPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS)) {
                GPUCulledBaseInstanceRenderIndex.registerShaders(this);
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this::close);
        }
    }

    private static <B extends ShaderProgram.Builder<?, B>> B commonShaderSetup(FP2Core fp2, B builder) {
        return builder
                .addUBO(RenderConstants.CAMERA_STATE_UNIFORMS_UBO_BINDING, RenderConstants.CAMERA_STATE_UNIFORMS_UBO_NAME)
                .addUBO(RenderConstants.DRAW_STATE_UNIFORMS_UBO_BINDING, RenderConstants.DRAW_STATE_UNIFORMS_UBO_NAME)
                .addSSBO(RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME)
                .addSSBO(RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME)
                .addSampler(fp2.client().terrainTextureUnit(), RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                .addSampler(fp2.client().lightmapTextureUnit(), RenderConstants.LIGHTMAP_SAMPLER_NAME);
    }

    public void close() {
        PResourceUtil.close(this.shaderRegistry);
    }
}
