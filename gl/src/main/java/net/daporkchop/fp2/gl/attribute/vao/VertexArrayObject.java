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

package net.daporkchop.fp2.gl.attribute.vao;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class VertexArrayObject implements AutoCloseable {
    public static VertexArrayObject create(OpenGL gl) {
        if (gl.supports(GLExtension.GL_ARB_direct_state_access)) {
            return (VertexArrayObject) (Object) new DSA(gl);
        } else {
            return (VertexArrayObject) (Object) new Basic(gl);
        }
    }

    public static Builder builder(OpenGL gl) {
        return new Builder(gl);
    }

    protected final OpenGL gl;
    protected final int id;

    /**
     * @return the ID of the corresponding OpenGL Vertex Array Object
     */
    public final int id() {
        return this.id;
    }

    public abstract void setFAttrib(GLBuffer buffer, int index, int size, int type, boolean normalized, int stride, long pointer, int divisor);

    public abstract void setIAttrib(GLBuffer buffer, int index, int size, int type, int stride, long pointer, int divisor);

    public final void configure(NewAttributeBuffer<?> buffer) {
        this.configure(buffer, 0);
    }

    public final void configure(NewAttributeBuffer<?> buffer, @NotNegative int divisor) {
        this.configure(buffer.format().vertexAttributeFormats(), buffer.buffers(divisor));
    }

    public abstract void configure(VertexAttributeFormat[] attributes, VertexArrayVertexBuffer[] buffers);

    public abstract void setElementsBuffer(GLBuffer buffer);

    /**
     * Executes the given action with this vertex array bound.
     *
     * @param action the action to run
     */
    public final void bind(Runnable action) {
        int old = this.gl.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.gl.glBindVertexArray(this.id);
            action.run();
        } finally {
            this.gl.glBindVertexArray(old);
        }
    }

    @Override
    public void close() {
        this.gl.glDeleteVertexArray(this.id);
    }

    /**
     * @author DaPorkchop_
     */
    private static final class Basic extends VertexArrayObject {
        Basic(OpenGL gl) {
            super(gl, gl.glGenVertexArray());
        }

        @Override
        public void setFAttrib(GLBuffer buffer, int index, int size, int type, boolean normalized, int stride, long pointer, int divisor) {
            buffer.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.bind(() -> {
                    this.gl.glEnableVertexAttribArray(index);
                    this.gl.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
                    this.gl.glVertexAttribDivisor(index, divisor); //TODO: throw exception if divisors aren't supported
                });
            });
        }

        @Override
        public void setIAttrib(GLBuffer buffer, int index, int size, int type, int stride, long pointer, int divisor) {
            buffer.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.bind(() -> {
                    this.gl.glEnableVertexAttribArray(index);
                    this.gl.glVertexAttribIPointer(index, size, type, stride, pointer);
                    this.gl.glVertexAttribDivisor(index, divisor);
                });
            });
        }

        @Override
        public void configure(VertexAttributeFormat[] attributes, VertexArrayVertexBuffer[] buffers) {
            checkArg(attributes.length == buffers.length);
            this.bind(() -> {
                int oldBuffer = this.gl.glGetInteger(GL_ARRAY_BUFFER_BINDING);
                int boundBuffer = oldBuffer;
                try {
                    for (int index = 0; index < attributes.length; index++) {
                        VertexAttributeFormat attrib = attributes[index];
                        VertexArrayVertexBuffer buffer = buffers[index];

                        if (buffer.buffer() != boundBuffer) {
                            boundBuffer = buffer.buffer();
                            this.gl.glBindBuffer(GL_ARRAY_BUFFER, buffer.buffer());
                        }

                        this.gl.glEnableVertexAttribArray(index);
                        if (attrib.integer()) {
                            this.gl.glVertexAttribIPointer(index, attrib.size(), attrib.type(), buffer.stride(), attrib.offset() + buffer.offset());
                        } else {
                            this.gl.glVertexAttribPointer(index, attrib.size(), attrib.type(), attrib.normalized(), buffer.stride(), attrib.offset() + buffer.offset());
                        }
                        this.gl.glVertexAttribDivisor(index, buffer.divisor());
                    }
                } finally {
                    this.gl.glBindBuffer(GL_ARRAY_BUFFER, oldBuffer);
                }
            });
        }

        @Override
        public void setElementsBuffer(GLBuffer buffer) {
            this.bind(() -> this.gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer.id()));
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static final class DSA extends VertexArrayObject {
        DSA(OpenGL gl) {
            super(gl, gl.glCreateVertexArray());
        }

        @Override
        public void setFAttrib(GLBuffer buffer, int index, int size, int type, boolean normalized, int stride, long pointer, int divisor) {
            this.gl.glEnableVertexArrayAttrib(this.id, index);
            this.gl.glVertexArrayAttribFormat(this.id, index, size, type, normalized, 0);
            this.gl.glVertexArrayAttribBinding(this.id, index, index);
            this.gl.glVertexArrayVertexBuffer(this.id, index, buffer.id(), pointer, stride);
            this.gl.glVertexArrayBindingDivisor(this.id, index, divisor);
        }

        @Override
        public void setIAttrib(GLBuffer buffer, int index, int size, int type, int stride, long pointer, int divisor) {
            this.gl.glEnableVertexArrayAttrib(this.id, index);
            this.gl.glVertexArrayAttribIFormat(this.id, index, size, type, 0);
            this.gl.glVertexArrayAttribBinding(this.id, index, index);
            this.gl.glVertexArrayVertexBuffer(this.id, index, buffer.id(), pointer, stride);
            this.gl.glVertexArrayBindingDivisor(this.id, index, divisor);
        }

        private static void configureFormat(OpenGL gl, int vaobj, int attribIndex, VertexAttributeFormat attrib) {
            if (attrib.integer()) {
                gl.glVertexArrayAttribIFormat(vaobj, attribIndex, attrib.size(), attrib.type(), attrib.offset());
            } else {
                gl.glVertexArrayAttribFormat(vaobj, attribIndex, attrib.size(), attrib.type(), attrib.normalized(), attrib.offset());
            }
        }

        @Override
        public void configure(VertexAttributeFormat[] attributes, VertexArrayVertexBuffer[] buffers) {
            checkArg(attributes.length == buffers.length);

            int bindingIndex = -1;
            VertexArrayVertexBuffer lastBuffer = null;

            for (int attribIndex = 0; attribIndex < attributes.length; attribIndex++) {
                VertexArrayVertexBuffer buffer = buffers[attribIndex];
                if (buffer != lastBuffer) {
                    lastBuffer = buffer;

                    bindingIndex++;
                    this.gl.glVertexArrayVertexBuffer(this.id, bindingIndex, buffer.buffer(), buffer.offset(), buffer.stride());
                    this.gl.glVertexArrayBindingDivisor(this.id, bindingIndex, buffer.divisor());
                }

                VertexAttributeFormat attrib = attributes[attribIndex];

                this.gl.glEnableVertexArrayAttrib(this.id, attribIndex);
                configureFormat(this.gl, this.id, attribIndex, attrib);
                this.gl.glVertexArrayAttribBinding(this.id, attribIndex, bindingIndex);
            }
        }

        /*private static boolean identityEquals(NewAttributeFormat<?>[] a, NewAttributeFormat<?>[] b) {
            if (a.length != b.length) {
                return false;
            }
            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void configure(NewAttributeFormat<?>... formats) {
            if (identityEquals(formats, this.configuredFormats)) { //already configured with the same vertex formats, do nothing
                return;
            }

            this.configuredFormats = formats = formats.clone();

            int attribIndex = 0;
            int enabledAttrs = this.enabledAttrs;
            for (NewAttributeFormat<?> format : formats) {
                for (VertexAttributeFormat attrib : format.vertexAttributeFormats()) {
                    if (attribIndex <= enabledAttrs) { //the attribute index hasn't been enabled yet
                        this.gl.glEnableVertexArrayAttrib(this.id, attribIndex);
                        enabledAttrs++;
                    }

                    configureFormat(this.gl, this.id, attribIndex, attrib);
                    attribIndex++;
                }
            }

            while (enabledAttrs > attribIndex) { //disable all attribute indices which were previously enabled
                this.gl.glDisableVertexArrayAttrib(this.id, --enabledAttrs);
            }
            this.enabledAttrs = enabledAttrs;

            throw new UnsupportedOperationException(); //TODO
        }

        private static boolean compatible(NewAttributeBuffer<?>[] buffers, NewAttributeFormat<?>[] formats) {
            if (buffers.length != formats.length) {
                return false;
            }
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i].format() != formats[i]) {
                    return false;
                }
            }
            return true;
        }

        private static NewAttributeFormat<?>[] toFormats(NewAttributeBuffer<?>[] buffers) {
            NewAttributeFormat<?>[] formats = new NewAttributeFormat[buffers.length];
            for (int i = 0; i < buffers.length; i++) {
                formats[i] = buffers[i].format();
            }
            return formats;
        }

        @Override
        public void bind(NewAttributeBuffer<?>... buffers) { //TODO: this api doesn't support divisors, lmao
            if (!compatible(buffers, this.configuredFormats)) { //the given buffers use vertex formats incompatible with the currently configured ones
                //configure this VAO for the vertex formats used by the given vertex buffers before proceeding
                this.configure(toFormats(buffers));
            }

            for (NewAttributeBuffer<?> buffer : buffers) {
                for (VertexArrayVertexBuffer vertexBuffer : buffer.buffers(0)) {
                }
            }

            throw new UnsupportedOperationException(); //TODO
        }*/

        @Override
        public void setElementsBuffer(GLBuffer buffer) {
            this.gl.glVertexArrayElementBuffer(this.id, buffer.id());
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    public static final class Builder {
        final OpenGL gl;

        final List<VertexAttributeFormat> formats = new ArrayList<>();
        final List<VertexArrayVertexBuffer> buffers = new ArrayList<>();

        GLBuffer elementsBuffer = null;

        public Builder buffer(NewAttributeBuffer<?> buffer) {
            return this.buffer(buffer, 0);
        }

        public Builder buffer(NewAttributeBuffer<?> buffer, @NotNegative int divisor) {
            this.formats.addAll(Arrays.asList(buffer.format().vertexAttributeFormats()));
            this.buffers.addAll(Arrays.asList(buffer.buffers(divisor)));
            return this;
        }

        public Builder elementBuffer(GLBuffer elementBuffer) {
            this.elementsBuffer = elementBuffer;
            return this;
        }

        public VertexArrayObject build() {
            VertexArrayObject vao = VertexArrayObject.create(this.gl);
            vao.configure(this.formats.toArray(new VertexAttributeFormat[0]), this.buffers.toArray(new VertexArrayVertexBuffer[0]));
            if (this.elementsBuffer != null) {
                vao.setElementsBuffer(this.elementsBuffer);
            }
            return vao;
        }
    }
}
