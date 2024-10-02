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

package net.daporkchop.fp2.gl;

import lombok.NonNull;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputCallback;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Wrapper around an existing OpenGL context which pretends to implement an older OpenGL version or fewer extensions.
 *
 * @author DaPorkchop_
 */
final class LegacyOpenGL extends OpenGL {
    static OpenGL wrap(@NonNull OpenGL delegate, @NonNull GLVersion version, @NonNull GLExtensionSet extensions, @NonNull GLProfile profile, boolean forwardCompatibility) {
        //TODO: better argument validation

        //avoid having more than one layer of wrapping over a single context
        while (delegate instanceof LegacyOpenGL) {
            delegate = ((LegacyOpenGL) delegate).delegate;
        }

        //dirty hack to create a context with a valid limits instance, as creating it in the OpenGL constructor would mean we'd be trying to access delegate
        //  before it was initialized
        Limits limits = new Limits(new LegacyOpenGL(delegate, version, extensions, profile, forwardCompatibility, null));
        return new LegacyOpenGL(delegate, version, extensions, profile, forwardCompatibility, limits);
    }

    private final OpenGL delegate;

    private LegacyOpenGL(@NonNull OpenGL delegate, @NonNull GLVersion version, @NonNull GLExtensionSet extensions, @NonNull GLProfile profile, boolean forwardCompatibility, Limits limits) {
        super(version, extensions, profile, forwardCompatibility, limits);
        this.delegate = delegate;
    }

    @Override
    protected GLVersion determineVersion() {
        throw new UnsupportedOperationException();
    }

    //
    //
    // OpenGL 1.1
    //
    //

    @Override
    public void glEnable(int cap) {
        this.delegate.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        this.delegate.glDisable(cap);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return this.delegate.glIsEnabled(cap);
    }

    @Override
    public int glGetError() {
        return this.delegate.glGetError();
    }

    @Override
    public void glFlush() {
        this.delegate.glFlush();
    }

    @Override
    public boolean glGetBoolean(int pname) {
        return this.delegate.glGetBoolean(pname);
    }

    @Override
    public void glGetBoolean(int pname, @NonNull ByteBuffer data) {
        this.delegate.glGetBoolean(pname, data);
    }

    @Override
    public int glGetInteger(int pname) {
        return this.delegate.glGetInteger(pname);
    }

    @Override
    public void glGetInteger(int pname, @NonNull IntBuffer data) {
        this.delegate.glGetInteger(pname, data);
    }

    @Override
    public float glGetFloat(int pname) {
        return this.delegate.glGetFloat(pname);
    }

    @Override
    public void glGetFloat(int pname, @NonNull FloatBuffer data) {
        this.delegate.glGetFloat(pname, data);
    }

    @Override
    public double glGetDouble(int pname) {
        return this.delegate.glGetDouble(pname);
    }

    @Override
    public void glGetDouble(int pname, @NonNull DoubleBuffer data) {
        this.delegate.glGetDouble(pname, data);
    }

    @Override
    public String glGetString(int pname) {
        return this.delegate.glGetString(pname);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        this.delegate.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        this.delegate.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices) {
        this.delegate.glDrawElements(mode, count, type, indices);
    }

    @Override
    public int glGenTexture() {
        return this.delegate.glGenTexture();
    }

    @Override
    public void glDeleteTexture(int texture) {
        this.delegate.glDeleteTexture(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        this.delegate.glBindTexture(target, texture);
    }

    @Override
    public void glTexParameter(int target, int pname, int param) {
        this.delegate.glTexParameter(target, pname, param);
    }

    @Override
    public void glTexParameter(int target, int pname, float param) {
        this.delegate.glTexParameter(target, pname, param);
    }

    @Override
    public int glGetTexParameterInteger(int target, int pname) {
        return this.delegate.glGetTexParameterInteger(target, pname);
    }

    @Override
    public float glGetTexParameterFloat(int target, int pname) {
        return this.delegate.glGetTexParameterFloat(target, pname);
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data) {
        this.delegate.glTexImage1D(target, level, internalformat, width, format, type, data);
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data) {
        this.delegate.glTexImage1D(target, level, internalformat, width, format, type, data);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data) {
        this.delegate.glTexImage2D(target, level, internalformat, width, height, format, type, data);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        this.delegate.glTexImage2D(target, level, internalformat, width, height, format, type, data);
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data) {
        this.delegate.glTexSubImage1D(target, level, xoffset, width, format, type, data);
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data) {
        this.delegate.glTexSubImage1D(target, level, xoffset, width, format, type, data);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data) {
        this.delegate.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        this.delegate.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glClear(int mask) {
        this.delegate.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        this.delegate.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        this.delegate.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glClearDepth(double depth) {
        this.delegate.glClearDepth(depth);
    }

    @Override
    public void glDepthFunc(int func) {
        this.delegate.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        this.delegate.glDepthMask(flag);
    }

    @Override
    public void glClearStencil(int s) {
        this.delegate.glClearStencil(s);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        this.delegate.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask) {
        this.delegate.glStencilMask(mask);
    }

    @Override
    public void glStencilOp(int sfail, int dpfail, int dppass) {
        this.delegate.glStencilOp(sfail, dpfail, dppass);
    }

    @Override
    public void glPushClientAttrib(int mask) {
        super.checkSupported(GLExtension.GL_ARB_compatibility);
        this.delegate.glPushClientAttrib(mask);
    }

    @Override
    public void glPopClientAttrib() {
        super.checkSupported(GLExtension.GL_ARB_compatibility);
        this.delegate.glPopClientAttrib();
    }

    @Override
    public void glPushAttrib(int mask) {
        super.checkSupported(GLExtension.GL_ARB_compatibility);
        this.delegate.glPushAttrib(mask);
    }

    @Override
    public void glPopAttrib() {
        super.checkSupported(GLExtension.GL_ARB_compatibility);
        this.delegate.glPopAttrib();
    }

    //
    //
    // OpenGL 1.2
    //
    //

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, long data) {
        this.delegate.glTexImage3D(target, level, internalformat, width, height, depth, format, type, data);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        this.delegate.glTexImage3D(target, level, internalformat, width, height, depth, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data) {
        this.delegate.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        this.delegate.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }

    //
    //
    // OpenGL 1.3
    //
    //

    @Override
    public void glActiveTexture(int texture) {
        this.delegate.glActiveTexture(texture);
    }

    //
    //
    // OpenGL 1.4
    //
    //

    @Override
    public void glMultiDrawArrays(int mode, long first, long count, int drawcount) {
        this.delegate.glMultiDrawArrays(mode, first, count, drawcount);
    }

    @Override
    public void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount) {
        this.delegate.glMultiDrawElements(mode, count, type, indices, drawcount);
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        this.delegate.glBlendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        this.delegate.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    //
    //
    // OpenGL 1.5
    //
    //

    @Override
    public int glGenBuffer() {
        return this.delegate.glGenBuffer();
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        this.delegate.glDeleteBuffer(buffer);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        this.delegate.glBindBuffer(target, buffer);
    }

    @Override
    public void glBufferData(int target, long data_size, long data, int usage) {
        this.delegate.glBufferData(target, data_size, data, usage);
    }

    @Override
    public void glBufferData(int target, @NonNull ByteBuffer data, int usage) {
        this.delegate.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, long data_size, long data) {
        this.delegate.glBufferSubData(target, offset, data_size, data);
    }

    @Override
    public void glBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        this.delegate.glBufferSubData(target, offset, data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, long data_size, long data) {
        this.delegate.glGetBufferSubData(target, offset, data_size, data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        this.delegate.glGetBufferSubData(target, offset, data);
    }

    @Override
    public int glGetBufferParameteri(int target, int pname) {
        return this.delegate.glGetBufferParameteri(target, pname);
    }

    @Override
    public long glMapBuffer(int target, int access) {
        return this.delegate.glMapBuffer(target, access);
    }

    @Override
    public ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer oldBuffer) {
        return this.delegate.glMapBuffer(target, access, length, oldBuffer);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return this.delegate.glUnmapBuffer(target);
    }

    //
    //
    // OpenGL 2.0
    //
    //

    @Override
    public int glCreateShader(int type) {
        return this.delegate.glCreateShader(type);
    }

    @Override
    public void glDeleteShader(int shader) {
        this.delegate.glDeleteShader(shader);
    }

    @Override
    public void glShaderSource(int shader, @NonNull CharSequence... source) {
        this.delegate.glShaderSource(shader, source);
    }

    @Override
    public void glCompileShader(int shader) {
        this.delegate.glCompileShader(shader);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return this.delegate.glGetShaderi(shader, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return this.delegate.glGetShaderInfoLog(shader);
    }

    @Override
    public int glCreateProgram() {
        return this.delegate.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        this.delegate.glDeleteProgram(program);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        this.delegate.glAttachShader(program, shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        this.delegate.glDetachShader(program, shader);
    }

    @Override
    public void glLinkProgram(int program) {
        this.delegate.glLinkProgram(program);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return this.delegate.glGetProgrami(program, pname);
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        this.delegate.glGetProgramiv(program, pname, params);
    }

    @Override
    public int[] glGetProgramiv(int program, int pname, int count) {
        return this.delegate.glGetProgramiv(program, pname, count);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return this.delegate.glGetProgramInfoLog(program);
    }

    @Override
    public void glUseProgram(int program) {
        this.delegate.glUseProgram(program);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        this.delegate.glEnableVertexAttribArray(index);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        this.delegate.glDisableVertexAttribArray(index);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        this.delegate.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glBindAttribLocation(int program, int index, @NonNull CharSequence name) {
        this.delegate.glBindAttribLocation(program, index, name);
    }

    @Override
    public int glGetAttribLocation(int program, @NonNull CharSequence name) {
        return this.delegate.glGetAttribLocation(program, name);
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull IntBuffer size, @NonNull IntBuffer type) {
        return this.delegate.glGetActiveAttrib(program, index, bufSize, size, type);
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull int[] size, @NonNull int[] type) {
        return this.delegate.glGetActiveAttrib(program, index, bufSize, size, type);
    }

    @Override
    public int glGetUniformLocation(int program, @NonNull CharSequence name) {
        return this.delegate.glGetUniformLocation(program, name);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        this.delegate.glUniform1i(location, v0);
    }

    @Override
    public void glUniform2i(int location, int v0, int v1) {
        this.delegate.glUniform2i(location, v0, v1);
    }

    @Override
    public void glUniform3i(int location, int v0, int v1, int v2) {
        this.delegate.glUniform3i(location, v0, v1, v2);
    }

    @Override
    public void glUniform4i(int location, int v0, int v1, int v2, int v3) {
        this.delegate.glUniform4i(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniform1f(int location, float v0) {
        this.delegate.glUniform1f(location, v0);
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        this.delegate.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        this.delegate.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        this.delegate.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniform1i(int location, IntBuffer value) {
        this.delegate.glUniform1i(location, value);
    }

    @Override
    public void glUniform2i(int location, IntBuffer value) {
        this.delegate.glUniform2i(location, value);
    }

    @Override
    public void glUniform3i(int location, IntBuffer value) {
        this.delegate.glUniform3i(location, value);
    }

    @Override
    public void glUniform4i(int location, IntBuffer value) {
        this.delegate.glUniform4i(location, value);
    }

    @Override
    public void glUniform1f(int location, FloatBuffer value) {
        this.delegate.glUniform1f(location, value);
    }

    @Override
    public void glUniform2f(int location, FloatBuffer value) {
        this.delegate.glUniform2f(location, value);
    }

    @Override
    public void glUniform3f(int location, FloatBuffer value) {
        this.delegate.glUniform3f(location, value);
    }

    @Override
    public void glUniform4f(int location, FloatBuffer value) {
        this.delegate.glUniform4f(location, value);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        this.delegate.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    //
    //
    // OpenGL 3.0
    //
    //

    @Override
    public int glGetInteger(int pname, int idx) {
        return this.delegate.glGetInteger(pname, idx);
    }

    @Override
    public String glGetString(int pname, int idx) {
        return this.delegate.glGetString(pname, idx);
    }

    @Override
    public int glGenVertexArray() {
        return this.delegate.glGenVertexArray();
    }

    @Override
    public void glDeleteVertexArray(int array) {
        this.delegate.glDeleteVertexArray(array);
    }

    @Override
    public void glBindVertexArray(int array) {
        this.delegate.glBindVertexArray(array);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        this.delegate.glVertexAttribIPointer(index, size, type, stride, pointer);
    }

    @Override
    public void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name) {
        this.delegate.glBindFragDataLocation(program, colorNumber, name);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        this.delegate.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        this.delegate.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        this.delegate.glBeginTransformFeedback(primitiveMode);
    }

    @Override
    public void glEndTransformFeedback() {
        this.delegate.glEndTransformFeedback();
    }

    @Override
    public void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode) {
        this.delegate.glTransformFeedbackVaryings(program, varyings, bufferMode);
    }

    @Override
    public long glMapBufferRange(int target, long offset, long length, int access) {
        return this.delegate.glMapBufferRange(target, offset, length, access);
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer) {
        return this.delegate.glMapBufferRange(target, offset, length, access, oldBuffer);
    }

    @Override
    public void glFlushMappedBufferRange(int target, long offset, long length) {
        this.delegate.glFlushMappedBufferRange(target, offset, length);
    }

    @Override
    public void glUniform1ui(int location, int v0) {
        this.delegate.glUniform1ui(location, v0);
    }

    @Override
    public void glUniform2ui(int location, int v0, int v1) {
        this.delegate.glUniform2ui(location, v0, v1);
    }

    @Override
    public void glUniform3ui(int location, int v0, int v1, int v2) {
        this.delegate.glUniform3ui(location, v0, v1, v2);
    }

    @Override
    public void glUniform4ui(int location, int v0, int v1, int v2, int v3) {
        this.delegate.glUniform4ui(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniform1ui(int location, IntBuffer value) {
        this.delegate.glUniform1ui(location, value);
    }

    @Override
    public void glUniform2ui(int location, IntBuffer value) {
        this.delegate.glUniform2ui(location, value);
    }

    @Override
    public void glUniform3ui(int location, IntBuffer value) {
        this.delegate.glUniform3ui(location, value);
    }

    @Override
    public void glUniform4ui(int location, IntBuffer value) {
        this.delegate.glUniform4ui(location, value);
    }

    //
    //
    // OpenGL 3.1
    //
    //

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        super.checkSupported(GLExtension.GL_ARB_copy_buffer);
        this.delegate.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        super.checkSupported(GLExtension.GL_ARB_draw_instanced);
        this.delegate.glDrawArraysInstanced(mode, first, count, instancecount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
        super.checkSupported(GLExtension.GL_ARB_draw_instanced);
        this.delegate.glDrawElementsInstanced(mode, count, type, indices, instancecount);
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        super.checkSupported(GLExtension.GL_ARB_texture_buffer_object);
        this.delegate.glTexBuffer(target, internalFormat, buffer);
    }

    @Override
    public int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        return this.delegate.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public int glGetActiveUniformBlocki(int program, int uniformBlockIndex, int pname) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        return this.delegate.glGetActiveUniformBlocki(program, uniformBlockIndex, pname);
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex, int bufSize) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        return this.delegate.glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize);
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        this.delegate.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public int[] glGetUniformIndices(int program, CharSequence[] uniformNames) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        return this.delegate.glGetUniformIndices(program, uniformNames);
    }

    @Override
    public int glGetActiveUniformsi(int program, int uniformIndex, int pname) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        return this.delegate.glGetActiveUniformsi(program, uniformIndex, pname);
    }

    @Override
    public String glGetActiveUniformName(int program, int uniformIndex, int bufSize) {
        super.checkSupported(GLExtension.GL_ARB_uniform_buffer_object);
        return this.delegate.glGetActiveUniformName(program, uniformIndex, bufSize);
    }

    //
    //
    // OpenGL 3.2
    //
    //

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        super.checkSupported(GLExtension.GL_ARB_draw_elements_base_vertex);
        this.delegate.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        super.checkSupported(GLExtension.GL_ARB_draw_elements_base_vertex);
        this.delegate.glMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        super.checkSupported(GLExtension.GL_ARB_sync);
        return this.delegate.glFenceSync(condition, flags);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        super.checkSupported(GLExtension.GL_ARB_sync);
        return this.delegate.glClientWaitSync(sync, flags, timeout);
    }

    @Override
    public int glGetSync(long sync, int pname) {
        super.checkSupported(GLExtension.GL_ARB_sync);
        return this.delegate.glGetSync(sync, pname);
    }

    @Override
    public void glDeleteSync(long sync) {
        super.checkSupported(GLExtension.GL_ARB_sync);
        this.delegate.glDeleteSync(sync);
    }

    //
    //
    // OpenGL 3.3
    //
    //

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        super.checkSupported(GLExtension.GL_ARB_instanced_arrays);
        this.delegate.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public int glGenSampler() {
        super.checkSupported(GLExtension.GL_ARB_sampler_objects);
        return this.delegate.glGenSampler();
    }

    @Override
    public void glDeleteSampler(int sampler) {
        super.checkSupported(GLExtension.GL_ARB_sampler_objects);
        this.delegate.glDeleteSampler(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        super.checkSupported(GLExtension.GL_ARB_sampler_objects);
        this.delegate.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, int param) {
        super.checkSupported(GLExtension.GL_ARB_sampler_objects);
        this.delegate.glSamplerParameter(sampler, pname, param);
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, float param) {
        super.checkSupported(GLExtension.GL_ARB_sampler_objects);
        this.delegate.glSamplerParameter(sampler, pname, param);
    }

    //
    //
    // OpenGL 4.1
    //
    //

    @Override
    public void glProgramUniform1i(int program, int location, int v0) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform1i(program, location, v0);
    }

    @Override
    public void glProgramUniform2i(int program, int location, int v0, int v1) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform2i(program, location, v0, v1);
    }

    @Override
    public void glProgramUniform3i(int program, int location, int v0, int v1, int v2) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform3i(program, location, v0, v1, v2);
    }

    @Override
    public void glProgramUniform4i(int program, int location, int v0, int v1, int v2, int v3) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform4i(program, location, v0, v1, v2, v3);
    }

    @Override
    public void glProgramUniform1ui(int program, int location, int v0) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform1ui(program, location, v0);
    }

    @Override
    public void glProgramUniform2ui(int program, int location, int v0, int v1) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform2ui(program, location, v0, v1);
    }

    @Override
    public void glProgramUniform3ui(int program, int location, int v0, int v1, int v2) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform3ui(program, location, v0, v1, v2);
    }

    @Override
    public void glProgramUniform4ui(int program, int location, int v0, int v1, int v2, int v3) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform4ui(program, location, v0, v1, v2, v3);
    }

    @Override
    public void glProgramUniform1f(int program, int location, float v0) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform1f(program, location, v0);
    }

    @Override
    public void glProgramUniform2f(int program, int location, float v0, float v1) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform2f(program, location, v0, v1);
    }

    @Override
    public void glProgramUniform3f(int program, int location, float v0, float v1, float v2) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform3f(program, location, v0, v1, v2);
    }

    @Override
    public void glProgramUniform4f(int program, int location, float v0, float v1, float v2, float v3) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform4f(program, location, v0, v1, v2, v3);
    }

    @Override
    public void glProgramUniform1i(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform1i(program, location, value);
    }

    @Override
    public void glProgramUniform2i(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform2i(program, location, value);
    }

    @Override
    public void glProgramUniform3i(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform3i(program, location, value);
    }

    @Override
    public void glProgramUniform4i(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform4i(program, location, value);
    }

    @Override
    public void glProgramUniform1ui(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform1ui(program, location, value);
    }

    @Override
    public void glProgramUniform2ui(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform2ui(program, location, value);
    }

    @Override
    public void glProgramUniform3ui(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform3ui(program, location, value);
    }

    @Override
    public void glProgramUniform4ui(int program, int location, IntBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform4ui(program, location, value);
    }

    @Override
    public void glProgramUniform1f(int program, int location, FloatBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform1f(program, location, value);
    }

    @Override
    public void glProgramUniform2f(int program, int location, FloatBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform2f(program, location, value);
    }

    @Override
    public void glProgramUniform3f(int program, int location, FloatBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform3f(program, location, value);
    }

    @Override
    public void glProgramUniform4f(int program, int location, FloatBuffer value) {
        super.checkSupported(GLExtension.GL_ARB_separate_shader_objects);
        this.delegate.glProgramUniform4f(program, location, value);
    }

    //
    //
    // OpenGL 4.2
    //
    //

    @Override
    public void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance) {
        super.checkSupported(GLExtension.GL_ARB_base_instance);
        this.delegate.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
    }

    @Override
    public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices, int instancecount, int basevertex, int baseinstance) {
        super.checkSupported(GLExtension.GL_ARB_base_instance);
        this.delegate.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex, baseinstance);
    }

    @Override
    public void glMemoryBarrier(int barriers) {
        super.checkSupported(GLExtension.GL_ARB_shader_image_load_store);
        this.delegate.glMemoryBarrier(barriers);
    }

    //
    //
    // OpenGL 4.3
    //
    //

    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        super.checkSupported(GLExtension.GL_ARB_compute_shader);
        this.delegate.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
    }

    @Override
    public void glInvalidateBufferData(int buffer) {
        super.checkSupported(GLExtension.GL_ARB_invalidate_subdata);
        this.delegate.glInvalidateBufferData(buffer);
    }

    @Override
    public void glInvalidateBufferSubData(int buffer, long offset, long length) {
        super.checkSupported(GLExtension.GL_ARB_invalidate_subdata);
        this.delegate.glInvalidateBufferSubData(buffer, offset, length);
    }

    @Override
    public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
        super.checkSupported(GLExtension.GL_ARB_multi_draw_indirect);
        this.delegate.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride) {
        super.checkSupported(GLExtension.GL_ARB_multi_draw_indirect);
        this.delegate.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
    }

    @Override
    public int glGetProgramInterfacei(int program, int programInterface, int pname) {
        super.checkSupported(GLExtension.GL_ARB_program_interface_query);
        return this.delegate.glGetProgramInterfacei(program, programInterface, pname);
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        super.checkSupported(GLExtension.GL_ARB_program_interface_query);
        return this.delegate.glGetProgramResourceIndex(program, programInterface, name);
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull IntBuffer props, IntBuffer length, @NonNull IntBuffer params) {
        super.checkSupported(GLExtension.GL_ARB_program_interface_query);
        this.delegate.glGetProgramResourceiv(program, programInterface, index, props, length, params);
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull int[] props, int[] length, @NonNull int[] params) {
        super.checkSupported(GLExtension.GL_ARB_program_interface_query);
        this.delegate.glGetProgramResourceiv(program, programInterface, index, props, length, params);
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index, int bufSize) {
        super.checkSupported(GLExtension.GL_ARB_program_interface_query);
        return this.delegate.glGetProgramResourceName(program, programInterface, index, bufSize);
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        super.checkSupported(GLExtension.GL_ARB_shader_storage_buffer_object);
        this.delegate.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
    }

    @Override
    public void glObjectLabel(int identifier, int name, @NonNull CharSequence label) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glObjectLabel(identifier, name, label);
    }

    @Override
    public void glObjectPtrLabel(long ptr, @NonNull CharSequence label) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glObjectPtrLabel(ptr, label);
    }

    @Override
    public String glGetObjectLabel(int identifier, int name) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        return this.delegate.glGetObjectLabel(identifier, name);
    }

    @Override
    public String glGetObjectPtrLabel(long ptr) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        return this.delegate.glGetObjectPtrLabel(ptr);
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glDebugMessageControl(source, type, severity, ids, enabled);
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, int[] ids, boolean enabled) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glDebugMessageControl(source, type, severity, ids, enabled);
    }

    @Override
    public void glDebugMessageInsert(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glDebugMessageInsert(source, type, id, severity, msg);
    }

    @Override
    public void glDebugMessageCallback(GLDebugOutputCallback callback) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glDebugMessageCallback(callback);
    }

    @Override
    public void glPushDebugGroup(int source, int id, @NonNull CharSequence msg) {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glPushDebugGroup(source, id, msg);
    }

    @Override
    public void glPopDebugGroup() {
        super.checkSupported(GLExtension.GL_KHR_debug);
        this.delegate.glPopDebugGroup();
    }

    //
    //
    // OpenGL 4.4
    //
    //

    @Override
    public void glBufferStorage(int target, long data_size, long data, int flags) {
        super.checkSupported(GLExtension.GL_ARB_buffer_storage);
        this.delegate.glBufferStorage(target, data_size, data, flags);
    }

    @Override
    public void glBufferStorage(int target, @NonNull ByteBuffer data, int flags) {
        super.checkSupported(GLExtension.GL_ARB_buffer_storage);
        this.delegate.glBufferStorage(target, data, flags);
    }

    @Override
    public void glBindBuffersBase(int target, int first, int count) {
        super.checkSupported(GLExtension.GL_ARB_multi_bind);
        this.delegate.glBindBuffersBase(target, first, count);
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull IntBuffer buffers) {
        super.checkSupported(GLExtension.GL_ARB_multi_bind);
        this.delegate.glBindBuffersBase(target, first, buffers);
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull int[] buffers) {
        super.checkSupported(GLExtension.GL_ARB_multi_bind);
        this.delegate.glBindBuffersBase(target, first, buffers);
    }

    //
    //
    // OpenGL 4.5
    //
    //

    @Override
    public void glClipControl(int origin, int depth) {
        super.checkSupported(GLExtension.GL_ARB_clip_control);
        this.delegate.glClipControl(origin, depth);
    }

    @Override
    public int glCreateBuffer() {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glCreateBuffer();
    }

    @Override
    public void glNamedBufferData(int buffer, long data_size, long data, int usage) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glNamedBufferData(buffer, data_size, data, usage);
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glNamedBufferData(buffer, data, usage);
    }

    private static final GLExtensionSet direct_state_access_AND_buffer_storage = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_direct_state_access)
            .add(GLExtension.GL_ARB_buffer_storage);

    @Override
    public void glNamedBufferStorage(int buffer, long data_size, long data, int flags) {
        super.checkSupported(direct_state_access_AND_buffer_storage);
        this.delegate.glNamedBufferStorage(buffer, data_size, data, flags);
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags) {
        super.checkSupported(direct_state_access_AND_buffer_storage);
        this.delegate.glNamedBufferStorage(buffer, data, flags);
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, long data_size, long data) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glNamedBufferSubData(buffer, offset, data_size, data);
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glNamedBufferSubData(buffer, offset, data);
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, long data_size, long data) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glGetNamedBufferSubData(buffer, offset, data_size, data);
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glGetNamedBufferSubData(buffer, offset, data);
    }

    @Override
    public int glGetNamedBufferParameteri(int buffer, int pname) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glGetNamedBufferParameteri(buffer, pname);
    }

    @Override
    public long glMapNamedBuffer(int buffer, int access) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glMapNamedBuffer(buffer, access);
    }

    @Override
    public ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glMapNamedBuffer(buffer, access, length, oldBuffer);
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long size, int access) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glMapNamedBufferRange(buffer, offset, size, access);
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
    }

    @Override
    public void glFlushMappedNamedBufferRange(int buffer, long offset, long length) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glFlushMappedNamedBufferRange(buffer, offset, length);
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glUnmapNamedBuffer(buffer);
    }

    private static final GLExtensionSet direct_state_access_AND_copy_buffer = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_direct_state_access)
            .add(GLExtension.GL_ARB_copy_buffer);

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
        super.checkSupported(direct_state_access_AND_copy_buffer);
        this.delegate.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
    }

    @Override
    public int glCreateVertexArray() {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        return this.delegate.glCreateVertexArray();
    }

    @Override
    public void glVertexArrayElementBuffer(int vaobj, int buffer) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayElementBuffer(vaobj, buffer);
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glEnableVertexArrayAttrib(vaobj, index);
    }

    @Override
    public void glDisableVertexArrayAttrib(int vaobj, int index) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glDisableVertexArrayAttrib(vaobj, index);
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
    }

    @Override
    public void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayVertexBuffers(vaobj, first, count, buffers, offsets, strides);
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides) {
        super.checkSupported(GLExtension.GL_ARB_direct_state_access);
        this.delegate.glVertexArrayVertexBuffers(vaobj, first, count, buffers, offsets, strides);
    }

    //
    //
    // OpenGL 4.6
    //
    //

    @Override
    public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawcount, int maxdrawcount, int stride) {
        super.checkSupported(GLExtension.GL_ARB_indirect_parameters);
        this.delegate.glMultiDrawArraysIndirectCount(mode, indirect, drawcount, maxdrawcount, stride);
    }

    @Override
    public void glMultiDrawElementsIndirectCount(int mode, int type, long indirect, long drawcount, int maxdrawcount, int stride) {
        super.checkSupported(GLExtension.GL_ARB_indirect_parameters);
        this.delegate.glMultiDrawElementsIndirectCount(mode, type, indirect, drawcount, maxdrawcount, stride);
    }

    //
    //
    // No OpenGL version
    //
    //

    @Override
    public void glDebugMessageControlARB(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        super.checkSupported(GLExtension.GL_ARB_debug_output);
        this.delegate.glDebugMessageControlARB(source, type, severity, ids, enabled);
    }

    @Override
    public void glDebugMessageControlARB(int source, int type, int severity, int[] ids, boolean enabled) {
        super.checkSupported(GLExtension.GL_ARB_debug_output);
        this.delegate.glDebugMessageControlARB(source, type, severity, ids, enabled);
    }

    @Override
    public void glDebugMessageInsertARB(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        super.checkSupported(GLExtension.GL_ARB_debug_output);
        this.delegate.glDebugMessageInsertARB(source, type, id, severity, msg);
    }

    @Override
    public void glDebugMessageCallbackARB(GLDebugOutputCallback callback) {
        super.checkSupported(GLExtension.GL_ARB_debug_output);
        this.delegate.glDebugMessageCallbackARB(callback);
    }

    @Override
    public void glBufferPageCommitmentARB(int target, long offset, long size, boolean commit) {
        super.checkSupported(GLExtension.GL_ARB_sparse_buffer);
        this.delegate.glBufferPageCommitmentARB(target, offset, size, commit);
    }

    private static final GLExtensionSet direct_state_access_AND_sparse_buffer = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_direct_state_access)
            .add(GLExtension.GL_ARB_sparse_buffer);

    @Override
    public void glNamedBufferPageCommitmentARB(int buffer, long offset, long size, boolean commit) {
        super.checkSupported(direct_state_access_AND_sparse_buffer);
        this.delegate.glNamedBufferPageCommitmentARB(buffer, offset, size, commit);
    }
}
