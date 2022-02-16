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

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class GLSLTypeFactory {
    @SuppressWarnings("UnstableApiUsage")
    static final Interner<GLSLType> GLSL_TYPE_INTERNER = Interners.newWeakInterner();

    public static GLSLBasicType vec(@NonNull GLSLPrimitiveType primitive, int components) {
        switch (components) {
            case 1:
                return primitive;
            case 2:
            case 3:
            case 4:
                return new GLSLVectorType(primitive, components);
            default:
                throw new IllegalArgumentException("cannot create vector with " + components + " components");
        }
    }

    public static GLSLMatrixType mat(@NonNull GLSLPrimitiveType primitive, int columns, int rows) {
        checkArg(columns >= 2 && columns <= 4 && rows >= 2 && rows <= 4, "cannot create %dx%d matrix", columns, rows);
        return new GLSLMatrixType(primitive, columns, rows);
    }

    public static GLSLArrayType array(@NonNull GLSLType elementType, int length) {
        return new GLSLArrayType(elementType, length);
    }
}
