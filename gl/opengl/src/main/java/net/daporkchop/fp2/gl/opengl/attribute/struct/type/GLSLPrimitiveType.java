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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

import static net.daporkchop.fp2.common.util.TypeSize.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum GLSLPrimitiveType implements GLSLBasicType {
    INVALID("<invalid>", -1, false, false) {
        @Override
        public String typePrefix() {
            throw new UnsupportedOperationException(this.toString());
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException(this.toString());
        }

        @Override
        public boolean integer() {
            throw new UnsupportedOperationException(this.toString());
        }

        @Override
        public boolean signed() {
            throw new UnsupportedOperationException(this.toString());
        }

        @Override
        public String declaration(@NonNull String fieldName) {
            throw new UnsupportedOperationException(this.toString());
        }
    },
    INT("i", INT_SIZE, true, true),
    UINT("u", INT_SIZE, true, false),
    FLOAT("", FLOAT_SIZE, false,true);

    @NonNull
    private final String typePrefix;

    private final int size;
    private final boolean integer;
    private final boolean signed;

    @Override
    public String declaration(@NonNull String fieldName) {
        return this.name().toLowerCase(Locale.ROOT) + ' ' + fieldName;
    }

    @Override
    public GLSLPrimitiveType primitive() {
        return this;
    }

    @Override
    public GLSLPrimitiveType withPrimitive(@NonNull GLSLPrimitiveType primitive) {
        return primitive;
    }

    @Override
    public int requiredVertexAttributeSlots() {
        return 1;
    }

    @Override
    public GLSLPrimitiveType ensureValid() {
        GLSLBasicType.super.ensureValid();
        return this;
    }

    @Override
    public GLSLType intern() {
        return this;
    }
}
