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
import net.daporkchop.fp2.core.client.shader.NewReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.index.NewIndexFormat;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;

import static net.daporkchop.fp2.api.FP2.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * Client-side container which stores global render objects.
 *
 * @author DaPorkchop_
 */
public final class GlobalRenderer {
    public final NewAttributeFormat<GlobalUniformAttributes> globalUniformAttributeFormat;

    public final NewAttributeFormat<VoxelGlobalAttributes> voxelInstancedAttributesFormat;
    public final NewAttributeFormat<VoxelLocalAttributes> voxelVertexAttributesFormat;

    public final NewIndexFormat unsignedShortIndexFormat;

    public final NewAttributeFormat<TextureUVs.QuadListAttribute> uvQuadListSSBOFormat;
    public final NewAttributeFormat<TextureUVs.PackedBakedQuadAttribute> uvPackedQuadSSBOFormat;

    public final ShaderMacros.Mutable shaderMacros;

    public final NewReloadableShaderProgram<DrawShaderProgram> blockShaderProgram;
    public final NewReloadableShaderProgram<DrawShaderProgram> blockCutoutShaderProgram;
    public final NewReloadableShaderProgram<DrawShaderProgram> blockStencilShaderProgram;

    public GlobalRenderer(FP2Core fp2, OpenGL gl) {
        this.globalUniformAttributeFormat = NewAttributeFormat.get(gl, GlobalUniformAttributes.class, AttributeTarget.UBO);

        this.voxelInstancedAttributesFormat = NewAttributeFormat.get(gl, VoxelGlobalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);
        this.voxelVertexAttributesFormat = NewAttributeFormat.get(gl, VoxelLocalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);

        this.unsignedShortIndexFormat = NewIndexFormat.get(IndexType.UNSIGNED_SHORT);

        this.uvQuadListSSBOFormat = NewAttributeFormat.get(gl, TextureUVs.QuadListAttribute.class, AttributeTarget.SSBO);
        this.uvPackedQuadSSBOFormat = NewAttributeFormat.get(gl, TextureUVs.PackedBakedQuadAttribute.class, AttributeTarget.SSBO);

        this.shaderMacros = new ShaderMacros.Mutable()
                .define("T_SHIFT", EngineConstants.T_SHIFT)
                .define("GLOBAL_UNIFORMS_UBO_NAME", RenderConstants.GLOBAL_UNIFORMS_UBO_NAME)
                .define("TEXTURE_ATLAS_SAMPLER_NAME", RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                .define("LIGHTMAP_SAMPLER_NAME", RenderConstants.LIGHTMAP_SAMPLER_NAME)
                .define("FP2_DEBUG", FP2_DEBUG);

        NewReloadableShaderProgram.SetupFunction<DrawShaderProgram.Builder> shaderSetup = builder -> builder
                .vertexAttributesWithPrefix("a_", this.voxelInstancedAttributesFormat)
                .vertexAttributesWithPrefix("a_", this.voxelVertexAttributesFormat)
                .addUBO(RenderConstants.GLOBAL_UNIFORMS_UBO_BINDING, RenderConstants.GLOBAL_UNIFORMS_UBO_NAME)
                .addSampler(RenderConstants.TEXTURE_ATLAS_SAMPLER_BINDING, RenderConstants.TEXTURE_ATLAS_SAMPLER_NAME)
                .addSampler(RenderConstants.LIGHTMAP_SAMPLER_BINDING, RenderConstants.LIGHTMAP_SAMPLER_NAME);

        val shaderRegistry = fp2.client().reloadableShaderRegistry();

        this.blockShaderProgram = shaderRegistry.createDraw(this.shaderMacros, shaderSetup)
                .addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                .addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/block.frag"))
                .build();

        this.blockCutoutShaderProgram = shaderRegistry.createDraw(new ShaderMacros.Mutable(this.shaderMacros).define("FP2_CUTOUT"), shaderSetup)
                .addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                .addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/block.frag"))
                .build();

        this.blockStencilShaderProgram = shaderRegistry.createDraw(this.shaderMacros, shaderSetup)
                .addShader(ShaderType.VERTEX, Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                .addShader(ShaderType.FRAGMENT, Identifier.from(MODID, "shaders/frag/stencil.frag"))
                .build();
    }

    public void close() {
        this.blockStencilShaderProgram.close();
        this.blockCutoutShaderProgram.close();
        this.blockShaderProgram.close();
    }
}
