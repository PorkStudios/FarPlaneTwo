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

package net.daporkchop.fp2.gl.opengl.attribute;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.instanced.InstancedAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.instanced.InstancedAttributeWriter;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.instanced.InstancedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.instanced.InstancedAttributeWriterImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeWriterImpl;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeWriter;
import net.daporkchop.fp2.gl.opengl.attribute.uniform.UniformAttributeBufferImpl;
import net.daporkchop.lib.common.math.PMath;

import java.util.Map;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class AttributeFormatImpl implements AttributeFormat {
    protected final OpenGL gl;

    protected final Map<String, AttributeImpl> attribs;
    protected final AttributeImpl[] attribsArray;

    protected final int[] offsetsLocal;
    protected final int[] sizesLocal;
    protected final int totalSizeLocal;

    protected final int[] offsetsInstanced;
    protected final int[] sizesInstanced;
    protected final int totalSizeInstanced;

    protected final int[] offsetsUniform;
    protected final int[] sizesUniform;
    protected final int totalSizeUniform;

    protected AttributeFormatImpl(@NonNull AttributeFormatBuilderImpl builder) {
        this.gl = builder.gl;

        this.attribs = builder.attributes.build();
        this.attribsArray = this.attribs.values().toArray(new AttributeImpl[0]);

        int attribCount = this.attribs.size();

        //locals
        this.offsetsLocal = new int[attribCount];
        this.sizesLocal = new int[attribCount];
        for (int alignment = 1, offset = 0, i = 0; i < attribCount; i++) {
            int size = PMath.roundUp(this.attribsArray[i].size(), alignment);
            this.offsetsLocal[i] = offset;
            this.sizesLocal[i] = size;
            offset += size;
        }
        this.totalSizeLocal = IntStream.of(this.sizesLocal).sum();

        //instanced
        this.offsetsInstanced = this.offsetsLocal;
        this.sizesInstanced = this.sizesLocal;
        this.totalSizeInstanced = this.totalSizeLocal;

        //uniform
        this.offsetsUniform = this.offsetsLocal;
        this.sizesUniform = this.sizesLocal;
        this.totalSizeUniform = this.totalSizeLocal;
    }

    @Override
    public Map<String, Attribute> attribs() {
        return uncheckedCast(this.attribs);
    }

    @Override
    public UniformAttributeBuffer createUniformBuffer(@NonNull BufferUsage usage) {
        return new UniformAttributeBufferImpl(this, usage);
    }

    @Override
    public InstancedAttributeWriter createInstancedWriter() {
        return new InstancedAttributeWriterImpl(this);
    }

    @Override
    public InstancedAttributeBuffer createInstancedBuffer(@NonNull BufferUsage usage) {
        return new InstancedAttributeBufferImpl(this, usage);
    }

    @Override
    public LocalAttributeWriter createLocalWriter() {
        return new LocalAttributeWriterImpl(this);
    }

    @Override
    public LocalAttributeBuffer createLocalBuffer(@NonNull BufferUsage usage) {
        return new LocalAttributeBufferImpl(this, usage);
    }
}
