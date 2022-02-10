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

package net.daporkchop.fp2.core.mode.heightmap.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.core.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.strategy.AbstractMultipassIndexedRenderStrategy;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.mode.heightmap.client.struct.HeightmapGlobalAttributes;
import net.daporkchop.fp2.core.mode.heightmap.client.struct.HeightmapLocalAttributes;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;

/**
 * @author DaPorkchop_
 */
@Getter
public class ShaderBasedHeightmapRenderStrategy extends AbstractMultipassIndexedRenderStrategy<HeightmapPos, HeightmapTile, HeightmapGlobalAttributes, HeightmapLocalAttributes> {
    protected final AttributeFormat<HeightmapGlobalAttributes> globalFormat;
    protected final AttributeFormat<HeightmapLocalAttributes> vertexFormat;

    protected final IndexFormat indexFormat;

    protected final DrawLayout drawLayout;

    protected final ReloadableShaderProgram<DrawShaderProgram> blockShader;
    protected final ReloadableShaderProgram<DrawShaderProgram> stencilShader;

    public ShaderBasedHeightmapRenderStrategy(@NonNull AbstractFarRenderer<HeightmapPos, HeightmapTile> farRenderer) {
        super(farRenderer);

        this.globalFormat = this.gl.createAttributeFormat(HeightmapGlobalAttributes.class).useFor(AttributeUsage.DRAW_GLOBAL).build();
        this.vertexFormat = this.gl.createAttributeFormat(HeightmapLocalAttributes.class).useFor(AttributeUsage.DRAW_LOCAL).build();
        this.indexFormat = this.gl.createIndexFormat().type(IndexType.UNSIGNED_SHORT).build();

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
                Identifier.from(FP2Core.MODID, "shaders/vert/heightmap/heightmap.vert"),
                Identifier.from(FP2Core.MODID, "shaders/frag/block.frag"));
        this.stencilShader = ReloadableShaderProgram.draw(this.gl, this.drawLayout, this.macros,
                Identifier.from(FP2Core.MODID, "shaders/vert/heightmap/heightmap.vert"),
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
        return HeightmapCullingStrategy.INSTANCE;
    }

    @Override
    public IRenderBaker<HeightmapPos, HeightmapTile, IndexedBakeOutput<HeightmapGlobalAttributes, HeightmapLocalAttributes>> createBaker() {
        return new HeightmapBaker(this.worldRenderer, this.textureUVs);
    }

    @Override
    public TransformLayoutBuilder configureSelectionLayout(@NonNull TransformLayoutBuilder builder, int level) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public TransformBindingBuilder configureSelectionBinding(@NonNull TransformBindingBuilder builder, int level) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public TransformShaderBuilder configureSelectionShader(@NonNull TransformShaderBuilder builder, int level) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    protected void doRelease() {
        super.doRelease();

        this.blockShader.close();
        this.stencilShader.close();
    }
}
