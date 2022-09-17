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
 */

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.BaseTexture;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.TextureStructFormat;

import java.util.function.IntConsumer;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseTextureImpl<F extends BaseTextureFormatImpl<F>> extends BaseAttributeBufferImpl<F> implements BaseTexture {
    protected final TextureStructFormat<Object> structFormat;

    protected final int id;
    protected final boolean managed;

    public BaseTextureImpl(@NonNull F format) {
        super(format);
        this.structFormat = format.structFormat();

        this.id = this.gl.api().glGenTexture();
        this.managed = true;
        this.gl.resourceArena().register(this, this.id, this.gl.api()::glDeleteTexture);
    }

    public BaseTextureImpl(@NonNull F format, int id) {
        super(format);
        this.structFormat = format.structFormat();

        this.id = id;
        this.managed = false;
    }

    @Override
    public void close() {
        if (this.managed) {
            this.gl.resourceArena().delete(this);
        }
    }

    public void bindAnyUnit(@NonNull TextureTarget target, @NonNull IntConsumer callback) {
        int oldBufferTexture = this.gl.api().glGetInteger(target.binding());
        try {
            this.gl.api().glBindTexture(target.target(), this.id);

            callback.accept(target.target());
        } finally {
            this.gl.api().glBindTexture(target.target(), oldBufferTexture);
        }
    }
}
