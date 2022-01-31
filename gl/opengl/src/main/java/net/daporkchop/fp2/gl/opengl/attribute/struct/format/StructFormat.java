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
 *
 */

package net.daporkchop.fp2.gl.opengl.attribute.struct.format;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;

import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class StructFormat<S, L extends StructLayout<?, ?>> {
    protected final String layoutName;

    protected final String structName;
    protected final List<GLSLField<?>> glslFields;

    public StructFormat(@NonNull L layout) {
        this.layoutName = layout.layoutName();

        this.structName = layout.structInfo().name();
        this.glslFields = layout.structInfo().memberFields();
    }

    /**
     * @return the total size of a struct, in bytes
     */
    public abstract long totalSize();

    /**
     * Creates a deep clone of the given struct instance.
     *
     * @param struct the source instance
     * @return the cloned instance
     */
    public abstract S clone(@NonNull S struct);
}
