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
import lombok.val;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.function.throwing.TPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBDrawElementsBaseVertex;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.ARBProgramInterfaceQuery;
import org.lwjgl.opengl.ARBSamplerObjects;
import org.lwjgl.opengl.ARBSeparateShaderObjects;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBSparseBuffer;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBTextureBufferObject;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.MemoryUtil;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public final class GLAPILWJGL3 extends OpenGL {
    public static OpenGL forCurrent() {
        return new GLAPILWJGL3();
    }

    // OpenGL 3.1
    private final boolean OpenGL31;
    private final boolean GL_ARB_copy_buffer;
    private final boolean GL_ARB_draw_instanced;
    private final boolean GL_ARB_texture_buffer_object;
    private final boolean GL_ARB_uniform_buffer_object;

    // OpenGL 3.2
    private final boolean OpenGL32;
    private final boolean GL_ARB_draw_elements_base_vertex;
    private final boolean GL_ARB_sync;

    // OpenGL 3.3
    private final boolean OpenGL33;
    private final boolean GL_ARB_instanced_arrays;
    private final boolean GL_ARB_sampler_objects;

    // OpenGL 4.1
    private final boolean OpenGL41;
    private final boolean GL_ARB_separate_shader_objects;

    // OpenGL 4.2
    private final boolean OpenGL42;
    private final boolean GL_ARB_shader_image_load_store;

    // OpenGL 4.3
    private final boolean OpenGL43;
    private final boolean GL_ARB_compute_shader;
    private final boolean GL_ARB_multi_draw_indirect;
    private final boolean GL_ARB_program_interface_query;
    private final boolean GL_ARB_shader_storage_buffer_object;
    private final boolean GL_KHR_debug;

    // OpenGL 4.4
    private final boolean OpenGL44;
    private final boolean GL_ARB_buffer_storage;

    // OpenGL 4.5
    private final boolean OpenGL45;
    private final boolean GL_ARB_direct_state_access;

    // No OpenGL version
    private final boolean GL_ARB_sparse_buffer;

    public GLAPILWJGL3() {
        GLCapabilities capabilities = GL.createCapabilities();

        // OpenGL 3.1
        this.OpenGL31 = capabilities.OpenGL31;
        this.GL_ARB_copy_buffer = !capabilities.OpenGL31 && capabilities.GL_ARB_copy_buffer;
        this.GL_ARB_draw_instanced = !capabilities.OpenGL31 && capabilities.GL_ARB_draw_instanced;
        this.GL_ARB_texture_buffer_object = !capabilities.OpenGL31 && capabilities.GL_ARB_texture_buffer_object;
        this.GL_ARB_uniform_buffer_object = !capabilities.OpenGL31 && capabilities.GL_ARB_uniform_buffer_object;

        // OpenGL 3.2
        this.OpenGL32 = capabilities.OpenGL32;
        this.GL_ARB_draw_elements_base_vertex = !capabilities.OpenGL32 && capabilities.GL_ARB_draw_elements_base_vertex;
        this.GL_ARB_sync = !capabilities.OpenGL32 && capabilities.GL_ARB_sync;

        // OpenGL 3.3
        this.OpenGL33 = capabilities.OpenGL33;
        this.GL_ARB_instanced_arrays = !capabilities.OpenGL33 && capabilities.GL_ARB_instanced_arrays;
        this.GL_ARB_sampler_objects = !capabilities.OpenGL33 && capabilities.GL_ARB_sampler_objects;

        // OpenGL 4.1
        this.OpenGL41 = capabilities.OpenGL41;
        this.GL_ARB_separate_shader_objects = !capabilities.OpenGL41 && capabilities.GL_ARB_separate_shader_objects;

        // OpenGL 4.2
        this.OpenGL42 = capabilities.OpenGL42;
        this.GL_ARB_shader_image_load_store = !capabilities.OpenGL42 && capabilities.GL_ARB_shader_image_load_store;

        // OpenGL 4.3
        this.OpenGL43 = capabilities.OpenGL43;
        this.GL_ARB_compute_shader = !capabilities.OpenGL43 && capabilities.GL_ARB_compute_shader;
        this.GL_ARB_multi_draw_indirect = !capabilities.OpenGL43 && capabilities.GL_ARB_multi_draw_indirect;
        this.GL_ARB_program_interface_query = !capabilities.OpenGL43 && capabilities.GL_ARB_program_interface_query;
        this.GL_ARB_shader_storage_buffer_object = !capabilities.OpenGL43 && capabilities.GL_ARB_shader_storage_buffer_object;
        this.GL_KHR_debug = !capabilities.OpenGL43 && capabilities.GL_KHR_debug;

        // OpenGL 4.4
        this.OpenGL44 = capabilities.OpenGL44;
        this.GL_ARB_buffer_storage = !capabilities.OpenGL44 && capabilities.GL_ARB_buffer_storage;

        // OpenGL 4.5
        this.OpenGL45 = capabilities.OpenGL45;
        this.GL_ARB_direct_state_access = !capabilities.OpenGL45 && capabilities.GL_ARB_direct_state_access;

        // No OpenGL version
        this.GL_ARB_sparse_buffer = capabilities.GL_ARB_sparse_buffer;
    }

    @Override
    protected GLVersion determineVersion() {
        GLCapabilities capabilities = GL.createCapabilities();

        return Stream.of(GLVersion.values())
                .filter((TPredicate<GLVersion, Throwable>) version -> {
                    try {
                        return (boolean) MethodHandles.publicLookup().findGetter(GLCapabilities.class, "OpenGL" + version.major() + version.minor(), boolean.class).invokeExact(capabilities);
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
        GL11C.glEnable(cap);
        super.debugCheckError();
    }

    @Override
    public void glDisable(int cap) {
        GL11C.glDisable(cap);
        super.debugCheckError();
    }

    @Override
    public boolean glIsEnabled(int cap) {
        val res = GL11.glIsEnabled(cap);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetError() {
        return GL11C.glGetError();
    }

    @Override
    public boolean glGetBoolean(int pname) {
        val res = GL11C.glGetBoolean(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetBoolean(int pname, long data) {
        GL11C.nglGetBooleanv(pname, data);
        super.debugCheckError();
    }

    @Override
    public int glGetInteger(int pname) {
        val res = GL11C.glGetInteger(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetInteger(int pname, long data) {
        GL11C.nglGetIntegerv(pname, data);
        super.debugCheckError();
    }

    @Override
    public float glGetFloat(int pname) {
        val res = GL11C.glGetFloat(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetFloat(int pname, long data) {
        GL11C.nglGetFloatv(pname, data);
        super.debugCheckError();
    }

    @Override
    public double glGetDouble(int pname) {
        val res = GL11C.glGetDouble(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetDouble(int pname, long data) {
        GL11C.nglGetDoublev(pname, data);
        super.debugCheckError();
    }

    @Override
    public String glGetString(int pname) {
        val res = GL11C.glGetString(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11C.glDrawArrays(mode, first, count);
        super.debugCheckError();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GL11C.glDrawElements(mode, count, type, indices);
        super.debugCheckError();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices) {
        GL11C.glDrawElements(mode, count, type, MemoryUtil.memAddress(indices));
        super.debugCheckError();
    }

    @Override
    public int glGenTexture() {
        val res = GL11C.glGenTextures();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteTexture(int texture) {
        GL11C.glDeleteTextures(texture);
        super.debugCheckError();
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GL11C.glBindTexture(target, texture);
        super.debugCheckError();
    }

    @Override
    public void glTexParameter(int target, int pname, int param) {
        GL11C.glTexParameteri(target, pname, param);
        super.debugCheckError();
    }

    @Override
    public void glTexParameter(int target, int pname, float param) {
        GL11C.glTexParameterf(target, pname, param);
        super.debugCheckError();
    }

    @Override
    public int glGetTexParameterInteger(int target, int pname) {
        val res = GL11C.glGetTexParameteri(target, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public float glGetTexParameterFloat(int target, int pname) {
        val res = GL11C.glGetTexParameterf(target, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data) {
        GL11C.nglTexImage1D(target, level, internalformat, width, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data) {
        GL11C.glTexImage1D(target, level, internalformat, width, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data) {
        GL11C.nglTexImage2D(target, level, internalformat, width, height, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        GL11C.glTexImage2D(target, level, internalformat, width, height, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data) {
        GL11C.nglTexSubImage1D(target, level, xoffset, width, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data) {
        GL11C.glTexSubImage1D(target, level, xoffset, width, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data) {
        GL11C.nglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        GL11C.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glClear(int mask) {
        GL11C.glClear(mask);
        super.debugCheckError();
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11C.glClearColor(red, green, blue, alpha);
        super.debugCheckError();
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11C.glColorMask(red, green, blue, alpha);
        super.debugCheckError();
    }

    @Override
    public void glClearDepth(double depth) {
        GL11C.glClearDepth(depth);
        super.debugCheckError();
    }

    @Override
    public void glDepthFunc(int func) {
        GL11C.glDepthFunc(func);
        super.debugCheckError();
    }

    @Override
    public void glDepthMask(boolean flag) {
        GL11C.glDepthMask(flag);
        super.debugCheckError();
    }

    @Override
    public void glClearStencil(int s) {
        GL11C.glClearStencil(s);
        super.debugCheckError();
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        GL11C.glStencilFunc(func, ref, mask);
        super.debugCheckError();
    }

    @Override
    public void glStencilMask(int mask) {
        GL11C.glStencilMask(mask);
        super.debugCheckError();
    }

    @Override
    public void glStencilOp(int sfail, int dpfail, int dppass) {
        GL11C.glStencilOp(sfail, dpfail, dppass);
        super.debugCheckError();
    }

    @Override
    public void glPushClientAttrib(int mask) {
        //TODO: check if supported
        GL11.glPushClientAttrib(mask);
        super.debugCheckError();
    }

    @Override
    public void glPopClientAttrib() {
        GL11.glPopClientAttrib();
        super.debugCheckError();
    }

    @Override
    public void glPushAttrib(int mask) {
        GL11.glPushAttrib(mask);
        super.debugCheckError();
    }

    @Override
    public void glPopAttrib() {
        GL11.glPopAttrib();
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.2
    //
    //

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, long data) {
        GL12C.nglTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        GL12C.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data) {
        GL12C.nglTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        GL12C.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.3
    //
    //

    @Override
    public void glActiveTexture(int texture) {
        GL13C.glActiveTexture(texture);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.4
    //
    //

    @Override
    public void glMultiDrawArrays(int mode, long first, long count, int drawcount) {
        GL14C.nglMultiDrawArrays(mode, first, count, drawcount);
        super.debugCheckError();
    }

    @Override
    public void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount) {
        GL14C.nglMultiDrawElements(mode, count, type, indices, drawcount);
        super.debugCheckError();
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        GL14C.glBlendColor(red, green, blue, alpha);
        super.debugCheckError();
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GL14C.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.5
    //
    //

    @Override
    public int glGenBuffer() {
        val res = GL15C.glGenBuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GL15C.glDeleteBuffers(buffer);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15C.glBindBuffer(target, buffer);
        super.debugCheckError();
    }

    @Override
    public void glBufferData(int target, long data_size, long data, int usage) {
        GL15C.nglBufferData(target, data_size, data, usage);
        super.debugCheckError();
    }

    @Override
    public void glBufferData(int target, @NonNull ByteBuffer data, int usage) {
        GL15C.glBufferData(target, data, usage);
        super.debugCheckError();
    }

    @Override
    public void glBufferSubData(int target, long offset, long data_size, long data) {
        GL15C.nglBufferSubData(target, offset, data_size, data);
        super.debugCheckError();
    }

    @Override
    public void glBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15C.glBufferSubData(target, offset, data);
        super.debugCheckError();
    }

    @Override
    public void glGetBufferSubData(int target, long offset, long data_size, long data) {
        GL15C.nglGetBufferSubData(target, offset, data_size, data);
        super.debugCheckError();
    }

    @Override
    public void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15C.glGetBufferSubData(target, offset, data);
        super.debugCheckError();
    }

    @Override
    public long glMapBuffer(int target, int access) {
        val res = GL15C.nglMapBuffer(target, access);
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer oldBuffer) {
        val res = GL15C.glMapBuffer(target, access, length, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        val res = GL15C.glUnmapBuffer(target);
        super.debugCheckError();
        return res;
    }

    //
    //
    // OpenGL 2.0
    //
    //

    @Override
    public int glCreateShader(int type) {
        val res = GL20C.glCreateShader(type);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20C.glDeleteShader(shader);
        super.debugCheckError();
    }

    @Override
    public void glShaderSource(int shader, @NonNull CharSequence... source) {
        GL20C.glShaderSource(shader, source);
        super.debugCheckError();
    }

    @Override
    public void glCompileShader(int shader) {
        GL20C.glCompileShader(shader);
        super.debugCheckError();
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        val res = GL20C.glGetShaderi(shader, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        val res = GL20C.glGetShaderInfoLog(shader, GL20C.glGetShaderi(shader, GL_INFO_LOG_LENGTH));
        super.debugCheckError();
        return res;
    }

    @Override
    public int glCreateProgram() {
        val res = GL20C.glCreateProgram();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20C.glDeleteProgram(program);
        super.debugCheckError();
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20C.glAttachShader(program, shader);
        super.debugCheckError();
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GL20C.glDetachShader(program, shader);
        super.debugCheckError();
    }

    @Override
    public void glLinkProgram(int program) {
        GL20C.glLinkProgram(program);
        super.debugCheckError();
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        val res = GL20C.glGetProgrami(program, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        GL20C.glGetProgramiv(program, pname, params);
        super.debugCheckError();
    }

    @Override
    public int[] glGetProgramiv(int program, int pname, int count) {
        int[] res = new int[count];
        GL20C.glGetProgramiv(program, pname, res);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        val res = GL20C.glGetProgramInfoLog(program, GL20C.glGetProgrami(program, GL_INFO_LOG_LENGTH));
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUseProgram(int program) {
        GL20C.glUseProgram(program);
        super.debugCheckError();
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20C.glEnableVertexAttribArray(index);
        super.debugCheckError();
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        GL20C.glDisableVertexAttribArray(index);
        super.debugCheckError();
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20C.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        super.debugCheckError();
    }

    @Override
    public void glBindAttribLocation(int program, int index, @NonNull CharSequence name) {
        GL20C.glBindAttribLocation(program, index, name);
        super.debugCheckError();
    }

    @Override
    public int glGetUniformLocation(int program, @NonNull CharSequence name) {
        val res = GL20C.glGetUniformLocation(program, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUniform(int location, int v0) {
        GL20C.glUniform1i(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, int v0, int v1) {
        GL20C.glUniform2i(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, int v0, int v1, int v2) {
        GL20C.glUniform3i(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, int v0, int v1, int v2, int v3) {
        GL20C.glUniform4i(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0) {
        GL20C.glUniform1f(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0, float v1) {
        GL20C.glUniform2f(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0, float v1, float v2) {
        GL20C.glUniform3f(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0, float v1, float v2, float v3) {
        GL20C.glUniform4f(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1(int location, IntBuffer value) {
        GL20C.glUniform1iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2(int location, IntBuffer value) {
        GL20C.glUniform2iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3(int location, IntBuffer value) {
        GL20C.glUniform3iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4(int location, IntBuffer value) {
        GL20C.glUniform4iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform1(int location, FloatBuffer value) {
        GL20C.glUniform1fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2(int location, FloatBuffer value) {
        GL20C.glUniform2fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3(int location, FloatBuffer value) {
        GL20C.glUniform3fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4(int location, FloatBuffer value) {
        GL20C.glUniform4fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        GL20C.glBlendEquationSeparate(modeRGB, modeAlpha);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 3.0
    //
    //

    @Override
    public int glGetInteger(int pname, int idx) {
        val res = GL30C.glGetIntegeri(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetString(int pname, int idx) {
        val res = GL30C.glGetStringi(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGenVertexArray() {
        val res = GL30C.glGenVertexArrays();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteVertexArray(int array) {
        GL30C.glDeleteVertexArrays(array);
        super.debugCheckError();
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30C.glBindVertexArray(array);
        super.debugCheckError();
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GL30C.glVertexAttribIPointer(index, size, type, stride, pointer);
        super.debugCheckError();
    }

    @Override
    public void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name) {
        GL30C.glBindFragDataLocation(program, colorNumber, name);
        super.debugCheckError();
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GL30C.glBindBufferBase(target, index, buffer);
        super.debugCheckError();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GL30C.glBindBufferRange(target, index, buffer, offset, size);
        super.debugCheckError();
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        GL30C.glBeginTransformFeedback(primitiveMode);
        super.debugCheckError();
    }

    @Override
    public void glEndTransformFeedback() {
        GL30C.glEndTransformFeedback();
        super.debugCheckError();
    }

    @Override
    public void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode) {
        GL30C.glTransformFeedbackVaryings(program, varyings, bufferMode);
        super.debugCheckError();
    }

    @Override
    public long glMapBufferRange(int target, long offset, long length, int access) {
        val res = PUnsafe.pork_directBufferAddress(GL30C.glMapBufferRange(target, offset, length, access, null));
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer) {
        val res = GL30C.glMapBufferRange(target, offset, length, access, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glFlushMappedBufferRange(int target, long offset, long length) {
        GL30C.glFlushMappedBufferRange(target, offset, length);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 3.1
    //
    //

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (this.OpenGL31) {
            GL31C.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
            super.debugCheckError();
        } else if (this.GL_ARB_copy_buffer) {
            ARBCopyBuffer.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_copy_buffer));
        }
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        if (this.OpenGL31) {
            GL31C.glDrawArraysInstanced(mode, first, count, instancecount);
            super.debugCheckError();
        } else if (this.GL_ARB_draw_instanced) {
            ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, instancecount);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_draw_instanced));
        }
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
        if (this.OpenGL31) {
            GL31C.glDrawElementsInstanced(mode, count, type, indices, instancecount);
            super.debugCheckError();
        } else if (this.GL_ARB_draw_instanced) {
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, count, type, indices, instancecount);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_draw_instanced));
        }
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        if (this.OpenGL31) {
            GL31C.glTexBuffer(target, internalFormat, buffer);
            super.debugCheckError();
        } else if (this.GL_ARB_texture_buffer_object) {
            ARBTextureBufferObject.glTexBufferARB(target, internalFormat, buffer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_texture_buffer_object));
        }
    }

    @Override
    public int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName) {
        if (this.OpenGL31) {
            val res = GL31C.glGetUniformBlockIndex(program, uniformBlockName);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetUniformBlockIndex(program, uniformBlockName);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_uniform_buffer_object));
        }
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        if (this.OpenGL31) {
            GL31C.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
            super.debugCheckError();
        } else if (this.GL_ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_uniform_buffer_object));
        }
    }

    @Override
    public int[] glGetUniformIndices(int program, CharSequence[] uniformNames) {
        long address = PUnsafe.allocateMemory(uniformNames.length * (long) Integer.BYTES);
        try {
            IntBuffer wrapped = DirectBufferHackery.wrapInt(address, uniformNames.length);
            if (this.OpenGL31) {
                GL31C.glGetUniformIndices(program, uniformNames, wrapped);
                super.debugCheckError();
            } else if (this.GL_ARB_uniform_buffer_object) {
                ARBUniformBufferObject.glGetUniformIndices(program, uniformNames, wrapped);
                super.debugCheckError();
            } else {
                throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_uniform_buffer_object));
            }

            int[] result = new int[uniformNames.length];
            wrapped.get(result);
            return result;
        } finally {
            PUnsafe.freeMemory(address);
        }
    }

    @Override
    public int glGetActiveUniformsi(int program, int uniformIndex, int pname) {
        if (this.OpenGL31) {
            val res = GL31C.glGetActiveUniformsi(program, uniformIndex, pname);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetActiveUniformsi(program, uniformIndex, pname);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_uniform_buffer_object));
        }
    }

    //
    //
    // OpenGL 3.2
    //
    //

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        if (this.OpenGL32) {
            GL32C.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
            super.debugCheckError();
        } else if (this.GL_ARB_draw_elements_base_vertex) {
            ARBDrawElementsBaseVertex.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_draw_elements_base_vertex));
        }
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        if (this.OpenGL32) {
            GL32C.nglMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
            super.debugCheckError();
        } else if (this.GL_ARB_draw_elements_base_vertex) {
            ARBDrawElementsBaseVertex.nglMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_draw_elements_base_vertex));
        }
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        if (this.OpenGL32) {
            val res = GL32C.glFenceSync(condition, flags);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sync) {
            val res = ARBSync.glFenceSync(condition, flags);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
        }
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        if (this.OpenGL32) {
            val res = GL32C.glClientWaitSync(sync, flags, timeout);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sync) {
            val res = ARBSync.glClientWaitSync(sync, flags, timeout);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
        }
    }

    @Override
    public int glGetSync(long sync, int pname) {
        if (this.OpenGL32) {
            val res = ARBSync.glGetSynci(sync, pname, null);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sync) {
            val res = ARBSync.glGetSynci(sync, pname, null);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
        }
    }

    @Override
    public void glDeleteSync(long sync) {
        if (this.OpenGL32) {
            GL32C.glDeleteSync(sync);
            super.debugCheckError();
        } else if (this.GL_ARB_sync) {
            ARBSync.glDeleteSync(sync);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
        }
    }

    //
    //
    // OpenGL 3.3
    //
    //

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        if (this.OpenGL33) {
            GL33C.glVertexAttribDivisor(index, divisor);
            super.debugCheckError();
        } else if (this.GL_ARB_instanced_arrays) {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_instanced_arrays));
        }
    }

    @Override
    public int glGenSampler() {
        if (this.OpenGL33) {
            val res = GL33C.glGenSamplers();
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sampler_objects) {
            val res = ARBSamplerObjects.glGenSamplers();
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sampler_objects));
        }
    }

    @Override
    public void glDeleteSampler(int sampler) {
        if (this.OpenGL33) {
            GL33C.glDeleteSamplers(sampler);
            super.debugCheckError();
        } else if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glDeleteSamplers(sampler);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sampler_objects));
        }
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        if (this.OpenGL33) {
            GL33C.glBindSampler(unit, sampler);
            super.debugCheckError();
        } else if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glBindSampler(unit, sampler);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sampler_objects));
        }
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, int param) {
        if (this.OpenGL33) {
            GL33C.glSamplerParameteri(sampler, pname, param);
            super.debugCheckError();
        } else if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameteri(sampler, pname, param);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sampler_objects));
        }
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, float param) {
        if (this.OpenGL33) {
            GL33C.glSamplerParameterf(sampler, pname, param);
            super.debugCheckError();
        } else if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameterf(sampler, pname, param);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sampler_objects));
        }
    }

    //
    //
    // OpenGL 4.1
    //
    //

    @Override
    public void glProgramUniform(int program, int location, int v0) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1i(program, location, v0);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1i(program, location, v0);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, int v0, int v1) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2i(program, location, v0, v1);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2i(program, location, v0, v1);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, int v0, int v1, int v2) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3i(program, location, v0, v1, v2);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3i(program, location, v0, v1, v2);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, int v0, int v1, int v2, int v3) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4i(program, location, v0, v1, v2, v3);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4i(program, location, v0, v1, v2, v3);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, float v0) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1f(program, location, v0);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1f(program, location, v0);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, float v0, float v1) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2f(program, location, v0, v1);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2f(program, location, v0, v1);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, float v0, float v1, float v2) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3f(program, location, v0, v1, v2);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3f(program, location, v0, v1, v2);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform(int program, int location, float v0, float v1, float v2, float v3) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4f(program, location, v0, v1, v2, v3);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4f(program, location, v0, v1, v2, v3);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform1(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1iv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1iv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform2(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2iv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2iv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform3(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3iv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3iv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform4(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4iv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4iv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform1(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1fv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1fv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform2(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2fv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2fv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform3(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3fv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3fv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform4(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4fv(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4fv(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    //
    //
    // OpenGL 4.2
    //
    //

    @Override
    public void glMemoryBarrier(int barriers) {
        if (this.OpenGL42) {
            GL42C.glMemoryBarrier(barriers);
            super.debugCheckError();
        } else if (this.GL_ARB_shader_image_load_store) {
            ARBShaderImageLoadStore.glMemoryBarrier(barriers);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_shader_image_load_store));
        }
    }

    //
    //
    // OpenGL 4.3
    //
    //

    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        if (this.OpenGL43) {
            GL43.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
            super.debugCheckError();
        } else if (this.GL_ARB_compute_shader) {
            ARBComputeShader.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_compute_shader));
        }
    }

    @Override
    public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
        if (this.OpenGL43) {
            GL43C.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
            super.debugCheckError();
        } else if (this.GL_ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_multi_draw_indirect));
        }
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride) {
        if (this.OpenGL43) {
            GL43C.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
            super.debugCheckError();
        } else if (this.GL_ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_multi_draw_indirect));
        }
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        if (this.OpenGL43) {
            val res = GL43C.glGetProgramResourceIndex(program, programInterface, name);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_program_interface_query) {
            val res = ARBProgramInterfaceQuery.glGetProgramResourceIndex(program, programInterface, name);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_program_interface_query));
        }
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        if (this.OpenGL43) {
            GL43C.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
            super.debugCheckError();
        } else if (this.GL_ARB_shader_storage_buffer_object) {
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_shader_storage_buffer_object));
        }
    }

    @Override
    public void glObjectLabel(int identifier, int name, @NonNull CharSequence label) {
        if (this.OpenGL43) {
            GL43C.glObjectLabel(identifier, name, label);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glObjectLabel(identifier, name, label);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public void glObjectPtrLabel(long ptr, @NonNull CharSequence label) {
        if (this.OpenGL43) {
            GL43C.glObjectPtrLabel(ptr, label);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glObjectPtrLabel(ptr, label);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public String glGetObjectLabel(int identifier, int name) {
        int maxLabelLength = this.limits().maxLabelLength();
        if (this.OpenGL43) {
            val res = GL43C.glGetObjectLabel(identifier, name, maxLabelLength);
            super.debugCheckError();
            return res;
        } else if (this.GL_KHR_debug) {
            val res = KHRDebug.glGetObjectLabel(identifier, name, maxLabelLength);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public String glGetObjectPtrLabel(long ptr) {
        int maxLabelLength = this.limits().maxLabelLength();
        if (this.OpenGL43) {
            val res = GL43C.glGetObjectPtrLabel(ptr, maxLabelLength);
            super.debugCheckError();
            return res;
        } else if (this.GL_KHR_debug) {
            val res = KHRDebug.glGetObjectPtrLabel(ptr, maxLabelLength);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    //
    //
    // OpenGL 4.4
    //
    //

    @Override
    public void glBufferStorage(int target, long data_size, long data, int flags) {
        if (this.OpenGL44) {
            GL44C.nglBufferStorage(target, data_size, data, flags);
            super.debugCheckError();
        } else if (this.GL_ARB_buffer_storage) {
            ARBBufferStorage.nglBufferStorage(target, data_size, data, flags);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_buffer_storage));
        }
    }

    @Override
    public void glBufferStorage(int target, @NonNull ByteBuffer data, int flags) {
        if (this.OpenGL44) {
            GL44C.glBufferStorage(target, data, flags);
            super.debugCheckError();
        } else if (this.GL_ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_buffer_storage));
        }
    }

    //
    //
    // OpenGL 4.5
    //
    //

    @Override
    public int glCreateBuffer() {
        if (this.OpenGL45) {
            val res = GL45C.glCreateBuffers();
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glCreateBuffers();
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferData(int buffer, long data_size, long data, int usage) {
        if (this.OpenGL45) {
            GL45C.nglNamedBufferData(buffer, data_size, data, usage);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.nglNamedBufferData(buffer, data_size, data, usage);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage) {
        if (this.OpenGL45) {
            GL45C.glNamedBufferData(buffer, data, usage);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferStorage(int buffer, long data_size, long data, int flags) {
        if (this.OpenGL45) {
            GL45C.nglNamedBufferStorage(buffer, data_size, data, flags);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.nglNamedBufferStorage(buffer, data_size, data, flags);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags) {
        if (this.OpenGL45) {
            GL45C.glNamedBufferStorage(buffer, data, flags);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, data, flags);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, long data_size, long data) {
        if (this.OpenGL45) {
            GL45C.nglNamedBufferSubData(buffer, offset, data_size, data);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.nglNamedBufferSubData(buffer, offset, data_size, data);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        if (this.OpenGL45) {
            GL45C.glNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, long data_size, long data) {
        if (this.OpenGL45) {
            GL45C.nglGetNamedBufferSubData(buffer, offset, data_size, data);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.nglGetNamedBufferSubData(buffer, offset, data_size, data);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        if (this.OpenGL45) {
            GL45C.glGetNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glGetNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public long glMapNamedBuffer(int buffer, int access) {
        if (this.OpenGL45) {
            val res = GL45C.nglMapNamedBuffer(buffer, access);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.nglMapNamedBuffer(buffer, access);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer) {
        if (this.OpenGL45) {
            val res = GL45C.glMapNamedBuffer(buffer, access, length, oldBuffer);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glMapNamedBuffer(buffer, access, length, oldBuffer);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long size, int access) {
        if (this.OpenGL45) {
            val res = PUnsafe.pork_directBufferAddress(GL45C.glMapNamedBufferRange(buffer, offset, size, access, null));
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = PUnsafe.pork_directBufferAddress(ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, size, access, null));
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer) {
        if (this.OpenGL45) {
            val res = GL45C.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glFlushMappedNamedBufferRange(int buffer, long offset, long length) {
        if (this.OpenGL45) {
            GL45C.glFlushMappedNamedBufferRange(buffer, offset, length);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        if (this.OpenGL45) {
            val res = GL45C.glUnmapNamedBuffer(buffer);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
        if (this.OpenGL45) {
            GL45C.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public int glCreateVertexArray() {
        if (this.OpenGL45) {
            val res = GL45C.glCreateVertexArrays();
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glCreateVertexArrays();
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayElementBuffer(int vaobj, int buffer) {
        if (this.OpenGL45) {
            GL45C.glVertexArrayElementBuffer(vaobj, buffer);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayElementBuffer(vaobj, buffer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        if (this.OpenGL45) {
            GL45C.glEnableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glEnableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glDisableVertexArrayAttrib(int vaobj, int index) {
        if (this.OpenGL45) {
            GL45C.glDisableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glDisableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        if (this.OpenGL45) {
            GL45C.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        if (this.OpenGL45) {
            GL45C.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        if (this.OpenGL45) {
            GL45C.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        if (this.OpenGL45) {
            GL45C.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride) {
        if (this.OpenGL45) {
            GL45C.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides) {
        if (buffers == null) {
            this.glVertexArrayVertexBuffers(vaobj, first, count, 0L, 0L, 0L);
        } else {
            long address = PUnsafe.allocateMemory((long) count * PUnsafe.addressSize());
            try {
                PointerBuffer wrappedOffsets = PointerBuffer.create(address, count).put(offsets, 0, count).clear();
                if (this.OpenGL45) {
                    ARBDirectStateAccess.glVertexArrayVertexBuffers(vaobj, first, buffers, wrappedOffsets, strides);
                    super.debugCheckError();
                } else if (this.GL_ARB_direct_state_access) {
                    ARBDirectStateAccess.glVertexArrayVertexBuffers(vaobj, first, buffers, wrappedOffsets, strides);
                    super.debugCheckError();
                } else {
                    throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
                }
            } finally {
                PUnsafe.freeMemory(address);
            }
        }
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides) {
        if (this.OpenGL45) {
            GL45C.nglVertexArrayVertexBuffers(vaobj, first, count, buffers, offsets, strides);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.nglVertexArrayVertexBuffers(vaobj, first, count, buffers, offsets, strides);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    //
    //
    // No OpenGL version
    //
    //

    @Override
    public void glBufferPageCommitmentARB(int target, long offset, long size, boolean commit) {
        if (this.GL_ARB_sparse_buffer) {
            ARBSparseBuffer.glBufferPageCommitmentARB(target, offset, size, commit);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sparse_buffer));
        }
    }

    @Override
    public void glNamedBufferPageCommitmentARB(int buffer, long offset, long size, boolean commit) {
        if ((this.OpenGL45 | this.GL_ARB_direct_state_access) & this.GL_ARB_sparse_buffer) {
            ARBSparseBuffer.glNamedBufferPageCommitmentARB(buffer, offset, size, commit);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_sparse_buffer));
        }
    }
}
