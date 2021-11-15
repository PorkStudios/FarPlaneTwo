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

package net.daporkchop.fp2.gl.opengl.attribute;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BaseAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.BaseAttributeFormat;
import net.daporkchop.fp2.gl.opengl.OpenGL;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Common parent class for attribute buffer implementations.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseAttributeBufferImpl<S, F extends BaseAttributeFormatImpl<S, ?>, F_EXTERNAL extends BaseAttributeFormat<S>> implements BaseAttributeBuffer<S, F_EXTERNAL> {
    protected final OpenGL gl;
    private final F formatImpl;

    public BaseAttributeBufferImpl(@NonNull F format) {
        this.gl = format.gl();
        this.formatImpl = format;
    }

    @Override
    @Deprecated
    public F_EXTERNAL format() {
        return uncheckedCast(this.formatImpl);
    }
}
