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

package net.daporkchop.fp2.gl.opengl.attribute.struct.type;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class GLSLAggregateType implements GLSLType {
    public static GLSLAggregateType vec2(@NonNull GLSLPrimitiveType type) {
        return new GLSLAggregateType(type, "vec2", 2);
    }

    public static GLSLAggregateType vec3(@NonNull GLSLPrimitiveType type) {
        return new GLSLAggregateType(type, "vec3", 3);
    }

    public static GLSLAggregateType vec4(@NonNull GLSLPrimitiveType type) {
        return new GLSLAggregateType(type, "vec4", 4);
    }

    @NonNull
    private final GLSLPrimitiveType componentType;
    @NonNull
    private final String typeName;

    private final int components;

    @Override
    public String declaration(@NonNull String fieldName) {
        return this.componentType.typePrefix() + this.typeName + ' ' + fieldName;
    }
}
