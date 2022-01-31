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

package net.daporkchop.fp2.gl.opengl.attribute.struct.type;

import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Data
@With
public final class GLSLArrayType implements GLSLType {
    @NonNull
    private final GLSLType elementType;
    private final int size;

    public GLSLArrayType(@NonNull GLSLType elementType, int size) {
        this.elementType = elementType;
        this.size = positive(size, "size");
    }

    @Override
    public String declaration(@NonNull String fieldName) {
        return this.elementType.declaration(fieldName + '[' + this.size + ']');
    }

    @Override
    public GLSLArrayType ensureValid() {
        this.elementType.ensureValid();
        return this;
    }

    @Override
    public Stream<GLSLField<? extends GLSLBasicType>> basicFields(@NonNull String name) {
        return IntStream.range(0, this.size)
                .mapToObj(i -> name + '[' + i + ']')
                .flatMap(this.elementType::basicFields);
    }
}
