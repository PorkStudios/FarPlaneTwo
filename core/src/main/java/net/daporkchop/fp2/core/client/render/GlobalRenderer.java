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
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;

/**
 * Client-side container which stores global render objects.
 *
 * @author DaPorkchop_
 */
@Getter
public final class GlobalRenderer implements AutoCloseable {
    private final FP2Core fp2;
    private final OpenGL gl;

    private final NewAttributeFormat<GlobalUniformAttributes> globalUniformAttributeFormat;

    private final NewAttributeFormat<VoxelGlobalAttributes> voxelInstancedAttributesFormat;
    private final NewAttributeFormat<VoxelLocalAttributes> voxelVertexAttributesFormat;

    public GlobalRenderer(FP2Core fp2, OpenGL gl) {
        this.fp2 = fp2;
        this.gl = gl;

        this.globalUniformAttributeFormat = NewAttributeFormat.get(gl, GlobalUniformAttributes.class, AttributeTarget.UBO);

        this.voxelInstancedAttributesFormat = NewAttributeFormat.get(gl, VoxelGlobalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);
        this.voxelVertexAttributesFormat = NewAttributeFormat.get(gl, VoxelLocalAttributes.class, AttributeTarget.VERTEX_ATTRIBUTE);
    }

    @Override
    public void close() {
        //TODO: delete shaders and stuff
    }
}
