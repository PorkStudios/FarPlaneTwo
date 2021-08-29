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

package net.daporkchop.fp2.client.gl.vertex.attribute;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.lib.common.math.PMath;

import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;

/**
 * Base implementation of {@link AbstractVertexAttribute}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractVertexAttribute<T extends IVertexAttribute> implements IVertexAttribute {
    protected final IVertexAttribute parent;
    protected final VertexAttributeInterpretation interpretation;
    protected final VertexAttributeType type;

    protected final int index;
    protected final int size;
    protected final int components;
    protected final int reportedComponents;

    public AbstractVertexAttribute(@NonNull VertexAttributeBuilder<T> builder) {
        this.parent = builder.parent();
        this.interpretation = builder.interpretation();
        this.type = builder.type();

        this.index = this.parent != null ? this.parent.index() + 1 : 0;

        this.components = builder.components();
        this.reportedComponents = builder.reportedComponents() >= 0 ? builder.reportedComponents() : this.components;
        this.size = PMath.roundUp(this.type.size(this.components), EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT);
    }

    @Override
    public void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, long offset, int stride) {
        this.interpretation.configureVAO(vao, buffer, this.reportedComponents, this.type, stride, offset, 0);
    }
}
