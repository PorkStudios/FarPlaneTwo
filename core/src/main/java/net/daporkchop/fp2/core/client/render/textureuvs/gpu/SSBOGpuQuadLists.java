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

package net.daporkchop.fp2.core.client.render.textureuvs.gpu;

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.List;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Implementation of {@link GpuQuadLists} which uses {@link QuadsTechnique#SSBO}.
 *
 * @author DaPorkchop_
 */
public final class SSBOGpuQuadLists extends GpuQuadLists {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GpuQuadLists.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_shader_storage_buffer_object);

    private final AttributeBuffer<TextureUVs.QuadListAttribute> listsBuffer;
    private final AttributeBuffer<TextureUVs.PackedBakedQuadAttribute> quadsBuffer;

    public SSBOGpuQuadLists(@NonNull OpenGL gl, @NonNull GlobalRenderer globalRenderer) {
        super(gl, QuadsTechnique.SSBO);

        try {
            gl.checkSupported(REQUIRED_EXTENSIONS);

            this.listsBuffer = globalRenderer.uvQuadListSSBOFormat.createBuffer();
            this.quadsBuffer = globalRenderer.uvPackedQuadSSBOFormat.createBuffer();
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(
                this.listsBuffer,
                this.quadsBuffer);
    }

    @Override
    public void set(@NonNull List<TextureUVs.QuadList> lists, @NonNull List<TextureUVs.PackedBakedQuad> quads) {
        try (val alloc = new DirectMemoryAllocator()) { //temporary allocator for staging data
            try (val listsOut = this.listsBuffer.format().createWriter(alloc)) {
                for (val list : lists) {
                    listsOut.append().copyFrom(list).close();
                }

                this.listsBuffer.set(listsOut, BufferUsage.STATIC_DRAW);
            }

            try (val quadsOut = this.quadsBuffer.format().createWriter(alloc)) {
                for (val quad : quads) {
                    quadsOut.append().copyFrom(quad).close();
                }

                this.quadsBuffer.set(quadsOut, BufferUsage.STATIC_DRAW);
            }
        }
    }

    @Override
    public void preservedBindState(StatePreserver.Builder builder) {
        super.preservedBindState(builder
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING)
                .indexedBuffer(IndexedBufferTarget.SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING));
    }

    @Override
    public void bind(OpenGL gl) {
        gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_LISTS_SSBO_BINDING, this.listsBuffer.bufferSSBO().id());
        gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, RenderConstants.TEXTURE_UVS_QUADS_SSBO_BINDING, this.quadsBuffer.bufferSSBO().id());
    }
}
