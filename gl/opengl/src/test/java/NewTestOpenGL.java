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

import lombok.SneakyThrows;
import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayObject;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.Shader;
import net.daporkchop.fp2.gl.shader.ShaderType;

import java.io.InputStream;
import java.util.function.BooleanSupplier;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class NewTestOpenGL {
    public static final int WINDOW_SIZE_W = 512;
    public static final int WINDOW_SIZE_H = WINDOW_SIZE_W;

    public static void run(BooleanSupplier closeRequested, Runnable swapAndSync) {
        System.setProperty("fp2.gl.opengl.debug", "true");

        run(OpenGL.forCurrent(), closeRequested, swapAndSync, id -> {
            InputStream in = TestOpenGL.class.getResourceAsStream(id.path());
            if (in != null) {
                return in;
            }
            throw new ResourceNotFoundException(id);
        }, new DirectMemoryAllocator());

        System.gc();
    }

    @SneakyThrows
    private static void run(OpenGL gl, BooleanSupplier closeRequested, Runnable swapAndSync, ResourceProvider resourceProvider, DirectMemoryAllocator alloc) {
        val uniformFormat = NewAttributeFormat.get(gl, TestOpenGL.UniformAttribs.class, AttributeTarget.UBO);
        val uniformBuffer = uniformFormat.createUniformBuffer();
        uniformBuffer.update().scale((byte) 32, (byte) 32).close();

        val vertexFormat = NewAttributeFormat.get(gl, TestOpenGL.LocalAttribs.class, AttributeTarget.VERTEX_ATTRIBUTE);
        val vertexBuffer = vertexFormat.createBuffer(BufferUsage.STATIC_DRAW);
        try (val writer = vertexFormat.createWriter(alloc)) {
            writer.append().pos(16, 16).close();
            writer.append().pos(16, 32).close();
            writer.append().pos(32, 32).close();
            //writer.append().pos(32, 16).close();
            writer.append().pos(32, 32).close();
            writer.append().pos(32, 16).close();
            writer.append().pos(16, 16).close();

            vertexBuffer.set(writer);
        }

        val instanceVertexFormat = NewAttributeFormat.get(gl, TestOpenGL.GlobalAttribs.class, AttributeTarget.VERTEX_ATTRIBUTE);
        val instanceVertexBuffer = instanceVertexFormat.createBuffer(BufferUsage.STATIC_DRAW);
        try (val writer = instanceVertexFormat.createWriter(alloc)) {
            for (int i = 0, color = -1, x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++, color = 0xFF << (i << 3), i++) {
                    writer.append()
                            .offset(x * 32, y * 32)
                            .color(0xFF000000 | color);
                }
            }

            instanceVertexBuffer.set(writer);
        }

        val vao = VertexArrayObject.builder(gl)
                .buffer(vertexBuffer)
                .buffer(instanceVertexBuffer, 1)
                .build();
        //vertexBuffer.configure(0, vao);
        //instanceVertexBuffer.configure(2, vao, 1);

        val vsh = new Shader(gl, ShaderType.VERTEX, resourceProvider, Identifier.from("new_test.vert"));
        val fsh = new Shader(gl, ShaderType.FRAGMENT, resourceProvider, Identifier.from("new_test.frag"));
        val shader = DrawShaderProgram.builder(gl)
                .vertexShader(vsh).fragmentShader(fsh)
                .addUBO(7, "Uniforms")
                .vertexAttributesWithPrefix("a_", vertexFormat)
                .vertexAttributesWithPrefix("a_", instanceVertexFormat)
                .build();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        do {
            gl.glClear(GL_COLOR_BUFFER_BIT);

            gl.glUseProgram(shader.id());
            gl.glBindVertexArray(vao.id());
            gl.glBindBufferBase(GL_UNIFORM_BUFFER, 7, uniformBuffer.buffer().id());
            //gl.glDrawArrays(GL_TRIANGLES, 0, 6);
            gl.glDrawArraysInstanced(GL_TRIANGLES, 0, 6, 4);
            gl.glBindBufferBase(GL_UNIFORM_BUFFER, 7, 0);
            gl.glBindVertexArray(0);
            gl.glUseProgram(0);

            swapAndSync.run();
        } while (!closeRequested.getAsBoolean());
    }
}