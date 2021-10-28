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

package net.daporkchop.fp2.gl.opengl.vertex;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.vertex.VertexAttributeBuilder;
import net.daporkchop.fp2.gl.vertex.VertexFormat;
import net.daporkchop.fp2.gl.vertex.VertexFormatBuilder;
import net.daporkchop.lib.common.math.PMath;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VertexFormatBuilderImpl implements VertexFormatBuilder.NameSelectionStage, VertexFormatBuilder.LayoutSelectionStage, VertexFormatBuilder.AlignmentSelectionStage, VertexFormatBuilder {
    protected final OpenGL gl;

    protected String name;
    protected boolean interleaved;
    protected int alignment;

    protected final ImmutableMap.Builder<String, VertexAttributeImpl> attributes = ImmutableMap.builder();
    protected int size;
    protected int offset;

    //
    // NameSelectionStage
    //

    @Override
    public LayoutSelectionStage name(@NonNull String name) {
        this.name = name;
        return this;
    }

    //
    // LayoutSelectionStage
    //

    @Override
    public AlignmentSelectionStage interleaved() {
        this.interleaved = true;
        return this;
    }

    @Override
    public AlignmentSelectionStage separate() {
        throw new UnsupportedOperationException(); //TODO: implement this
    }

    //
    // AlignmentSelectionStage
    //

    @Override
    public VertexFormatBuilder alignedTo(int alignment) {
        this.alignment = positive(alignment, "alignment");
        return this;
    }

    @Override
    public VertexFormatBuilder notAligned() {
        this.alignment = 1; //no alignment is functionally equivalent to an alignment of 1
        return this;
    }

    //
    // VertexFormatBuilder
    //

    @Override
    public VertexAttributeBuilder.NameSelectionStage attrib() {
        return new VertexAttributeBuilderImpl(this);
    }

    @Override
    public VertexFormat build() {
        return this.interleaved
                ? new VertexFormatImpl.Interleaved(this)
                : null;
    }

    //
    // internal
    //

    protected int addAttribute(@NonNull VertexAttributeImpl attribute) {
        this.attributes.put(attribute.name, attribute);
        return this.size++;
    }

    protected int computeSize(@NonNull VertexAttributeImpl attribute) {
        return PMath.roundUp(attribute.type.size(attribute.components), this.alignment);
    }

    protected int computeOffset(@NonNull VertexAttributeImpl attribute) {
        if (this.interleaved) {
            int offset = this.offset;
            this.offset += attribute.size;
            return offset;
        } else {
            return 0;
        }
    }
}
