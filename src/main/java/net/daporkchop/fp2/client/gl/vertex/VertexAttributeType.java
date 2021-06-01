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

package net.daporkchop.fp2.client.gl.vertex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * The different primitive types allowed to be used as a vertex attribute value.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public enum VertexAttributeType {
    BYTE(GL_BYTE, BYTE_SIZE),
    UNSIGNED_BYTE(GL_UNSIGNED_BYTE, BYTE_SIZE),
    SHORT(GL_SHORT, SHORT_SIZE),
    UNSIGNED_SHORT(GL_UNSIGNED_SHORT, SHORT_SIZE),
    INT(GL_INT, INT_SIZE),
    UNSIGNED_INT(GL_UNSIGNED_INT, INT_SIZE),
    FLOAT(GL_FLOAT, FLOAT_SIZE),
    INT_2_10_10_10_REV(GL_INT_2_10_10_10_REV, INT_SIZE) {
        @Override
        public int size(int components) {
            return INT_SIZE; //this type always uses the full 32 bits
        }
    },
    UNSIGNED_INT_2_10_10_10_REV(GL_UNSIGNED_INT_2_10_10_10_REV, INT_SIZE) {
        @Override
        public int size(int components) {
            return INT_SIZE; //this type always uses the full 32 bits
        }
    };

    @Getter
    private final int glType;
    private final int size;

    /**
     * Gets the size of a vertex attribute using this type with the given number of components.
     *
     * @param components the vertex attribute component count
     * @return the size of a vertex attribute using this type with the given number of components
     */
    public int size(int components) {
        return this.size * components;
    }
}
