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

package net.daporkchop.fp2.gl.opengl.attribute.struct.codegen;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.util.codegen.SimpleGeneratingClassLoader;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class StructFormatClassLoader<S, L extends StructLayout<?, ?>, F extends StructFormat<S, L>> extends SimpleGeneratingClassLoader {
    @NonNull
    protected final OpenGL gl;
    @NonNull
    protected final L layout;

    protected String formatClassName() {
        return "StructFormatImpl";
    }

    protected abstract byte[] generateFormatClass();

    protected String bufferClassName() {
        return "AttributeBufferImpl";
    }

    protected abstract byte[] generateBufferClass();

    protected String writerClassName() {
        return "AttributeWriterImpl";
    }

    protected abstract byte[] generateWriterClass();

    protected String handleClassName() {
        return "AttributeHandleImpl";
    }

    protected abstract byte[] generateHandleClass();

    @Override
    protected void registerClassGenerators(@NonNull BiConsumer<String, Supplier<byte[]>> register) {
        register.accept(this.formatClassName(), this::generateFormatClass);
        register.accept(this.bufferClassName(), this::generateBufferClass);
        register.accept(this.writerClassName(), this::generateWriterClass);
        register.accept(this.handleClassName(), this::generateHandleClass);
    }

    public F createFormat() throws Exception {
        Class<?> formatClass = this.loadClass(this.formatClassName());
        return uncheckedCast(formatClass.getConstructor(OpenGL.class, this.layout.getClass()).newInstance(this.gl, this.layout));
    }
}
