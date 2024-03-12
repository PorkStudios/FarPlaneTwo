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

package net.daporkchop.fp2.gl.codegen.struct.interleaved;

import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;

import java.util.function.Function;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInterleavedAttributeFormat<STRUCT extends AttributeStruct> extends NewAttributeFormat<STRUCT> {
    public final LayoutInfo layoutInfo;

    public AbstractInterleavedAttributeFormat(OpenGL gl, LayoutInfo layoutInfo) {
        super(gl, layoutInfo.compatibleTargets().apply(gl), layoutInfo.rootLayout().size());
        this.layoutInfo = layoutInfo;
    }

    @Override
    public final void bindVertexAttributeLocations(int program, @NonNull Function<String, String> nameFormatter, int baseBindingIndex) throws UnsupportedOperationException {
        if (!this.supports(AttributeTarget.VERTEX_ATTRIBUTE)) {
            throw new UnsupportedOperationException("Attribute format doesn't support " + AttributeTarget.VERTEX_ATTRIBUTE);
        }

        OpenGL gl = this.gl();
        StructAttributeType structType = this.layoutInfo.rootType();
        for (int index = 0; index < structType.fieldCount(); index++) {
            gl.glBindAttribLocation(program, baseBindingIndex, nameFormatter.apply(structType.fieldName(index)));
            baseBindingIndex += structType.fieldType(index).occupiedVertexAttributes();
        }
    }
}
