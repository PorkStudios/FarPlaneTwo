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

package net.daporkchop.fp2.gl.opengl.attribute.texture.codegen;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.texture.BaseTextureFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatImpl;
import net.daporkchop.fp2.gl.opengl.util.codegen.SimpleGeneratingClassLoader;
import net.daporkchop.lib.common.annotation.param.Positive;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class TextureFormatClassLoader<F extends BaseTextureFormatImpl<F>> extends SimpleGeneratingClassLoader {
    @NonNull
    protected final OpenGL gl;
    @NonNull
    protected final PixelFormatImpl pixelFormat;

    protected final @Positive int dimensions;

    protected abstract String dimensionName(int dimension);

    protected String formatClassName() {
        return "TextureFormat" + this.dimensions + "DImpl";
    }

    protected abstract byte[] generateFormatClass();

    protected String textureClassName() {
        return "Texture" + this.dimensions + "DImpl";
    }

    protected abstract byte[] generateTextureClass();

    protected String writerClassName() {
        return "TextureWriter" + this.dimensions + "DImpl";
    }

    protected abstract byte[] generateWriterClass();

    @Override
    protected void registerClassGenerators(@NonNull BiConsumer<String, Supplier<byte[]>> register) {
        register.accept(this.formatClassName(), this::generateFormatClass);
        register.accept(this.textureClassName(), this::generateTextureClass);
        register.accept(this.writerClassName(), this::generateWriterClass);
    }

    public F createFormat() throws Exception {
        Class<?> formatClass = this.loadClass(this.formatClassName());
        return uncheckedCast(formatClass.getConstructor(OpenGL.class, String.class).newInstance(this.gl, ""));
    }
}
