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

package net.daporkchop.fp2.gl.opengl;

import lombok.NonNull;

import java.nio.ByteBuffer;

/**
 * Provides access to the OpenGL API.
 *
 * @author DaPorkchop_
 */
public interface GLAPI {
    //
    //
    // UTILITIES
    //
    //

    GLVersion version();

    //
    //
    // OpenGL 1.1
    //
    //

    int glGetError();

    int glGetInteger(int pname);

    String glGetString(int pname);

    void glDrawArrays(int mode, int first, int count);

    void glDrawElements(int mode, int count, int type, long indices);

    void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices);

    int glGenTexture();

    void glDeleteTexture(int texture);

    //
    //
    // OpenGL 1.4
    //
    //

    void glMultiDrawArrays(int mode, long first, long count, int drawcount);

    //
    //
    // OpenGL 1.5
    //
    //

    int glGenBuffer();

    void glDeleteBuffer(int buffer);

    void glBindBuffer(int target, int buffer);

    void glBufferData(int target, long data_size, long data, int usage);

    void glBufferData(int target, @NonNull ByteBuffer data, int usage);

    void glBufferSubData(int target, long offset, long data_size, long data);

    void glBufferSubData(int target, long offset, @NonNull ByteBuffer data);

    void glGetBufferSubData(int target, long offset, long data_size, long data);

    void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data);

    long glMapBuffer(int target, int usage);

    void glUnmapBuffer(int target);

    //
    //
    // OpenGL 2.0
    //
    //

    int glCreateShader(int type);

    void glDeleteShader(int shader);

    void glShaderSource(int shader, @NonNull CharSequence... source);

    void glCompileShader(int shader);

    int glGetShaderi(int shader, int pname);

    String glGetShaderInfoLog(int shader);

    int glCreateProgram();

    void glDeleteProgram(int program);

    void glAttachShader(int program, int shader);

    void glDetachShader(int program, int shader);

    void glLinkProgram(int program);

    int glGetProgrami(int program, int pname);

    String glGetProgramInfoLog(int program);

    void glUseProgram(int program);

    void glEnableVertexAttribArray(int index);

    void glDisableVertexArray(int index);

    void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long buffer_buffer_offset);

    void glBindAttribLocation(int program, int index, @NonNull CharSequence name);

    void glUniform(int location, int v0);

    void glUniform(int location, int v0, int v1);

    void glUniform(int location, int v0, int v1, int v2);

    void glUniform(int location, int v0, int v1, int v2, int v3);

    void glUniform(int location, float v0);

    void glUniform(int location, float v0, float v1);

    void glUniform(int location, float v0, float v1, float v2);

    void glUniform(int location, float v0, float v1, float v2, float v3);

    //
    //
    // OpenGL 3.0
    //
    //

    int glGetInteger(int pname, int idx);

    String glGetString(int pname, int idx);

    int glGenVertexArray();

    void glDeleteVertexArray(int array);

    void glBindVertexArray(int array);

    void glVertexAttribIPointer(int index, int size, int type, int stride, long buffer_buffer_offset);

    void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name);

    void glBindBufferBase(int target, int index, int buffer);

    void glBindBufferRange(int target, int index, int buffer, long offset, long size);

    //
    //
    // OpenGL 3.1
    //
    //

    //GL_ARB_uniform_buffer_object
    int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName);

    //GL_ARB_uniform_buffer_object
    void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding);

    //
    //
    // OpenGL 3.2
    //
    //

    //GL_ARB_draw_elements_base_vertex
    void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex);

    //GL_ARB_draw_elements_base_vertex
    void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex);

    //
    //
    // OpenGL 3.3
    //
    //

    //GL_ARB_instanced_arrays
    void glVertexAttribDivisor(int index, int divisor);

    //
    //
    // OpenGL 4.2
    //
    //

    //GL_ARB_shader_image_load_store
    void glMemoryBarrier(int barriers);

    //
    //
    // OpenGL 4.3
    //
    //

    //GL_ARB_multi_draw_indirect
    void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride);

    //GL_ARB_multi_draw_indirect
    void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride);
}
