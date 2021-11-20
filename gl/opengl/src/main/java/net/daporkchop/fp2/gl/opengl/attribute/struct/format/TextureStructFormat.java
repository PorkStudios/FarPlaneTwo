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

package net.daporkchop.fp2.gl.opengl.attribute.struct.format;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.TextureStructLayout;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class TextureStructFormat<S> extends StructFormat<S, TextureStructLayout<S>> {
    protected final long stride;
    protected final int textureInternalFormat;
    protected final int textureFormat;
    protected final int textureType;

    public TextureStructFormat(@NonNull TextureStructLayout<S> layout, int textureInternalFormat, int textureFormat, int textureType) {
        super(layout);

        this.stride = layout.stride();
        this.textureInternalFormat = textureInternalFormat;
        this.textureFormat = textureFormat;
        this.textureType = textureType;
    }

    @Override
    public long totalSize() {
        return this.stride;
    }

    /**
     * Loads the fields from the given struct instance, translates them to the layout format, and writes them to the given destination.
     *
     * @param struct    the struct
     * @param dstBase   the destination base instance
     * @param dstOffset the destination base offset
     */
    public abstract void copy(@NonNull S struct, Object dstBase, long dstOffset);

    /**
     * Copies fields in the layout format from the given source to the given destination.
     *
     * @param srcBase   the source base instance
     * @param srcOffset the source base offset
     * @param dstBase   the destination base instance
     * @param dstOffset the destination base offset
     */
    public abstract void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset);
}
