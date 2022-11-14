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
 */

package net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.draw.list.DrawCommandIndexed;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.SimpleDrawListImpl;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;

import java.util.Collections;
import java.util.Map;

import static net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect.MultiDrawElementsIndirect.*;

/**
 * @author DaPorkchop_
 */
public class ShaderSelectedDrawListMultiDrawElementsIndirect extends DrawListMultiDrawElementsIndirect implements SimpleDrawListImpl.ShaderSelected<DrawCommandIndexed> {
    protected final AttributeFormat<Command> format;
    protected final InterleavedAttributeBufferImpl<?, Command> srcBuffer;
    protected final InterleavedAttributeBufferImpl<?, Command> dstBuffer;

    public ShaderSelectedDrawListMultiDrawElementsIndirect(@NonNull DrawListBuilderImpl builder) {
        super(builder);

        this.format = this.gl().createAttributeFormat(Command.class)
                .useFor(AttributeUsage.TRANSFORM_INPUT, AttributeUsage.TRANSFORM_OUTPUT)
                .build();

        this.srcBuffer = (InterleavedAttributeBufferImpl<?, Command>) this.format.createBuffer(BufferUsage.STREAM_DRAW);
        this.dstBuffer = (InterleavedAttributeBufferImpl<?, Command>) this.format.createBuffer(BufferUsage.STREAM_COPY);
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        super.resize0(oldCapacity, newCapacity);

        this.srcBuffer.capacity(newCapacity);
        this.dstBuffer.capacity(newCapacity);
    }

    @Override
    public void close() {
        super.close();

        this.srcBuffer.close();
        this.dstBuffer.close();
    }

    @Override
    public TransformLayoutBuilder configureTransformLayoutForSelection(@NonNull TransformLayoutBuilder builder) {
        return builder.withInput(this.format).withOutput(this.format);
    }

    @Override
    public TransformBindingBuilder configureTransformBindingForSelection(@NonNull TransformBindingBuilder builder) {
        return builder.withInput(this.srcBuffer).withOutput(this.dstBuffer);
    }

    @Override
    public TransformShaderBuilder configureTransformShaderForSelection(@NonNull TransformShaderBuilder builder) {
        return builder.include(Identifier.from(OpenGL.OPENGL_NAMESPACE, "draw/list/elements/multidrawindirect/ShaderSelectedDrawListMultiDrawElementsIndirect_selection.vert"));
    }

    @Override
    public TransformShaderProgramBuilder configureTransformShaderProgramForSelection(@NonNull TransformShaderProgramBuilder builder) {
        return builder;
    }

    @Override
    public Map<StateValueProperty<?>, Object> configureStateForPrepare0(@NonNull State state) {
        return Collections.emptyMap();
    }

    @Override
    public void prepare0(GLAPI api, int mode) {
        this.srcBuffer.buffer().upload(this.commandsAddr, this.capacity * _SIZE);
        this.dstBuffer.buffer().capacity(this.capacity * _SIZE);
    }

    @Override
    public Map<StateValueProperty<?>, Object> configureStateForTransform0(@NonNull State state) {
        return Collections.emptyMap();
    }

    @Override
    public int transformCount0() {
        return this.capacity;
    }

    @Override
    public Map<StateValueProperty<?>, Object> configureStateForDrawSelected0(@NonNull State state) {
        return Collections.singletonMap(StateProperties.BOUND_BUFFER.get(BufferTarget.DRAW_INDIRECT_BUFFER), this.dstBuffer.buffer().id());
    }

    @Override
    public void drawSelected0(GLAPI api, int mode) {
        api.glMultiDrawElementsIndirect(mode, this.indexType, 0L, this.capacity, 0);
    }

    /**
     * A MultiDrawElementsIndirect command.
     *
     * @author DaPorkchop_
     */
    @Attribute(sort = 0, name = "count", typeScalar = @ScalarType(int.class))
    @Attribute(sort = 1, name = "instanceCount", typeScalar = @ScalarType(int.class))
    @Attribute(sort = 2, name = "firstIndex", typeScalar = @ScalarType(int.class))
    @Attribute(sort = 3, name = "baseVertex", typeScalar = @ScalarType(int.class))
    @Attribute(sort = 4, name = "baseInstance", typeScalar = @ScalarType(int.class))
    public interface Command extends AttributeStruct {
        @AttributeSetter
        Command count(int count);

        @AttributeSetter
        Command instanceCount(int instanceCount);

        @AttributeSetter
        Command firstIndex(int firstIndex);

        @AttributeSetter
        Command baseVertex(int baseVertex);

        @AttributeSetter
        Command baseInstance(int baseInstance);
    }
}
