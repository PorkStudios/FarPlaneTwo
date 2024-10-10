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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Wrapper around an OpenGL vertex array object.
 *
 * @author DaPorkchop_
 */
public abstract class VertexArrayObject extends GLObject.Normal {
    //Unlike with buffers, i'm putting code for DSA in a separate class. This is because the DSA functions for VAOs only provide
    //  GL_ARB_vertex_attrib_binding-style functionality, which works completely differently from the old code.

    public static VertexArrayObject create(OpenGL gl) {
        if (gl.supports(GLExtension.GL_ARB_direct_state_access) && gl.supports(GLExtension.GL_ARB_vertex_attrib_binding)) {
            return (VertexArrayObject) (Object) new DSA(gl);
        } else {
            return (VertexArrayObject) (Object) new Basic(gl);
        }
    }

    public static Builder builder(OpenGL gl) {
        return new Builder(gl);
    }

    protected int enabledAttributes;

    VertexArrayObject(OpenGL gl, int id) {
        super(gl, id);
    }

    /**
     * Configures this vertex array with the given vertex attribute formats and buffers.
     *
     * @param attributes the vertex attribute formats
     * @param buffers    the vertex attribute buffers
     */
    public abstract void configure(VertexAttributeFormat[] attributes, VertexArrayVertexBuffer[] buffers);

    /**
     * Binds the given buffer to this vertex array as the elements buffer.
     *
     * @param buffer the buffer to bind
     */
    public abstract void setElementsBuffer(GLBuffer buffer);

    /**
     * Executes the given action with this vertex array bound.
     * <p>
     * This will restore the previously bound vertex array when the operation completes.
     *
     * @param action the action to run
     */
    public final void bindPreserving(Runnable action) {
        this.checkOpen();
        int old = this.gl.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.gl.glBindVertexArray(this.id);
            action.run();
        } finally {
            this.gl.glBindVertexArray(old);
        }
    }

    /**
     * Immediately binds this vertex array to the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound vertex array when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this vertex array bound will not cause future issues.
     */
    public final void bindUnsafe() {
        this.checkOpen();
        this.gl.glBindVertexArray(this.id);
    }

    /**
     * Executes the given action with this vertex array bound.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound vertex array when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this vertex array bound will not cause future issues.
     *
     * @param action the action to run
     */
    public final void bindUnsafe(Runnable action) {
        this.checkOpen();
        this.gl.glBindVertexArray(this.id);
        action.run();
    }

    /**
     * Executes the given action with this vertex array bound.
     *
     * @param action the action to run
     */
    public final void bind(Runnable action) {
        if (OpenGL.PRESERVE_VAO_BINDINGS_IN_METHODS) {
            this.bindPreserving(action);
        } else {
            this.bindUnsafe(action);
        }
    }

    @Override
    protected final void delete() {
        this.gl.glDeleteVertexArray(this.id);
    }

    @Override
    public void setDebugLabel(@NonNull CharSequence label) {
        this.gl.glObjectLabel(GL_VERTEX_ARRAY, this.id, label);
    }

    @Override
    public String getDebugLabel() {
        return this.gl.glGetObjectLabel(GL_VERTEX_ARRAY, this.id);
    }

    /**
     * @author DaPorkchop_
     */
    private static final class Basic extends VertexArrayObject {
        Basic(OpenGL gl) {
            super(gl, gl.glGenVertexArray());
        }

        private void configureDivisor(int index, int divisor) {
            if (this.gl.supports(GLExtension.GL_ARB_instanced_arrays)) {
                this.gl.glVertexAttribDivisor(index, divisor);
            } else if (divisor != 0) {
                throw new UnsupportedOperationException(this.gl.unsupportedMsg(GLExtension.GL_ARB_instanced_arrays));
            }
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

                        //enable the attribute array if it wasn't already
                        if (index >= this.enabledAttributes) {
                            this.gl.glEnableVertexAttribArray(index);
                        }

                        if (attrib.integer()) {
                            this.gl.glVertexAttribIPointer(index, attrib.size(), attrib.type(), buffer.stride(), attrib.offset() + buffer.offset());
                        } else {
                            this.gl.glVertexAttribPointer(index, attrib.size(), attrib.type(), attrib.normalized(), buffer.stride(), attrib.offset() + buffer.offset());
                        }
                        this.configureDivisor(index, buffer.divisor());
                    }

                    //disable any attribute arrays which were enabled before but aren't anymore
                    for (int index = attributes.length; index < this.enabledAttributes; index++) {
                        this.gl.glDisableVertexAttribArray(index);
                    }
                    this.enabledAttributes = attributes.length;
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

        private void configureDivisor(int index, int divisor) {
            if (this.gl.supports(GLExtension.GL_ARB_instanced_arrays)) {
                this.gl.glVertexArrayBindingDivisor(this.id, index, divisor);
            } else if (divisor != 0) {
                throw new UnsupportedOperationException(this.gl.unsupportedMsg(GLExtension.GL_ARB_instanced_arrays));
            }
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
            this.checkOpen();
            checkArg(attributes.length == buffers.length);

            int bindingIndex = -1;
            VertexArrayVertexBuffer lastBuffer = null;

            for (int attribIndex = 0; attribIndex < attributes.length; attribIndex++) {
                VertexArrayVertexBuffer buffer = buffers[attribIndex];
                if (buffer != lastBuffer) {
                    lastBuffer = buffer;

                    bindingIndex++;
                    this.gl.glVertexArrayVertexBuffer(this.id, bindingIndex, buffer.buffer(), buffer.offset(), buffer.stride());
                    this.configureDivisor(bindingIndex, buffer.divisor());
                }

                VertexAttributeFormat attrib = attributes[attribIndex];

                //enable the attribute array if it wasn't already
                if (attribIndex >= this.enabledAttributes) {
                    this.gl.glEnableVertexArrayAttrib(this.id, attribIndex);
                }
                configureFormat(this.gl, this.id, attribIndex, attrib);
                this.gl.glVertexArrayAttribBinding(this.id, attribIndex, bindingIndex);
            }

            //disable any attribute arrays which were enabled before but aren't anymore
            for (int attribIndex = attributes.length; attribIndex < this.enabledAttributes; attribIndex++) {
                this.gl.glDisableVertexArrayAttrib(this.id, attribIndex);
            }
            this.enabledAttributes = attributes.length;
        }

        @Override
        public void setElementsBuffer(GLBuffer buffer) {
            this.checkOpen();
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

        public Builder buffer(AttributeBuffer<?> buffer) {
            return this.buffer(buffer, 0);
        }

        public Builder buffer(AttributeBuffer<?> buffer, @NotNegative int divisor) {
            this.formats.addAll(Arrays.asList(buffer.format().vertexAttributeFormats()));
            this.buffers.addAll(Arrays.asList(buffer.buffers(divisor)));
            return this;
        }

        public Builder elementBuffer(GLBuffer elementBuffer) {
            this.elementsBuffer = elementBuffer;
            return this;
        }

        public Builder elementBuffer(IndexBuffer indexBuffer) {
            return this.elementBuffer(indexBuffer.elementsBuffer());
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
