/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.core.mode.voxel.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.core.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.core.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.strategy.AbstractMultipassIndexedRenderStrategy;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.mode.voxel.client.struct.VoxelLocalAttributes;

/**
 * @author DaPorkchop_
 */
@Getter
public class ShaderBasedVoxelRenderStrategy extends AbstractMultipassIndexedRenderStrategy<VoxelPos, VoxelTile, VoxelGlobalAttributes, VoxelLocalAttributes> {
    protected final AttributeFormat<VoxelGlobalAttributes> globalFormat;
    protected final AttributeFormat<VoxelLocalAttributes> vertexFormat;

    protected final IndexFormat indexFormat;

    protected final DrawLayout drawLayout;

    protected final ReloadableShaderProgram<DrawShaderProgram> blockShader;
    protected final ReloadableShaderProgram<DrawShaderProgram> stencilShader;

    public ShaderBasedVoxelRenderStrategy(@NonNull AbstractFarRenderer<VoxelPos, VoxelTile> farRenderer) {
        super(farRenderer);

        this.globalFormat = this.gl.createAttributeFormat(VoxelGlobalAttributes.class).useFor(AttributeUsage.DRAW_GLOBAL).build();
        this.vertexFormat = this.gl.createAttributeFormat(VoxelLocalAttributes.class).useFor(AttributeUsage.DRAW_LOCAL).build();

        this.indexFormat = this.gl.createIndexFormat()
                .type(IndexType.UNSIGNED_SHORT)
                .build();

        this.drawLayout = this.gl.createDrawLayout()
                .withGlobal(this.globalFormat)
                .withLocal(this.vertexFormat)
                .withUniform(this.uniformFormat)
                .withUniformArray(this.textureUVs.listsFormat())
                .withUniformArray(this.textureUVs.quadsFormat())
                .withTexture(this.textureFormatTerrain)
                .withTexture(this.textureFormatLightmap)
                .build();

        this.blockShader = ReloadableShaderProgram.draw(this.gl, this.drawLayout, this.macros,
                Identifier.from(FP2Core.MODID, "shaders/vert/voxel/voxel.vert"),
                Identifier.from(FP2Core.MODID, "shaders/frag/block.frag"));
        this.stencilShader = ReloadableShaderProgram.draw(this.gl, this.drawLayout, this.macros,
                Identifier.from(FP2Core.MODID, "shaders/vert/voxel/voxel.vert"),
                Identifier.from(FP2Core.MODID, "shaders/frag/stencil.frag"));
    }

    @Override
    public DrawShaderProgram blockShader() {
        return this.blockShader.get();
    }

    @Override
    public DrawShaderProgram stencilShader() {
        return this.stencilShader.get();
    }

    @Override
    public ICullingStrategy cullingStrategy() {
        return new VoxelCullingStrategy(this.worldRenderer.blockedTracker());
    }

    @Override
    public IRenderBaker<VoxelPos, VoxelTile, IndexedBakeOutput<VoxelGlobalAttributes, VoxelLocalAttributes>> createBaker() {
        return new VoxelBaker(this.worldRenderer, this.textureUVs);
    }

    @Override
    protected void doRelease() {
        super.doRelease();

        this.blockShader.close();
        this.stencilShader.close();
    }
}