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

package net.daporkchop.fp2.gl.opengl.attribute.texture.codegen;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.texture.BaseTextureFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatImpl;
import net.daporkchop.fp2.gl.codegen.util.SimpleGeneratingClassLoader;
import net.daporkchop.lib.common.annotation.param.Positive;

import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    protected final Cache<String, F> cachedFormatsByName = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    protected abstract String dimensionName(int dimension);

    protected abstract Class<?> baseFormatClass();

    protected String formatClassName() {
        return "TextureFormat" + this.dimensions + "DImpl_" + this.pixelFormat;
    }

    protected abstract byte[] generateFormatClass();

    protected abstract Class<?> baseTextureClass();

    protected String textureClassName() {
        return "Texture" + this.dimensions + "DImpl_" + this.pixelFormat;
    }

    protected abstract byte[] generateTextureClass();

    protected abstract Class<?> baseWriterClass();

    protected String writerClassName() {
        return "TextureWriter" + this.dimensions + "DImpl_" + this.pixelFormat;
    }

    protected abstract byte[] generateWriterClass();

    @Override
    protected void registerClassGenerators(BiConsumer<String, Supplier<byte[]>> registerGenerator, Consumer<Class<?>> registerClass) {
        registerGenerator.accept(this.formatClassName(), this::generateFormatClass);
        registerGenerator.accept(this.textureClassName(), this::generateTextureClass);
        registerGenerator.accept(this.writerClassName(), this::generateWriterClass);
    }

    @SneakyThrows(ExecutionException.class)
    public F createFormat(@NonNull String name) {
        return this.cachedFormatsByName.get(name.intern(), () -> {
            Class<?> formatClass = this.loadClass(this.formatClassName());
            return uncheckedCast(formatClass.getConstructor(OpenGL.class, String.class).newInstance(this.gl, name.intern()));
        });
    }
}
