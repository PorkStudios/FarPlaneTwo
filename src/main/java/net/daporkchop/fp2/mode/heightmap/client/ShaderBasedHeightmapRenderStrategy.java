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

package net.daporkchop.fp2.mode.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.command.elements.DrawElementsCommand;
import net.daporkchop.fp2.client.gl.shader.RenderShaderProgram;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.mode.common.client.bake.indexed.MultipassIndexedBakeOutput;
import net.daporkchop.fp2.mode.common.client.strategy.IShaderBasedMultipassRenderStrategy;
import net.daporkchop.fp2.mode.common.client.strategy.AbstractMultipassIndexedRenderStrategy;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;

/**
 * @author DaPorkchop_
 */
public class ShaderBasedHeightmapRenderStrategy extends AbstractMultipassIndexedRenderStrategy<HeightmapPos, HeightmapTile> implements IShaderBasedMultipassRenderStrategy<HeightmapPos, HeightmapTile, MultipassIndexedBakeOutput, DrawElementsCommand> {
    public ShaderBasedHeightmapRenderStrategy(@NonNull IFarRenderMode<HeightmapPos, HeightmapTile> mode) {
        super(mode, HeightmapBaker.VERTEX_FORMAT);
    }

    @Override
    public ICullingStrategy<HeightmapPos> cullingStrategy() {
        return HeightmapCullingStrategy.INSTANCE;
    }

    @Override
    public IRenderBaker<HeightmapPos, HeightmapTile, MultipassIndexedBakeOutput> createBaker() {
        return new HeightmapBaker();
    }

    @Override
    public RenderShaderProgram blockShader() {
        return HeightmapShaders.BLOCK_SHADER;
    }

    @Override
    public RenderShaderProgram stencilShader() {
        return HeightmapShaders.STENCIL_SHADER;
    }
}
