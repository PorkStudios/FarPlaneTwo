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

package net.daporkchop.fp2.gl.opengl;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLVersion;

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

    GLVersion determineVersion();

    //
    //
    // OpenGL 1.1
    //
    //

    void glEnable(int cap);

    void glDisable(int cap);

    int glGetError();

    boolean glGetBoolean(int pname);

    void glGetBoolean(int pname, long data);

    int glGetInteger(int pname);

    void glGetInteger(int pname, long data);

    float glGetFloat(int pname);

    void glGetFloat(int pname, long data);

    double glGetDouble(int pname);

    void glGetDouble(int pname, long data);

    String glGetString(int pname);

    void glDrawArrays(int mode, int first, int count);

    void glDrawElements(int mode, int count, int type, long indices);

    void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices);

    int glGenTexture();

    void glDeleteTexture(int texture);

    void glBindTexture(int target, int texture);

    void glTexParameter(int target, int pname, int param);

    void glTexParameter(int target, int pname, float param);

    int glGetTexParameterInteger(int target, int pname);

    float glGetTexParameterFloat(int target, int pname);

    void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data);

    void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data);

    void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data);

    void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data);

    void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data);

    void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data);

    void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data);

    void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data);

    void glClear(int mask);

    void glClearColor(float red, float green, float blue, float alpha);

    void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void glClearDepth(double depth);

    void glDepthFunc(int func);

    void glDepthMask(boolean flag);

    void glClearStencil(int s);

    void glStencilFunc(int func, int ref, int mask);

    void glStencilMask(int mask);

    void glStencilOp(int sfail, int dpfail, int dppass);

    void glPushClientAttrib(int mask);

    void glPopClientAttrib();

    void glPushAttrib(int mask);

    void glPopAttrib();

    //
    //
    // OpenGL 1.2
    //
    //

    void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, long data);

    void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data);

    void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data);

    void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data);

    //
    //
    // OpenGL 1.3
    //
    //

    void glActiveTexture(int texture);

    //
    //
    // OpenGL 1.4
    //
    //

    void glMultiDrawArrays(int mode, long first, long count, int drawcount);

    void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount);

    void glBlendColor(float red, float green, float blue, float alpha);

    void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);

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

    void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);

    void glBindAttribLocation(int program, int index, @NonNull CharSequence name);

    int glGetUniformLocation(int program, @NonNull CharSequence name);

    void glUniform(int location, int v0);

    void glUniform(int location, int v0, int v1);

    void glUniform(int location, int v0, int v1, int v2);

    void glUniform(int location, int v0, int v1, int v2, int v3);

    void glUniform(int location, float v0);

    void glUniform(int location, float v0, float v1);

    void glUniform(int location, float v0, float v1, float v2);

    void glUniform(int location, float v0, float v1, float v2, float v3);

    void glBlendEquationSeparate(int modeRGB, int modeAlpha);

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

    void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer);

    void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name);

    void glBindBufferBase(int target, int index, int buffer);

    void glBindBufferRange(int target, int index, int buffer, long offset, long size);

    void glBeginTransformFeedback(int primitiveMode);

    void glEndTransformFeedback();

    void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode);

    //
    //
    // OpenGL 3.1
    //
    //

    //GL_ARB_copy_buffer
    void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size);

    //GL_ARB_texture_buffer_object
    void glTexBuffer(int target, int internalFormat, int buffer);

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

    //GL_ARB_sampler_objects
    int glGenSampler();

    //GL_ARB_sampler_objects
    void glDeleteSampler(int sampler);

    //GL_ARB_sampler_objects
    void glBindSampler(int unit, int sampler);

    //GL_ARB_sampler_objects
    void glSamplerParameter(int sampler, int pname, int param);

    //GL_ARB_sampler_objects
    void glSamplerParameter(int sampler, int pname, float param);

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

    //GL_ARB_program_interface_query
    int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name);

    //GL_ARB_shader_storage_buffer_object
    void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding);
}
