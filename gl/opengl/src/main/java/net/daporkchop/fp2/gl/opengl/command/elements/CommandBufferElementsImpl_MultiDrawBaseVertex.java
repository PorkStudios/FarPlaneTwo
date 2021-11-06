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

package net.daporkchop.fp2.gl.opengl.command.elements;

import lombok.NonNull;
import net.daporkchop.fp2.gl.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.binding.DrawMode;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.command.DrawCommandBufferBuilderImpl;
import net.daporkchop.fp2.gl.opengl.command.DrawCommandBufferImpl;
import net.daporkchop.fp2.gl.opengl.binding.DrawBindingIndexedImpl;
import net.daporkchop.fp2.gl.opengl.index.IndexFormatImpl;
import net.daporkchop.fp2.gl.opengl.shader.DrawShaderProgramImpl;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class CommandBufferElementsImpl_MultiDrawBaseVertex extends DrawCommandBufferImpl<DrawCommandIndexed, DrawBindingIndexedImpl> {
    protected long countAddr;
    protected long indicesAddr;
    protected long basevertexAddr;

    protected final int indexType;
    protected final int indexShift;

    public CommandBufferElementsImpl_MultiDrawBaseVertex(@NonNull DrawCommandBufferBuilderImpl builder) {
        super(builder);

        IndexFormatImpl format = this.binding.indices().format();
        this.indexType = GLEnumUtil.from(format.type());

        int indexSize = format.size();
        checkArg(BinMath.isPow2(positive(indexSize, "indexSize")), "indexSize (%d) is not a power of two!", indexSize);
        this.indexShift = Integer.numberOfTrailingZeros(indexSize);
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        this.countAddr = this.alloc.realloc(this.countAddr, newCapacity * (long) INT_SIZE);
        this.indicesAddr = this.alloc.realloc(this.indicesAddr, newCapacity * (long) LONG_SIZE);
        this.basevertexAddr = this.alloc.realloc(this.basevertexAddr, newCapacity * (long) INT_SIZE);

        if (newCapacity > oldCapacity) { //zero out the commands
            PUnsafe.setMemory(this.countAddr + oldCapacity * (long) INT_SIZE, (newCapacity - oldCapacity) * (long) INT_SIZE, (byte) 0);
            PUnsafe.setMemory(this.indicesAddr + oldCapacity * (long) LONG_SIZE, (newCapacity - oldCapacity) * (long) LONG_SIZE, (byte) 0);
            PUnsafe.setMemory(this.basevertexAddr + oldCapacity * (long) INT_SIZE, (newCapacity - oldCapacity) * (long) INT_SIZE, (byte) 0);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.countAddr);
        this.alloc.free(this.indicesAddr);
        this.alloc.free(this.basevertexAddr);
    }

    @Override
    public void set(int index, @NonNull DrawCommandIndexed command) {
        checkIndex(this.capacity, index);

        PUnsafe.putInt(this.countAddr + index * (long) INT_SIZE, command.count());
        PUnsafe.putLong(this.indicesAddr + index * (long) INT_SIZE, (long) command.firstIndex() << this.indexShift);
        PUnsafe.putInt(this.basevertexAddr + index * (long) INT_SIZE, command.baseVertex());
    }

    @Override
    public DrawCommandIndexed get(int index) {
        checkIndex(this.capacity, index);

        return new DrawCommandIndexed(
                toInt(PUnsafe.getLong(this.indicesAddr + index * (long) INT_SIZE) >>> this.indexShift),
                PUnsafe.getInt(this.countAddr + index * (long) INT_SIZE),
                PUnsafe.getInt(this.basevertexAddr + index * (long) INT_SIZE));
    }

    @Override
    public void clear(int index) {
        PUnsafe.putInt(this.countAddr + checkIndex(this.capacity, index) * (long) INT_SIZE, 0);
    }

    @Override
    public void execute(@NonNull DrawMode mode, @NonNull DrawShaderProgram shader) {
        ((DrawShaderProgramImpl) shader).bind(() -> this.binding.bind(() -> {
            this.api.glMultiDrawElementsBaseVertex(GLEnumUtil.from(mode), this.countAddr, this.indexType, this.indicesAddr, this.capacity, this.basevertexAddr);
        }));
    }
}
