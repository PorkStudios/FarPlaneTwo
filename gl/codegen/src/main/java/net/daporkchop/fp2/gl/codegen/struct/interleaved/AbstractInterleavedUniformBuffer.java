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

import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.buffer.BufferAccess;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInterleavedUniformBuffer<STRUCT extends AttributeStruct> extends UniformBuffer<STRUCT> {
    protected AbstractInterleavedUniformBuffer(AttributeFormat<STRUCT> format) {
        super(format);
    }

    @Override
    public final STRUCT update() {
        return this.update(PUnsafe.allocateMemory(this.format.size()));
    }

    protected abstract STRUCT update(long address);

    public final void uploadAndRelease(long address) {
        try {
            //map the buffer using GL_MAP_INVALIDATE_BUFFER_BIT and copy the new data into the mapping.
            //  this lets us explicitly orphan the buffer, and works whether or not the underlying buffer storage is immutable
            this.buffer.map(BufferAccess.WRITE_ONLY, GL_MAP_INVALIDATE_BUFFER_BIT,
                    mapping -> mapping.put(DirectBufferHackery.wrapByte(address, mapping.remaining())));
        } finally {
            PUnsafe.freeMemory(address);
        }
    }
}
