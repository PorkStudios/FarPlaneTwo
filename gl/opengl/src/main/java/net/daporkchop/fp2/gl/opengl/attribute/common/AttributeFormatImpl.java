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

package net.daporkchop.fp2.gl.opengl.attribute.common;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;

import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AttributeFormatImpl<F extends AttributeFormatImpl<F, S, SF>, S, SF extends StructFormat<S, ?>> extends BaseAttributeFormatImpl<F> implements AttributeFormat<S> {
    private final SF structFormat;

    public AttributeFormatImpl(@NonNull OpenGL gl, @NonNull SF structFormat) {
        super(gl);

        this.structFormat = structFormat;
    }

    @Override
    public long size() {
        return this.structFormat.totalSize();
    }

    @Override
    public String rawName() {
        return this.structFormat.structName();
    }

    @Override
    public List<GLSLField<?>> rawAttributeFields() {
        return this.structFormat.glslFields();
    }

    @Override
    public AttributeWriter<S> createWriter() {
        return this.structFormat().writer(this);
    }

    @Override
    public AttributeBuffer<S> createBuffer(@NonNull BufferUsage usage) {
        return this.structFormat().buffer(this, usage);
    }
}
