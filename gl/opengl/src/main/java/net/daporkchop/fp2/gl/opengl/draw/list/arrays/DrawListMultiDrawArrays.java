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

package net.daporkchop.fp2.gl.opengl.draw.list.arrays;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.bitset.AbstractGLBitSet;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListImpl;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collections;
import java.util.Map;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class DrawListMultiDrawArrays extends DrawListImpl<DrawCommandArrays, DrawBindingImpl> {
    protected long firstAddr;
    protected long countAddr;

    @Getter(lazy = true)
    private final AttributeFormat<CountAttribute> format = this.gl.createAttributeFormat(CountAttribute.class)
            .useFor(AttributeUsage.TRANSFORM_INPUT)
            .useFor(AttributeUsage.TRANSFORM_OUTPUT)
            .build();

    @Getter(lazy = true)
    private final InterleavedAttributeBufferImpl<?, CountAttribute> readBuffer = (InterleavedAttributeBufferImpl<?, CountAttribute>) this.format().createBuffer(BufferUsage.STREAM_DRAW);
    @Getter(lazy = true)
    private final InterleavedAttributeBufferImpl<?, CountAttribute> writeBuffer = (InterleavedAttributeBufferImpl<?, CountAttribute>) this.format().createBuffer(BufferUsage.STREAM_READ);

    public DrawListMultiDrawArrays(@NonNull DrawListBuilderImpl builder) {
        super(builder);
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        this.firstAddr = this.alloc.realloc(this.firstAddr, newCapacity * (long) INT_SIZE);
        this.countAddr = this.alloc.realloc(this.countAddr, newCapacity * (long) INT_SIZE);

        if (newCapacity > oldCapacity) { //zero out the commands
            PUnsafe.setMemory(this.firstAddr + oldCapacity * (long) INT_SIZE, (newCapacity - oldCapacity) * (long) INT_SIZE, (byte) 0);
            PUnsafe.setMemory(this.countAddr + oldCapacity * (long) INT_SIZE, (newCapacity - oldCapacity) * (long) INT_SIZE, (byte) 0);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.firstAddr);
        this.alloc.free(this.countAddr);
    }

    @Override
    public void set(int index, @NonNull DrawCommandArrays command) {
        checkIndex(this.capacity, index);

        PUnsafe.putInt(this.firstAddr + index * (long) INT_SIZE, command.first());
        PUnsafe.putInt(this.countAddr + index * (long) INT_SIZE, command.count());
    }

    @Override
    public DrawCommandArrays get(int index) {
        checkIndex(this.capacity, index);

        return new DrawCommandArrays(
                PUnsafe.getInt(this.firstAddr + index * (long) INT_SIZE),
                PUnsafe.getInt(this.countAddr + index * (long) INT_SIZE));
    }

    @Override
    public void clear(int index) {
        PUnsafe.putInt(this.countAddr + checkIndex(this.capacity, index) * (long) INT_SIZE, 0);
    }

    @Override
    public Map<StateValueProperty<?>, Object> stateProperties0() {
        return Collections.emptyMap();
    }

    @Override
    public void draw0(GLAPI api, int mode) {
        api.glMultiDrawArrays(mode, this.firstAddr, this.countAddr, this.capacity);
    }

    @Override
    public void draw0(GLAPI api, int mode, AbstractGLBitSet selectionMask) {
        long dstCounts = PUnsafe.allocateMemory(this.capacity * (long) INT_SIZE);
        try {
            selectionMask.mapClient(this.capacity, (bitsBase, bitsOffset) -> {
                long srcCountAddr = this.countAddr;
                long dstCountAddr = dstCounts;
                for (int bitIndex = 0; bitIndex < this.capacity; bitsOffset += INT_SIZE) {
                    int word = PUnsafe.getInt(bitsBase, bitsOffset);

                    for (int mask = 1; mask != 0 && bitIndex < this.capacity; mask <<= 1, bitIndex++, srcCountAddr += INT_SIZE, dstCountAddr += INT_SIZE) {
                        PUnsafe.putInt(dstCountAddr, PUnsafe.getInt(srcCountAddr) & (-(word & mask) >> 31));
                        PUnsafe.putInt(dstCountAddr, 3);
                    }
                }
            });

            api.glMultiDrawArrays(mode, this.firstAddr, dstCounts, this.capacity);
        } finally {
            PUnsafe.freeMemory(dstCounts);
        }
    }

    @Override
    public void drawSelected0_pre(GLAPI api, int mode) {
        this.readBuffer().buffer().upload(this.countAddr, this.capacity * (long) INT_SIZE);
        this.writeBuffer().buffer().capacity(this.capacity * (long) INT_SIZE);
    }

    @Override
    public void drawSelected0_post(GLAPI api, int mode) {
        this.writeBuffer().buffer().map(true, false, countsAddr -> api.glMultiDrawArrays(mode, this.firstAddr, countsAddr, this.capacity));
    }

    @Override
    public TransformLayoutBuilder configureTransformLayoutForSelection(@NonNull TransformLayoutBuilder builder) {
        return builder.withInput(this.format()).withOutput(this.format());
    }

    @Override
    public TransformBindingBuilder configureTransformBindingForSelection(@NonNull TransformBindingBuilder builder) {
        return builder.withInput(this.readBuffer()).withOutput(this.writeBuffer());
    }

    @Override
    public TransformShaderBuilder configureTransformShaderForSelection(@NonNull TransformShaderBuilder builder) {
        return builder.include(Identifier.from(OpenGL.OPENGL_NAMESPACE, "draw/list/arrays/DrawListMultiDrawArrays_selection.vert"));
    }

    @Override
    public TransformShaderProgramBuilder configureTransformShaderProgramForSelection(@NonNull TransformShaderProgramBuilder builder) {
        return builder;
    }

    @RequiredArgsConstructor
    public static final class CountAttribute {
        @Attribute
        public final int count_internal;
    }
}
