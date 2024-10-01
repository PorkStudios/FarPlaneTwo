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
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.texture.GLBufferTexture;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.fp2.gl.texture.TextureTarget;
import net.daporkchop.fp2.gl.util.list.DirectFloatList;
import net.daporkchop.fp2.gl.util.list.DirectIVec2List;
import net.daporkchop.fp2.gl.util.list.DirectVec4List;
import net.daporkchop.fp2.gl.util.type.GLIVec2;
import net.daporkchop.fp2.gl.util.type.GLVec4;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.List;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Implementation of {@link GpuQuadLists} which uses {@link QuadsTechnique#BUFFER_TEXTURE}.
 *
 * @author DaPorkchop_
 */
public final class BufferTextureGpuQuadLists extends GpuQuadLists {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLBufferTexture.REQUIRED_EXTENSIONS;

    private final GLMutableBuffer listsBuffer;
    private final GLMutableBuffer quadsCoordBuffer;
    private final GLMutableBuffer quadsTintBuffer;

    private final GLBufferTexture listsTexture;
    private final GLBufferTexture quadsCoordTexture;
    private final GLBufferTexture quadsTintTexture;

    public BufferTextureGpuQuadLists(@NonNull OpenGL gl) {
        super(gl, QuadsTechnique.BUFFER_TEXTURE);

        try {
            gl.checkSupported(REQUIRED_EXTENSIONS);

            this.listsBuffer = GLMutableBuffer.create(gl);
            this.quadsCoordBuffer = GLMutableBuffer.create(gl);
            this.quadsTintBuffer = GLMutableBuffer.create(gl);

            this.listsTexture = GLBufferTexture.create(gl, TextureInternalFormat.RG32UI, this.listsBuffer);
            this.quadsCoordTexture = GLBufferTexture.create(gl, TextureInternalFormat.RGBA32F, this.quadsCoordBuffer);
            this.quadsTintTexture = GLBufferTexture.create(gl, TextureInternalFormat.R32F, this.quadsTintBuffer);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(
                this.quadsTintTexture,
                this.quadsCoordTexture,
                this.listsTexture,
                this.quadsTintBuffer,
                this.quadsCoordBuffer,
                this.listsBuffer);
    }

    private static void checkFitsInTextureBuffer(int maxTextureBufferSize, String name, int size, int scale) {
        if (size * scale > maxTextureBufferSize) {
            throw new IllegalStateException(name + " list doesn't fit in an OpenGL texture buffer! (" + size + " * " + scale + " bytes, GL_MAX_TEXTURE_BUFFER_SIZE=" + maxTextureBufferSize + ')');
        }
    }

    @Override
    public void set(@NonNull List<TextureUVs.QuadList> lists, @NonNull List<TextureUVs.PackedBakedQuad> quads) {
        int listsSize = lists.size();
        int quadsSize = quads.size();

        //ensure that both lists are small enough to fit in an OpenGL texture buffer
        int maxTextureBufferSize = this.gl.limits().maxTextureBufferSize();
        checkFitsInTextureBuffer(maxTextureBufferSize, "lists", listsSize, GLIVec2.BYTES);
        checkFitsInTextureBuffer(maxTextureBufferSize, "quads", quadsSize, GLVec4.BYTES);

        try (val alloc = new DirectMemoryAllocator()) { //temporary allocator for staging data
            try (val listsList = new DirectIVec2List(alloc)) {
                listsList.reserve(listsSize);
                for (val list : lists) {
                    listsList.add(new GLIVec2(list.texQuadListFirst(), list.texQuadListLast()));
                }

                this.listsBuffer.upload(listsList.byteBufferView(), BufferUsage.STATIC_DRAW);
            }

            try (val quadsCoordList = new DirectVec4List(alloc);
                 val quadsTintList = new DirectFloatList(alloc)) {
                quadsCoordList.reserve(quadsSize);
                quadsTintList.reserve(quadsSize);
                for (val quad : quads) {
                    quadsCoordList.add(new GLVec4(quad.texQuadCoordS(), quad.texQuadCoordT(), quad.texQuadCoordP(), quad.texQuadCoordQ()));
                    quadsTintList.add(quad.texQuadTint());
                }

                this.quadsCoordBuffer.upload(quadsCoordList.byteBufferView(), BufferUsage.STREAM_DRAW);
                this.quadsTintBuffer.upload(quadsTintList.byteBufferView(), BufferUsage.STREAM_DRAW);
            }
        }
    }

    @Override
    public void preservedBindState(StatePreserver.Builder builder) {
        super.preservedBindState(builder
                .texture(TextureTarget.TEXTURE_BUFFER, RenderConstants.TEXTURE_UVS_LISTS_SAMPLERBUFFER_BINDING)
                .texture(TextureTarget.TEXTURE_BUFFER, RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_BINDING)
                .texture(TextureTarget.TEXTURE_BUFFER, RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_BINDING));
    }

    @Override
    public void bind(OpenGL gl) {
        gl.glActiveTexture(GL_TEXTURE0 + RenderConstants.TEXTURE_UVS_LISTS_SAMPLERBUFFER_BINDING);
        gl.glBindTexture(GL_TEXTURE_BUFFER, this.listsTexture.id());
        gl.glActiveTexture(GL_TEXTURE0 + RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_BINDING);
        gl.glBindTexture(GL_TEXTURE_BUFFER, this.quadsCoordTexture.id());
        gl.glActiveTexture(GL_TEXTURE0 + RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_BINDING);
        gl.glBindTexture(GL_TEXTURE_BUFFER, this.quadsTintTexture.id());
    }
}
