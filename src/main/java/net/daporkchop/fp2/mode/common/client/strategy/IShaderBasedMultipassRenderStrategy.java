/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.shader.RenderShaderProgram;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.index.AbstractRenderIndex;

/**
 * @author DaPorkchop_
 */
public interface IShaderBasedMultipassRenderStrategy<POS extends IFarPos, T extends IFarTile> extends IMultipassRenderStrategy<POS, T> {
    @Override
    default void renderSolid(@NonNull AbstractRenderIndex<POS, ?, ?, ?> index, int level) {
        try (RenderShaderProgram program = this.blockShader().use()) {
            IMultipassRenderStrategy.super.renderSolid(index, level);
        }
    }

    @Override
    default void renderCutout(@NonNull AbstractRenderIndex<POS, ?, ?, ?> index, int level) {
        try (RenderShaderProgram program = this.blockShader().use()) {
            IMultipassRenderStrategy.super.renderCutout(index, level);
        }
    }

    @Override
    default void renderTransparentStencilPass(@NonNull AbstractRenderIndex<POS, ?, ?, ?> index) {
        try (RenderShaderProgram program = this.stencilShader().use()) {
            IMultipassRenderStrategy.super.renderTransparentStencilPass(index);
        }
    }

    @Override
    default void renderTransparentFragmentPass(@NonNull AbstractRenderIndex<POS, ?, ?, ?> index) {
        try (RenderShaderProgram program = this.blockShaderTransparent().use()) {
            IMultipassRenderStrategy.super.renderTransparentFragmentPass(index);
        }
    }

    /**
     * @return the shader used for rendering blocks
     */
    RenderShaderProgram blockShader();

    /**
     * @return the shader used for rendering blocks
     */
    default RenderShaderProgram blockShaderTransparent() {
        return this.blockShader();
    }

    /**
     * @return the shader used for preparing the stencil buffer
     */
    RenderShaderProgram stencilShader();
}
