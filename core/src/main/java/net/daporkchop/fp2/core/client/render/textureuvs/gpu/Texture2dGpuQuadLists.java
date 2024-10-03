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
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.texture.GLTexture2D;
import net.daporkchop.fp2.gl.texture.PixelFormat;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.fp2.gl.texture.TextureTarget;
import net.daporkchop.fp2.gl.texture.PixelType;
import net.daporkchop.fp2.gl.util.AbstractDirectList;
import net.daporkchop.fp2.gl.util.list.DirectFloatList;
import net.daporkchop.fp2.gl.util.list.DirectIVec2List;
import net.daporkchop.fp2.gl.util.list.DirectVec4List;
import net.daporkchop.fp2.gl.util.type.GLIVec2;
import net.daporkchop.fp2.gl.util.type.GLVec4;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.math.PMath;

import java.util.List;
import java.util.function.Function;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Implementation of {@link GpuQuadLists} which uses {@link QuadsTechnique#BUFFER_TEXTURE}.
 *
 * @author DaPorkchop_
 */
public final class Texture2dGpuQuadLists extends GpuQuadLists {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty();

    public static final int X_COORD_WRAP = 1024; //this is the minimum permitted value of GL_MAX_TEXTURE_SIZE

    private final GLTexture2D listsTexture;
    private final GLTexture2D quadsCoordTexture;
    private final GLTexture2D quadsTintTexture;

    public Texture2dGpuQuadLists(@NonNull OpenGL gl) {
        super(gl, QuadsTechnique.TEXTURE_2D);

        try {
            gl.checkSupported(REQUIRED_EXTENSIONS);

            this.listsTexture = GLTexture2D.create(gl);
            this.quadsCoordTexture = GLTexture2D.create(gl);
            this.quadsTintTexture = GLTexture2D.create(gl);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(
                this.quadsTintTexture,
                this.quadsCoordTexture,
                this.listsTexture);
    }

    @Override
    public void set(@NonNull List<TextureUVs.QuadList> lists, @NonNull List<TextureUVs.PackedBakedQuad> quads) {
        int listsSize = lists.size();
        int quadsSize = quads.size();

        this.listsTexture.mipmapLevels(0, 0);
        this.quadsCoordTexture.mipmapLevels(0, 0);
        this.quadsTintTexture.mipmapLevels(0, 0);

        try (val alloc = new DirectMemoryAllocator()) { //temporary allocator for staging data
            try (val listsList = new DirectIVec2List(alloc)) {
                int width = X_COORD_WRAP;
                int height = PMath.roundUp(listsSize, width) / width;
                int capacity = width * height;

                listsList.reserve(capacity);
                for (val list : lists) {
                    listsList.add(new GLIVec2(list.texQuadListFirst(), list.texQuadListLast()));
                }
                listsList.appendZero(capacity - listsList.size());

                this.listsTexture.texImage(0, width, height, TextureInternalFormat.RG32UI, PixelFormat.RG_INTEGER, PixelType.UNSIGNED_INT, listsList.byteBufferView());
            }

            try (val quadsCoordList = new DirectVec4List(alloc);
                 val quadsTintList = new DirectFloatList(alloc)) {
                int width = X_COORD_WRAP;
                int height = PMath.roundUp(quadsSize, width) / width;
                int capacity = width * height;

                quadsCoordList.reserve(capacity);
                quadsTintList.reserve(capacity);
                for (val quad : quads) {
                    quadsCoordList.add(new GLVec4(quad.texQuadCoordS(), quad.texQuadCoordT(), quad.texQuadCoordP(), quad.texQuadCoordQ()));
                    quadsTintList.add(quad.texQuadTint());
                }
                quadsCoordList.appendZero(capacity - quadsCoordList.size());
                quadsTintList.appendZero(capacity - quadsTintList.size());

                this.quadsCoordTexture.texImage(0, width, height, TextureInternalFormat.RGBA32F, PixelFormat.RGBA, PixelType.FLOAT, quadsCoordList.byteBufferView());
                this.quadsTintTexture.texImage(0, width, height, TextureInternalFormat.R32F, PixelFormat.RED, PixelType.FLOAT, quadsTintList.byteBufferView());
            }
        }
    }

    @Override
    public void preservedBindState(StatePreserver.Builder builder) {
        super.preservedBindState(builder
                .texture(TextureTarget.TEXTURE_2D, RenderConstants.TEXTURE_UVS_LISTS_SAMPLER2D_BINDING)
                .texture(TextureTarget.TEXTURE_2D, RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLER2D_BINDING)
                .texture(TextureTarget.TEXTURE_2D, RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLER2D_BINDING));
    }

    @Override
    public void bind(OpenGL gl) {
        gl.glActiveTexture(GL_TEXTURE0 + RenderConstants.TEXTURE_UVS_LISTS_SAMPLER2D_BINDING);
        gl.glBindTexture(GL_TEXTURE_2D, this.listsTexture.id());
        gl.glActiveTexture(GL_TEXTURE0 + RenderConstants.TEXTURE_UVS_QUADS_COORD_SAMPLER2D_BINDING);
        gl.glBindTexture(GL_TEXTURE_2D, this.quadsCoordTexture.id());
        gl.glActiveTexture(GL_TEXTURE0 + RenderConstants.TEXTURE_UVS_QUADS_TINT_SAMPLER2D_BINDING);
        gl.glBindTexture(GL_TEXTURE_2D, this.quadsTintTexture.id());
    }
}
