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

package net.daporkchop.fp2.gl.opengl.lwjgl3;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLVersion;
import net.daporkchop.lib.common.function.exception.EPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.ARBDrawElementsBaseVertex;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.ARBProgramInterfaceQuery;
import org.lwjgl.opengl.ARBSamplerObjects;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBTextureBufferObject;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class GLAPILWJGL3 implements GLAPI {
    // OpenGL 3.1
    private final boolean GL_ARB_copy_buffer;
    private final boolean GL_ARB_texture_buffer_object;
    private final boolean GL_ARB_uniform_buffer_object;

    // OpenGL 3.2
    private final boolean GL_ARB_draw_elements_base_vertex;

    // OpenGL 3.3
    private final boolean GL_ARB_instanced_arrays;
    private final boolean GL_ARB_sampler_objects;

    // OpenGL 4.2
    private final boolean GL_ARB_shader_image_load_store;

    // OpenGL 4.3
    private final boolean GL_ARB_multi_draw_indirect;
    private final boolean GL_ARB_program_interface_query;
    private final boolean GL_ARB_shader_storage_buffer_object;

    public GLAPILWJGL3() {
        GLCapabilities capabilities = GL.createCapabilities();

        // OpenGL 3.1
        this.GL_ARB_copy_buffer = !capabilities.OpenGL31 && capabilities.GL_ARB_copy_buffer;
        this.GL_ARB_texture_buffer_object = !capabilities.OpenGL31 && capabilities.GL_ARB_texture_buffer_object;
        this.GL_ARB_uniform_buffer_object = !capabilities.OpenGL31 && capabilities.GL_ARB_uniform_buffer_object;

        // OpenGL 3.2
        this.GL_ARB_draw_elements_base_vertex = !capabilities.OpenGL32 && capabilities.GL_ARB_draw_elements_base_vertex;

        // OpenGL 3.3
        this.GL_ARB_instanced_arrays = !capabilities.OpenGL33 && capabilities.GL_ARB_instanced_arrays;
        this.GL_ARB_sampler_objects = !capabilities.OpenGL33 && capabilities.GL_ARB_sampler_objects;

        // OpenGL 4.2
        this.GL_ARB_shader_image_load_store = !capabilities.OpenGL42 && capabilities.GL_ARB_shader_image_load_store;

        // OpenGL 4.3
        this.GL_ARB_multi_draw_indirect = !capabilities.OpenGL43 && capabilities.GL_ARB_multi_draw_indirect;
        this.GL_ARB_program_interface_query = !capabilities.OpenGL43 && capabilities.GL_ARB_program_interface_query;
        this.GL_ARB_shader_storage_buffer_object = !capabilities.OpenGL43 && capabilities.GL_ARB_shader_storage_buffer_object;
    }

    @Override
    public GLVersion version() {
        GLCapabilities capabilities = GL.createCapabilities();

        return Stream.of(GLVersion.values())
                .filter((EPredicate<GLVersion>) version -> {
                    try {
                        return (boolean) GLCapabilities.class.getDeclaredField("OpenGL" + version.major() + version.minor()).get(capabilities);
                    } catch (NoSuchFieldException e) {
                        return false; //field not found, therefore the version isn't supported by LWJGL2
                    }
                })
                .max(Comparator.naturalOrder())
                .get();
    }

    //
    //
    // OpenGL 1.1
    //
    //

    @Override
    public void glEnable(int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    @Override
    public boolean glGetBoolean(int pname) {
        return GL11.glGetBoolean(pname);
    }

    @Override
    public void glGetBoolean(int pname, long data) {
        GL11.nglGetBooleanv(pname, data);
    }

    @Override
    public int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    @Override
    public void glGetInteger(int pname, long data) {
        GL11.nglGetIntegerv(pname, data);
    }

    @Override
    public float glGetFloat(int pname) {
        return GL11.glGetFloat(pname);
    }

    @Override
    public void glGetFloat(int pname, long data) {
        GL11.nglGetFloatv(pname, data);
    }

    @Override
    public double glGetDouble(int pname) {
        return GL11.glGetDouble(pname);
    }

    @Override
    public void glGetDouble(int pname, long data) {
        GL11.nglGetDoublev(pname, data);
    }

    @Override
    public String glGetString(int pname) {
        return GL11.glGetString(pname);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices) {
        GL11.glDrawElements(mode, count, type, MemoryUtil.memAddress(indices));
    }

    @Override
    public int glGenTexture() {
        return GL11.glGenTextures();
    }

    @Override
    public void glDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
    }

    @Override
    public void glTexParameter(int target, int pname, int param) {
        GL11.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexParameter(int target, int pname, float param) {
        GL11.glTexParameterf(target, pname, param);
    }

    @Override
    public int glGetTexParameterInteger(int target, int pname) {
        return GL11.glGetTexParameteri(target, pname);
    }

    @Override
    public float glGetTexParameterFloat(int target, int pname) {
        return GL11.glGetTexParameterf(target, pname);
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data) {
        GL11.nglTexImage1D(target, level, internalformat, width, 0, format, type, data);
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexImage1D(target, level, internalformat, width, 0, format, type, data);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data) {
        GL11.nglTexImage2D(target, level, internalformat, width, height, 0, format, type, data);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexImage2D(target, level, internalformat, width, height, 0, format, type, data);
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data) {
        GL11.nglTexSubImage1D(target, level, xoffset, width, format, type, data);
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexSubImage1D(target, level, xoffset, width, format, type, data);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data) {
        GL11.nglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }

    @Override
    public void glClear(int mask) {
        GL11.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glClearDepth(double depth) {
        GL11.glClearDepth(depth);
    }

    @Override
    public void glDepthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glClearStencil(int s) {
        GL11.glClearStencil(s);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    @Override
    public void glStencilOp(int sfail, int dpfail, int dppass) {
        GL11.glStencilOp(sfail, dpfail, dppass);
    }

    @Override
    public void glPushClientAttrib(int mask) {
        GL11.glPushClientAttrib(mask);
    }

    @Override
    public void glPopClientAttrib() {
        GL11.glPopClientAttrib();
    }

    @Override
    public void glPushAttrib(int mask) {
        GL11.glPushAttrib(mask);
    }

    @Override
    public void glPopAttrib() {
        GL11.glPopAttrib();
    }

    //
    //
    // OpenGL 1.2
    //
    //

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, long data) {
        GL12.nglTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, data);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data) {
        GL12.nglTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
    }

    //
    //
    // OpenGL 1.3
    //
    //

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    //
    //
    // OpenGL 1.4
    //
    //

    @Override
    public void glMultiDrawArrays(int mode, long first, long count, int drawcount) {
        GL14.nglMultiDrawArrays(mode, first, count, drawcount);
    }

    @Override
    public void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount) {
        GL14.nglMultiDrawElements(mode, count, type, indices, drawcount);
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        GL14.glBlendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GL14.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    //
    //
    // OpenGL 1.5
    //
    //

    @Override
    public int glGenBuffer() {
        return GL15.glGenBuffers();
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void glBufferData(int target, long data_size, long data, int usage) {
        GL15.nglBufferData(target, data_size, data, usage);
    }

    @Override
    public void glBufferData(int target, @NonNull ByteBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, long data_size, long data) {
        GL15.nglBufferSubData(target, offset, data_size, data);
    }

    @Override
    public void glBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, long data_size, long data) {
        GL15.nglGetBufferSubData(target, offset, data_size, data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15.glGetBufferSubData(target, offset, data);
    }

    @Override
    public long glMapBuffer(int target, int usage) {
        return GL15.nglMapBuffer(target, usage);
    }

    @Override
    public void glUnmapBuffer(int target) {
        GL15.glUnmapBuffer(target);
    }

    //
    //
    // OpenGL 2.0
    //
    //

    @Override
    public int glCreateShader(int type) {
        return GL20.glCreateShader(type);
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    @Override
    public void glShaderSource(int shader, @NonNull CharSequence... source) {
        GL20.glShaderSource(shader, source);
    }

    @Override
    public void glCompileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GL20.glGetShaderi(shader, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL_INFO_LOG_LENGTH));
    }

    @Override
    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GL20.glDetachShader(program, shader);
    }

    @Override
    public void glLinkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GL20.glGetProgrami(program, pname);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL_INFO_LOG_LENGTH));
    }

    @Override
    public void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glDisableVertexArray(int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glBindAttribLocation(int program, int index, @NonNull CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    @Override
    public int glGetUniformLocation(int program, @NonNull CharSequence name) {
        return GL20.glGetUniformLocation(program, name);
    }

    @Override
    public void glUniform(int location, int v0) {
        GL20.glUniform1i(location, v0);
    }

    @Override
    public void glUniform(int location, int v0, int v1) {
        GL20.glUniform2i(location, v0, v1);
    }

    @Override
    public void glUniform(int location, int v0, int v1, int v2) {
        GL20.glUniform3i(location, v0, v1, v2);
    }

    @Override
    public void glUniform(int location, int v0, int v1, int v2, int v3) {
        GL20.glUniform4i(location, v0, v1, v2, v3);
    }

    @Override
    public void glUniform(int location, float v0) {
        GL20.glUniform1f(location, v0);
    }

    @Override
    public void glUniform(int location, float v0, float v1) {
        GL20.glUniform2f(location, v0, v1);
    }

    @Override
    public void glUniform(int location, float v0, float v1, float v2) {
        GL20.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform(int location, float v0, float v1, float v2, float v3) {
        GL20.glUniform4f(location, v0, v1, v2, v3);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        GL20.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    //
    //
    // OpenGL 3.0
    //
    //

    @Override
    public int glGetInteger(int pname, int idx) {
        return GL30.glGetIntegeri(pname, idx);
    }

    @Override
    public String glGetString(int pname, int idx) {
        return GL30.glGetStringi(pname, idx);
    }

    @Override
    public int glGenVertexArray() {
        return GL30.glGenVertexArrays();
    }

    @Override
    public void glDeleteVertexArray(int array) {
        GL30.glDeleteVertexArrays(array);
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30.glBindVertexArray(array);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
    }

    @Override
    public void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name) {
        GL30.glBindFragDataLocation(program, colorNumber, name);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GL30.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GL30.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        GL30.glBeginTransformFeedback(primitiveMode);
    }

    @Override
    public void glEndTransformFeedback() {
        GL30.glEndTransformFeedback();
    }

    @Override
    public void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode) {
        GL30.glTransformFeedbackVaryings(program, varyings, bufferMode);
    }

    //
    //
    // OpenGL 3.1
    //
    //

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (this.GL_ARB_copy_buffer) {
            ARBCopyBuffer.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        } else {
            GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        }
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        if (this.GL_ARB_texture_buffer_object) {
            ARBTextureBufferObject.glTexBufferARB(target, internalFormat, buffer);
        } else {
            GL31.glTexBuffer(target, internalFormat, buffer);
        }
    }

    @Override
    public int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName) {
        if (this.GL_ARB_uniform_buffer_object) {
            return ARBUniformBufferObject.glGetUniformBlockIndex(program, uniformBlockName);
        } else {
            return GL31.glGetUniformBlockIndex(program, uniformBlockName);
        }
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        if (this.GL_ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        } else {
            GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        }
    }

    //
    //
    // OpenGL 3.2
    //
    //

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        if (this.GL_ARB_draw_elements_base_vertex) {
            ARBDrawElementsBaseVertex.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        } else {
            GL32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        }
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        if (this.GL_ARB_draw_elements_base_vertex) {
            ARBDrawElementsBaseVertex.nglMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
        } else {
            GL32.nglMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
        }
    }

    //
    //
    // OpenGL 3.3
    //
    //

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        if (this.GL_ARB_instanced_arrays) {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        } else {
            GL33.glVertexAttribDivisor(index, divisor);
        }
    }

    @Override
    public int glGenSampler() {
        if (this.GL_ARB_sampler_objects) {
            return ARBSamplerObjects.glGenSamplers();
        } else {
            return GL33.glGenSamplers();
        }
    }

    @Override
    public void glDeleteSampler(int sampler) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glDeleteSamplers(sampler);
        } else {
            GL33.glDeleteSamplers(sampler);
        }
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glBindSampler(unit, sampler);
        } else {
            GL33.glBindSampler(unit, sampler);
        }
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, int param) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameteri(sampler, pname, param);
        } else {
            GL33.glSamplerParameteri(sampler, pname, param);
        }
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, float param) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameterf(sampler, pname, param);
        } else {
            GL33.glSamplerParameterf(sampler, pname, param);
        }
    }

    //
    //
    // OpenGL 4.2
    //
    //

    @Override
    public void glMemoryBarrier(int barriers) {
        if (this.GL_ARB_shader_image_load_store) {
            ARBShaderImageLoadStore.glMemoryBarrier(barriers);
        } else {
            GL42.glMemoryBarrier(barriers);
        }
    }

    //
    //
    // OpenGL 4.3
    //
    //

    @Override
    public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
        if (this.GL_ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        } else {
            GL43.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride) {
        if (this.GL_ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
        } else {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
        }
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        if (this.GL_ARB_program_interface_query) {
            return ARBProgramInterfaceQuery.glGetProgramResourceIndex(program, programInterface, name);
        } else {
            return GL43.glGetProgramResourceIndex(program, programInterface, name);
        }
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        if (this.GL_ARB_shader_storage_buffer_object) {
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        } else {
            GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        }
    }
}
