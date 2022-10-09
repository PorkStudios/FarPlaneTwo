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

package net.daporkchop.fp2.gl.opengl.attribute.struct.format;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;

import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class StructFormat<S, L extends StructLayout<?, ?>> {
    protected final OpenGL gl;
    protected final String layoutName;

    protected final String structName;
    protected final List<GLSLField<?>> glslFields;

    public StructFormat(@NonNull OpenGL gl, @NonNull L layout) {
        this.gl = gl;
        this.layoutName = layout.layoutName();

        this.structName = layout.structInfo().name();
        this.glslFields = layout.structInfo().memberFields();
    }

    /**
     * @return the total size of a struct, in bytes
     */
    public abstract long totalSize();

    /**
     * @return a new {@link AttributeWriter} for the struct type
     */
    public abstract AttributeWriter<S> writer(@NonNull AttributeFormatImpl<?, S, ?> attributeFormat);

    /**
     * @return a new {@link AttributeBuffer} for the struct type
     */
    public abstract AttributeBuffer<S> buffer(@NonNull AttributeFormatImpl<?, S, ?> attributeFormat, @NonNull BufferUsage usage);
}
