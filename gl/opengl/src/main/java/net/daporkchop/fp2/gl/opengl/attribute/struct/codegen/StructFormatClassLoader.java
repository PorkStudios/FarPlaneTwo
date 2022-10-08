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
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.util.codegen.SimpleGeneratingClassLoader;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    protected abstract String generatedClassNamePrefix();

    protected String generatedClassNameSuffix() {
        return "layout='" + this.layout.layoutName() + "',struct=" + this.layout.structInfo().clazz().getTypeName().replace('.', '_');
    }

    protected abstract Class<?> baseFormatClass();

    protected String formatClassName() {
        return this.generatedClassNamePrefix() + AttributeFormat.class.getSimpleName() + "Impl_" + this.generatedClassNameSuffix();
    }

    protected abstract byte[] generateFormatClass();

    protected abstract Class<?> baseBufferClass();

    protected String bufferClassName() {
        return this.generatedClassNamePrefix() + AttributeBuffer.class.getSimpleName() + "Impl_" + this.generatedClassNameSuffix();
    }

    protected abstract byte[] generateBufferClass();

    protected abstract Class<?> baseWriterClass();

    protected String writerClassName() {
        return this.generatedClassNamePrefix() + AttributeWriter.class.getSimpleName() + "Impl_" + this.generatedClassNameSuffix();
    }

    protected abstract byte[] generateWriterClass();

    protected abstract Class<?> baseHandleClass();

    protected String handleClassName() {
        return this.generatedClassNamePrefix() + AttributeStruct.class.getSimpleName() + "Impl_" + this.generatedClassNameSuffix();
    }

    protected abstract byte[] generateHandleClass();

    protected String handleSetToSingleInternalName() {
        return this.generatedClassNamePrefix() + AttributeStruct.class.getSimpleName() + "Impl_setToSingle_" + this.generatedClassNameSuffix();
    }

    protected abstract byte[] generateHandleSetToSingleClass();

    @Override
    protected void registerClassGenerators(@NonNull BiConsumer<String, Supplier<byte[]>> registerGenerator, @NonNull Consumer<Class<?>> registerClass) {
        registerGenerator.accept(this.formatClassName(), this::generateFormatClass);
        registerGenerator.accept(this.bufferClassName(), this::generateBufferClass);
        registerGenerator.accept(this.writerClassName(), this::generateWriterClass);

        registerGenerator.accept(this.handleClassName(), this::generateHandleClass);
        registerGenerator.accept(this.handleSetToSingleInternalName(), this::generateHandleSetToSingleClass);

        //make the struct interface visible from the generated classloader (if it comes from a different classloader than the one which loaded the gl:opengl module,
        //  it wouldn't be visible to the generated classes)
        registerClass.accept(this.layout.structInfo().clazz());
    }

    public F createFormat() throws Exception {
        Class<?> formatClass = this.loadClass(this.formatClassName());
        return uncheckedCast(formatClass.getConstructor(OpenGL.class, this.layout.getClass()).newInstance(this.gl, this.layout));
    }
}
