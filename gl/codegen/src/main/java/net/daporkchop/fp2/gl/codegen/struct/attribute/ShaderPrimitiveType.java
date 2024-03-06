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

package net.daporkchop.fp2.gl.codegen.struct.attribute;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum ShaderPrimitiveType {
    UINT(Integer.BYTES, true, false),
    INT(Integer.BYTES, true, true),
    FLOAT(Float.BYTES, false, true),
    ;

    public static ShaderPrimitiveType from(JavaPrimitiveType type) {
        if (type.integer()) {
            return type.signed() ? INT : UINT;
        } else {
            checkArg(type.signed(), type);
            return FLOAT;
        }
    }

    private final int size;
    private final boolean integer;
    private final boolean signed;
}
