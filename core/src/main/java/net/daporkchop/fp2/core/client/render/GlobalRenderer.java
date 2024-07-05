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

import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.shader.NewReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.index.attribdivisor.GPUCulledBaseInstanceRenderIndex;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.index.NewIndexFormat;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;

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

    public final NewAttributeFormat<GlobalUniformAttributes> globalUniformAttributeFormat;
    public final NewAttributeFormat<IFrustum.ClippingPlanes> frustumClippingPlanesUBOFormat;

    public final NewAttributeFormat<VoxelGlobalAttributes> voxelInstancedAttributesFormat;
    public final NewAttributeFormat<VoxelLocalAttributes> voxelVertexAttributesFormat;

    public final NewIndexFormat unsignedShortIndexFormat;

    public final NewAttributeFormat<TextureUVs.QuadListAttribute> uvQuadListSSBOFormat;
    public final NewAttributeFormat<TextureUVs.PackedBakedQuadAttribute> uvPackedQuadSSBOFormat;

    public final ShaderMacros shaderMacros;

    public final NewReloadableShaderProgram<DrawShaderProgram> blockShaderProgram;
    public final NewReloadableShaderProgram<DrawShaderProgram> blockCutoutShaderProgram;
    public final NewReloadableShaderProgram<DrawShaderProgram> blockStencilShaderProgram;

    public final NewReloadableShaderProgram<ComputeShaderProgram> indirectTileFrustumCullingShaderProgram;

    public GlobalRenderer(FP2Core fp2, OpenGL gl) {
        this.shaderRegistry = new ReloadableShaderRegistry(fp2);

        this.globalUniformAttributeFormat = NewAttributeFormat.get(gl, GlobalUniformAttributes.class, AttributeTarget.UBO);
        this.frustumClippingPlanesUBOFormat = NewAttributeFormat.get(gl, IFrustum.ClippingPlanes.class, AttributeTarget.UBO);

        //determine whether we want the tile position attribute format to support the SSBO target
        boolean tilePositionArrayAsSsbo = false;
        tilePositionArrayAsSsbo |= gl.supports(GPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS);
        this.voxelInstancedAttributesFormat = NewAttributeFormat.get(gl, VoxelGlobalAttributes.class, tilePositionArrayAsSsbo
                ? EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE, AttributeTarget.SSBO)
                : EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE));

        this.voxelVertexAttributesFormat = NewAttributeFormat.get(gl, VoxelLocalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);

        this.unsignedShortIndexFormat = NewIndexFormat.get(IndexType.UNSIGNED_SHORT);

        this.uvQuadListSSBOFormat = NewAttributeFormat.get(gl, TextureUVs.QuadListAttribute.class, AttributeTarget.SSBO);
        this.uvPackedQuadSSBOFormat = NewAttributeFormat.get(gl, TextureUVs.PackedBakedQuadAttribute.class, AttributeTarget.SSBO);

        this.shaderMacros = ShaderMacros.builder()
                .define("T_SHIFT", EngineConstants.T_SHIFT)
                .define("RENDER_PASS_COUNT", RenderConstants.RENDER_PASS_COUNT)
                .define("MAX_CLIPPING_PLANES", IFrustum.MAX_CLIPPING_PLANES)
                .define("FP2_DEBUG", FP2_DEBUG)

                .define("GLOBAL_UNIFORMS_UBO_NAME", RenderConstants.GLOBAL_UNIFORMS_UBO_NAME)
                .define("GLOBAL_UNIFORMS_UBO_LAYOUT", this.globalUniformAttributeFormat.interfaceBlockLayoutName())
                .define("TEXTURE_ATLAS_SAMPLER_NAME", RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                .define("LIGHTMAP_SAMPLER_NAME", RenderConstants.LIGHTMAP_SAMPLER_NAME)
                .define("TEXTURE_UVS_LISTS_SSBO_NAME", RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME)
                .define("TEXTURE_UVS_LISTS_SSBO_LAYOUT", this.uvQuadListSSBOFormat.interfaceBlockLayoutName())
                .define("TEXTURE_UVS_QUADS_SSBO_NAME", RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME)
                .define("TEXTURE_UVS_QUADS_SSBO_LAYOUT", this.uvPackedQuadSSBOFormat.interfaceBlockLayoutName())
                .build();

        NewReloadableShaderProgram.SetupFunction<DrawShaderProgram.Builder> shaderSetup = builder -> commonShaderSetup(builder)
                .vertexAttributesWithPrefix("a_", this.voxelInstancedAttributesFormat)
                .vertexAttributesWithPrefix("a_", this.voxelVertexAttributesFormat);

        this.blockShaderProgram = this.shaderRegistry.createDraw("block", this.shaderMacros, shaderSetup)
                .addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                .addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/block.frag"))
                .build();

        this.blockCutoutShaderProgram = this.shaderRegistry.createDraw("block_cutout", this.shaderMacros.toBuilder().define("FP2_CUTOUT").build(), shaderSetup)
                .addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                .addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/block.frag"))
                .build();

        this.blockStencilShaderProgram = this.shaderRegistry.createDraw("block_stencil", this.shaderMacros, shaderSetup)
                .addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                .addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/stencil.frag"))
                .build();

        if (gl.supports(GPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS)) {
            val computeShaderMacros = this.shaderMacros.toBuilder()
                    .define("TILE_POSITIONS_SSBO_NAME", RenderConstants.TILE_POSITIONS_SSBO_NAME)
                    .define("TILE_POSITIONS_SSBO_LAYOUT", this.voxelInstancedAttributesFormat.interfaceBlockLayoutName())
                    .define("INDIRECT_DRAWS_SSBO_NAME", RenderConstants.INDIRECT_DRAWS_SSBO_NAME)
                    .define("INDIRECT_DRAWS_SSBO_LAYOUT", "std430") //TODO: decide on the optimal layout automagically
                    .build();

            NewReloadableShaderProgram.SetupFunction<ComputeShaderProgram.Builder> computeShaderSetup = builder -> {
                commonShaderSetup(builder)
                        .addSSBO(RenderConstants.TILE_POSITIONS_SSBO_BINDING, RenderConstants.TILE_POSITIONS_SSBO_NAME);
                for (int pass = 0; pass < RenderConstants.RENDER_PASS_COUNT; pass++) {
                    builder.addSSBO(RenderConstants.INDIRECT_DRAWS_SSBO_FIRST_BINDING + pass, RenderConstants.INDIRECT_DRAWS_SSBO_NAME + '[' + pass + ']');
                }
            };

            this.indirectTileFrustumCullingShaderProgram = this.shaderRegistry.createCompute("tile_frustum_culling", computeShaderMacros, computeShaderSetup)
                    .addShader(ShaderType.COMPUTE, Identifier.from(MODID, "shaders/comp/indirect_tile_frustum_culling.comp"))
                    .build();
        } else {
            this.indirectTileFrustumCullingShaderProgram = null;
        }
    }

    private static <B extends ShaderProgram.Builder<?, B>> B commonShaderSetup(B builder) {
        return builder
                .addUBO(RenderConstants.GLOBAL_UNIFORMS_UBO_BINDING, RenderConstants.GLOBAL_UNIFORMS_UBO_NAME)
                .addSSBO(RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_LISTS_SSBO_NAME)
                .addSSBO(RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING, RenderConstants.TEXTURE_UVS_QUADS_SSBO_NAME)
                .addSampler(RenderConstants.TEXTURE_ATLAS_SAMPLER_BINDING, RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                .addSampler(RenderConstants.LIGHTMAP_SAMPLER_BINDING, RenderConstants.LIGHTMAP_SAMPLER_NAME);
    }

    public void close() {
        this.blockStencilShaderProgram.close();
        this.blockCutoutShaderProgram.close();
        this.blockShaderProgram.close();
    }
}
