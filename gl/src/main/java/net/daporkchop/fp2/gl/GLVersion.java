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

package net.daporkchop.fp2.gl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

/**
 * All known OpenGL versions.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum GLVersion {
    OpenGL10(1, 0, -1),
    OpenGL11(1, 1, -1),
    OpenGL12(1, 2, -1),
    OpenGL13(1, 3, -1),
    OpenGL14(1, 4, -1),
    OpenGL15(1, 5, -1),
    OpenGL20(2, 0, 110),
    OpenGL21(2, 1, 120),
    OpenGL30(3, 0, 130),
    OpenGL31(3, 1, 140),
    OpenGL32(3, 2, 150),
    OpenGL33(3, 3, 330),
    OpenGL40(4, 0, 400),
    OpenGL41(4, 1, 410),
    OpenGL42(4, 2, 420),
    OpenGL43(4, 3, 430),
    OpenGL44(4, 4, 440),
    OpenGL45(4, 5, 450),
    OpenGL46(4, 6, 460);

    private final int major;
    private final int minor;

    private final int glsl;

    @Override
    public String toString() {
        return "OpenGL " + this.major + '.' + this.minor;
    }

    /**
     * @return a {@link GLExtensionSet} containing every {@link GLExtension} which is core in this OpenGL version
     */
    public GLExtensionSet coreExtensions() {
        return Stream.of(GLExtension.values()).filter(extension -> extension.core(this)).collect(GLExtensionSet.toExtensionSet());
    }
}
