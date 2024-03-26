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

package net.daporkchop.fp2.gl.opengl.lwjgl2;

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.lwjgl2.extra.ExtraFunctions;
import net.daporkchop.fp2.gl.opengl.lwjgl2.extra.ExtraFunctionsProvider;
import net.daporkchop.lib.common.function.throwing.TPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.BufferChecks;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBDrawElementsBaseVertex;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.ARBProgramInterfaceQuery;
import org.lwjgl.opengl.ARBSamplerObjects;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBTextureBufferObject;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.ContextCapabilities;
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
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public final class GLAPILWJGL2 extends OpenGL implements GLAPI {
    private final ExtraFunctions extraFunctions;

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

    // OpenGL 4.2
    private final boolean OpenGL42;
    private final boolean GL_ARB_shader_image_load_store;

    // OpenGL 4.3
    private final boolean OpenGL43;
    private final boolean GL_ARB_multi_draw_indirect;
    private final boolean GL_ARB_program_interface_query;
    private final boolean GL_ARB_shader_storage_buffer_object;

    // OpenGL 4.4
    private final boolean OpenGL44;
    private final boolean GL_ARB_buffer_storage;

    // OpenGL 4.5
    private final boolean OpenGL45;
    private final boolean GL_ARB_direct_state_access;

    public GLAPILWJGL2() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        this.extraFunctions = ExtraFunctionsProvider.INSTANCE.get();

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

        // OpenGL 4.2
        this.OpenGL42 = capabilities.OpenGL42;
        this.GL_ARB_shader_image_load_store = !capabilities.OpenGL42 && capabilities.GL_ARB_shader_image_load_store;

        // OpenGL 4.3
        this.OpenGL43 = capabilities.OpenGL43;
        this.GL_ARB_multi_draw_indirect = !capabilities.OpenGL43 && capabilities.GL_ARB_multi_draw_indirect;
        this.GL_ARB_program_interface_query = !capabilities.OpenGL43 && capabilities.GL_ARB_program_interface_query;
        this.GL_ARB_shader_storage_buffer_object = !capabilities.OpenGL43 && capabilities.GL_ARB_shader_storage_buffer_object;

        // OpenGL 4.4
        this.OpenGL44 = capabilities.OpenGL44;
        this.GL_ARB_buffer_storage = !capabilities.OpenGL44 && capabilities.GL_ARB_buffer_storage;

        // OpenGL 4.5
        this.OpenGL45 = capabilities.OpenGL45;
        this.GL_ARB_direct_state_access = !capabilities.OpenGL45 && capabilities.GL_ARB_direct_state_access;
    }

    @Override
    public GLVersion determineVersion() { //TODO: make protected
        ContextCapabilities capabilities = GLContext.getCapabilities();

        return Stream.of(GLVersion.values())
                .filter((TPredicate<GLVersion, Throwable>) version -> {
                    try {
                        return (boolean) MethodHandles.publicLookup().findGetter(ContextCapabilities.class, "OpenGL" + version.major() + version.minor(), boolean.class).invokeExact(capabilities);
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
        super.debugCheckError();
    }

    @Override
    public void glDisable(int cap) {
        GL11.glDisable(cap);
        super.debugCheckError();
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    @Override
    public boolean glGetBoolean(int pname) {
        val res = GL11.glGetBoolean(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetBoolean(int pname, long data) {
        GL11.glGetBoolean(pname, DirectBufferHackery.wrapByte(data, 16)); //LWJGL2 will throw a fit if the buffer doesn't have at least 16 elements
        super.debugCheckError();
    }

    @Override
    public int glGetInteger(int pname) {
        val res = GL11.glGetInteger(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetInteger(int pname, long data) {
        GL11.glGetInteger(pname, DirectBufferHackery.wrapInt(data, 16));
        super.debugCheckError();
    }

    @Override
    public float glGetFloat(int pname) {
        val res = GL11.glGetFloat(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetFloat(int pname, long data) {
        GL11.glGetFloat(pname, DirectBufferHackery.wrapFloat(data, 16));
        super.debugCheckError();
    }

    @Override
    public double glGetDouble(int pname) {
        val res = GL11.glGetDouble(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetDouble(int pname, long data) {
        GL11.glGetDouble(pname, DirectBufferHackery.wrapDouble(data, 16));
        super.debugCheckError();
    }

    @Override
    public String glGetString(int pname) {
        val res = GL11.glGetString(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
        super.debugCheckError();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GL11.glDrawElements(mode, count, type, indices);
        super.debugCheckError();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices) {
        GL11.glDrawElements(mode, count, type, indices);
        super.debugCheckError();
    }

    @Override
    public int glGenTexture() {
        val res = GL11.glGenTextures();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
        super.debugCheckError();
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
        super.debugCheckError();
    }

    @Override
    public void glTexParameter(int target, int pname, int param) {
        GL11.glTexParameteri(target, pname, param);
        super.debugCheckError();
    }

    @Override
    public void glTexParameter(int target, int pname, float param) {
        GL11.glTexParameterf(target, pname, param);
        super.debugCheckError();
    }

    @Override
    public int glGetTexParameterInteger(int target, int pname) {
        val res = GL11.glGetTexParameteri(target, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public float glGetTexParameterFloat(int target, int pname) {
        val res = GL11.glGetTexParameterf(target, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data) {
        GL11.glTexImage1D(target, level, internalformat, width, 0, format, type, data == 0L
                ? null
                : DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage1DStorage(format, type, width)));
        super.debugCheckError();
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexImage1D(target, level, internalformat, width, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data) {
        GL11.glTexImage2D(target, level, internalformat, width, height, 0, format, type, data == 0L
                ? null
                : DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage2DStorage(format, type, width, height)));
        super.debugCheckError();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexImage2D(target, level, internalformat, width, height, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data) {
        GL11.glTexSubImage1D(target, level, xoffset, width, format, type, DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage1DStorage(format, type, width)));
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexSubImage1D(target, level, xoffset, width, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage2DStorage(format, type, width, height)));
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glClear(int mask) {
        GL11.glClear(mask);
        super.debugCheckError();
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
        super.debugCheckError();
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
        super.debugCheckError();
    }

    @Override
    public void glClearDepth(double depth) {
        GL11.glClearDepth(depth);
        super.debugCheckError();
    }

    @Override
    public void glDepthFunc(int func) {
        GL11.glDepthFunc(func);
        super.debugCheckError();
    }

    @Override
    public void glDepthMask(boolean flag) {
        GL11.glDepthMask(flag);
        super.debugCheckError();
    }

    @Override
    public void glClearStencil(int s) {
        GL11.glClearStencil(s);
        super.debugCheckError();
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
        super.debugCheckError();
    }

    @Override
    public void glStencilMask(int mask) {
        GL11.glStencilMask(mask);
        super.debugCheckError();
    }

    @Override
    public void glStencilOp(int sfail, int dpfail, int dppass) {
        GL11.glStencilOp(sfail, dpfail, dppass);
        super.debugCheckError();
    }

    @Override
    public void glPushClientAttrib(int mask) {
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
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, data == 0L
                ? null
                : DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage3DStorage(format, type, width, height, depth)));
        super.debugCheckError();
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage3DStorage(format, type, width, height, depth)));
        super.debugCheckError();
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.3
    //
    //

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.4
    //
    //

    @Override
    public void glMultiDrawArrays(int mode, long first, long count, int drawcount) {
        GL14.glMultiDrawArrays(mode, DirectBufferHackery.wrapInt(first, drawcount), DirectBufferHackery.wrapInt(count, drawcount));
        super.debugCheckError();
    }

    @Override
    public void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount) {
        this.extraFunctions.glMultiDrawElements(mode, count, type, indices, drawcount);
        super.debugCheckError();
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        GL14.glBlendColor(red, green, blue, alpha);
        super.debugCheckError();
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GL14.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 1.5
    //
    //

    @Override
    public int glGenBuffer() {
        val res = GL15.glGenBuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
        super.debugCheckError();
    }
    
    //LWJGL2 doesn't expose glBufferData with 64-bit data_size...
    private static final MethodHandle glBufferData;
    private static final MethodHandle nglBufferData;
    
    static {
        Field _glBufferData = ContextCapabilities.class.getDeclaredField("glBufferData");
        _glBufferData.setAccessible(true);
        glBufferData = MethodHandles.publicLookup().unreflectGetter(_glBufferData);
        
        Method _nglBufferData = GL15.class.getDeclaredMethod("nglBufferData", int.class, long.class, long.class, int.class, long.class);
        _nglBufferData.setAccessible(true);
        nglBufferData = MethodHandles.publicLookup().unreflect(_nglBufferData);
    }

    @Override
    public void glBufferData(int target, long data_size, long data, int usage) {
        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glBufferData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglBufferData.invokeExact(target, data_size, data, usage, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glBufferData(int target, @NonNull ByteBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
        super.debugCheckError();
    }
    
    //LWJGL2 doesn't expose glBufferSubData with 64-bit data_size...
    private static final MethodHandle glBufferSubData;
    private static final MethodHandle nglBufferSubData;
    
    static {
        Field _glBufferSubData = ContextCapabilities.class.getDeclaredField("glBufferSubData");
        _glBufferSubData.setAccessible(true);
        glBufferSubData = MethodHandles.publicLookup().unreflectGetter(_glBufferSubData);
        
        Method _nglBufferSubData = GL15.class.getDeclaredMethod("nglBufferSubData", int.class, long.class, long.class, long.class, long.class);
        _nglBufferSubData.setAccessible(true);
        nglBufferSubData = MethodHandles.publicLookup().unreflect(_nglBufferSubData);
    }

    @Override
    public void glBufferSubData(int target, long offset, long data_size, long data) {
		ContextCapabilities caps = GLContext.getCapabilities();
		long function_pointer = (long) glBufferSubData.invokeExact(caps);
		BufferChecks.checkFunctionAddress(function_pointer);
        nglBufferSubData.invokeExact(target, offset, data_size, data, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
        super.debugCheckError();
    }
    
    //LWJGL2 doesn't expose glGetBufferSubData with 64-bit data_size...
    private static final MethodHandle glGetBufferSubData;
    private static final MethodHandle nglGetBufferSubData;
    
    static {
        Field _glGetBufferSubData = ContextCapabilities.class.getDeclaredField("glGetBufferSubData");
        _glGetBufferSubData.setAccessible(true);
        glGetBufferSubData = MethodHandles.publicLookup().unreflectGetter(_glGetBufferSubData);
        
        Method _nglGetBufferSubData = GL15.class.getDeclaredMethod("nglGetBufferSubData", int.class, long.class, long.class, long.class, long.class);
        _nglGetBufferSubData.setAccessible(true);
        nglGetBufferSubData = MethodHandles.publicLookup().unreflect(_nglGetBufferSubData);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, long data_size, long data) {
        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glGetBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglGetBufferSubData.invokeExact(target, offset, data_size, data, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15.glGetBufferSubData(target, offset, data);
        super.debugCheckError();
    }

    @Override
    public long glMapBuffer(int target, int access) {
        val res = PUnsafe.pork_directBufferAddress(GL15.glMapBuffer(target, access, null));
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer oldBuffer) {
        val res = GL15.glMapBuffer(target, access, length, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        val res = GL15.glUnmapBuffer(target);
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
        val res = GL20.glCreateShader(type);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20.glDeleteShader(shader);
        super.debugCheckError();
    }

    @Override
    public void glShaderSource(int shader, @NonNull CharSequence... source) {
        GL20.glShaderSource(shader, source);
        super.debugCheckError();
    }

    @Override
    public void glCompileShader(int shader) {
        GL20.glCompileShader(shader);
        super.debugCheckError();
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        val res = GL20.glGetShaderi(shader, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        val res = GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL_INFO_LOG_LENGTH));
        super.debugCheckError();
        return res;
    }

    @Override
    public int glCreateProgram() {
        val res = GL20.glCreateProgram();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20.glDeleteProgram(program);
        super.debugCheckError();
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
        super.debugCheckError();
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GL20.glDetachShader(program, shader);
        super.debugCheckError();
    }

    @Override
    public void glLinkProgram(int program) {
        GL20.glLinkProgram(program);
        super.debugCheckError();
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        val res = GL20.glGetProgrami(program, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        val res = GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL_INFO_LOG_LENGTH));
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUseProgram(int program) {
        GL20.glUseProgram(program);
        super.debugCheckError();
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
        super.debugCheckError();
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        GL20.glDisableVertexAttribArray(index);
        super.debugCheckError();
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        super.debugCheckError();
    }

    @Override
    public void glBindAttribLocation(int program, int index, @NonNull CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
        super.debugCheckError();
    }

    @Override
    public int glGetUniformLocation(int program, @NonNull CharSequence name) {
        val res = GL20.glGetUniformLocation(program, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUniform(int location, int v0) {
        GL20.glUniform1i(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, int v0, int v1) {
        GL20.glUniform2i(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, int v0, int v1, int v2) {
        GL20.glUniform3i(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, int v0, int v1, int v2, int v3) {
        GL20.glUniform4i(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0) {
        GL20.glUniform1f(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0, float v1) {
        GL20.glUniform2f(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0, float v1, float v2) {
        GL20.glUniform3f(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform(int location, float v0, float v1, float v2, float v3) {
        GL20.glUniform4f(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        GL20.glBlendEquationSeparate(modeRGB, modeAlpha);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 3.0
    //
    //

    @Override
    public int glGetInteger(int pname, int idx) {
        val res = GL30.glGetInteger(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetString(int pname, int idx) {
        val res = GL30.glGetStringi(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGenVertexArray() {
        val res = GL30.glGenVertexArrays();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteVertexArray(int array) {
        GL30.glDeleteVertexArrays(array);
        super.debugCheckError();
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30.glBindVertexArray(array);
        super.debugCheckError();
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
        super.debugCheckError();
    }

    @Override
    public void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name) {
        GL30.glBindFragDataLocation(program, colorNumber, name);
        super.debugCheckError();
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GL30.glBindBufferBase(target, index, buffer);
        super.debugCheckError();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GL30.glBindBufferRange(target, index, buffer, offset, size);
        super.debugCheckError();
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        GL30.glBeginTransformFeedback(primitiveMode);
        super.debugCheckError();
    }

    @Override
    public void glEndTransformFeedback() {
        GL30.glEndTransformFeedback();
        super.debugCheckError();
    }

    @Override
    public void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode) {
        GL30.glTransformFeedbackVaryings(program, varyings, bufferMode);
        super.debugCheckError();
    }

    @Override
    public long glMapBufferRange(int target, long offset, long length, int access) {
        val res = PUnsafe.pork_directBufferAddress(GL30.glMapBufferRange(target, offset, length, access, null));
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer) {
        val res = GL30.glMapBufferRange(target, offset, length, access, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glFlushMappedBufferRange(int target, long offset, long length) {
        GL30.glFlushMappedBufferRange(target, offset, length);
        super.debugCheckError();
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
            super.debugCheckError();
        } else {
            GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
            super.debugCheckError();
        }
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        if (this.GL_ARB_draw_instanced) {
            ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, instancecount);
            super.debugCheckError();
        } else {
            GL31.glDrawArraysInstanced(mode, first, count, instancecount);
            super.debugCheckError();
        }
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        if (this.GL_ARB_texture_buffer_object) {
            ARBTextureBufferObject.glTexBufferARB(target, internalFormat, buffer);
            super.debugCheckError();
        } else {
            GL31.glTexBuffer(target, internalFormat, buffer);
            super.debugCheckError();
        }
    }

    @Override
    public int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName) {
        if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetUniformBlockIndex(program, uniformBlockName);
            super.debugCheckError();
            return res;
        } else {
            val res = GL31.glGetUniformBlockIndex(program, uniformBlockName);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        if (this.GL_ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
            super.debugCheckError();
        } else {
            GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
            super.debugCheckError();
        }
    }

    @Override
    public int[] glGetUniformIndices(int program, CharSequence[] uniformNames) {
        long address = PUnsafe.allocateMemory(uniformNames.length * (long) Integer.BYTES);
        try {
            IntBuffer wrapped = DirectBufferHackery.wrapInt(address, uniformNames.length);
            if (this.GL_ARB_uniform_buffer_object) {
                ARBUniformBufferObject.glGetUniformIndices(program, uniformNames, wrapped);
                super.debugCheckError();
            } else {
                GL31.glGetUniformIndices(program, uniformNames, wrapped);
                super.debugCheckError();
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
        if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetActiveUniformsi(program, uniformIndex, pname);
            super.debugCheckError();
            return res;
        } else {
            val res = GL31.glGetActiveUniformsi(program, uniformIndex, pname);
            super.debugCheckError();
            return res;
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
            super.debugCheckError();
        } else {
            GL32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
            super.debugCheckError();
        }
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        this.extraFunctions.glMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
        super.debugCheckError();
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        if (this.GL_ARB_sync) {
            val res = ARBSync.glFenceSync(condition, flags).getPointer();
            super.debugCheckError();
            return res;
        } else {
            val res = GL32.glFenceSync(condition, flags).getPointer();
            super.debugCheckError();
            return res;
        }
    }

    private static final MethodHandle GLSync_CTOR;

    static {
        Constructor<GLSync> ctor = GLSync.class.getDeclaredConstructor(long.class);
        ctor.setAccessible(true);
        GLSync_CTOR = MethodHandles.publicLookup().unreflectConstructor(ctor);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        if (this.GL_ARB_sync) {
            val res = ARBSync.glClientWaitSync((GLSync) GLSync_CTOR.invokeExact(sync), flags, timeout);
            super.debugCheckError();
            return res;
        } else {
            val res = GL32.glClientWaitSync((GLSync) GLSync_CTOR.invokeExact(sync), flags, timeout);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public int glGetSync(long sync, int pname) {
        if (this.GL_ARB_sync) {
            val res = ARBSync.glGetSynci((GLSync) GLSync_CTOR.invokeExact(sync), pname);
            super.debugCheckError();
            return res;
        } else {
            val res = GL32.glGetSynci((GLSync) GLSync_CTOR.invokeExact(sync), pname);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glDeleteSync(long sync) {
        if (this.GL_ARB_sync) {
            ARBSync.glDeleteSync((GLSync) GLSync_CTOR.invokeExact(sync));
            super.debugCheckError();
        } else {
            GL32.glDeleteSync((GLSync) GLSync_CTOR.invokeExact(sync));
            super.debugCheckError();
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
            super.debugCheckError();
        } else {
            GL33.glVertexAttribDivisor(index, divisor);
            super.debugCheckError();
        }
    }

    @Override
    public int glGenSampler() {
        if (this.GL_ARB_sampler_objects) {
            val res = ARBSamplerObjects.glGenSamplers();
            super.debugCheckError();
            return res;
        } else {
            val res = GL33.glGenSamplers();
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glDeleteSampler(int sampler) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glDeleteSamplers(sampler);
            super.debugCheckError();
        } else {
            GL33.glDeleteSamplers(sampler);
            super.debugCheckError();
        }
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glBindSampler(unit, sampler);
            super.debugCheckError();
        } else {
            GL33.glBindSampler(unit, sampler);
            super.debugCheckError();
        }
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, int param) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameteri(sampler, pname, param);
            super.debugCheckError();
        } else {
            GL33.glSamplerParameteri(sampler, pname, param);
            super.debugCheckError();
        }
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, float param) {
        if (this.GL_ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameterf(sampler, pname, param);
            super.debugCheckError();
        } else {
            GL33.glSamplerParameterf(sampler, pname, param);
            super.debugCheckError();
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
            super.debugCheckError();
        } else {
            GL42.glMemoryBarrier(barriers);
            super.debugCheckError();
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
            super.debugCheckError();
        } else {
            GL43.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
            super.debugCheckError();
        }
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride) {
        if (this.GL_ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
            super.debugCheckError();
        } else {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
            super.debugCheckError();
        }
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        if (this.GL_ARB_program_interface_query) {
            val res = ARBProgramInterfaceQuery.glGetProgramResourceIndex(program, programInterface, name);
            super.debugCheckError();
            return res;
        } else {
            val res = GL43.glGetProgramResourceIndex(program, programInterface, name);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        if (this.GL_ARB_shader_storage_buffer_object) {
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
            super.debugCheckError();
        } else {
            GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
            super.debugCheckError();
        }
    }

    //
    //
    // OpenGL 4.4
    //
    //
    
    private static final MethodHandle glBufferStorage;
    private static final MethodHandle nglBufferStorage;

    static {
        Field _glBufferStorage = ContextCapabilities.class.getDeclaredField("glBufferStorage");
        _glBufferStorage.setAccessible(true);
        glBufferStorage = MethodHandles.publicLookup().unreflectGetter(_glBufferStorage);
        
        Method _nglBufferStorage = GL44.class.getDeclaredMethod("nglBufferStorage", int.class, long.class, long.class, int.class, long.class);
        _nglBufferStorage.setAccessible(true);
        nglBufferStorage = MethodHandles.publicLookup().unreflect(_nglBufferStorage);
    }

    @Override
    public void glBufferStorage(int target, long data_size, long data, int flags) {
        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glBufferStorage.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglBufferStorage.invokeExact(target, data_size, data, flags, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glBufferStorage(int target, @NonNull ByteBuffer data, int flags) {
        if (this.GL_ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
            super.debugCheckError();
        } else {
            GL44.glBufferStorage(target, data, flags);
            super.debugCheckError();
        }
    }

    //
    //
    // OpenGL 4.5
    //
    //

    @Override
    public int glCreateBuffer() {
        if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glCreateBuffers();
            super.debugCheckError();
            return res;
        } else {
            val res = GL45.glCreateBuffers();
            super.debugCheckError();
            return res;
        }
    }

    //LWJGL2 doesn't expose glNamedBufferData with 64-bit data_size...
    private static final MethodHandle glNamedBufferData;
    private static final MethodHandle nglNamedBufferData;

    static {
        Field _glNamedBufferData = ContextCapabilities.class.getDeclaredField("glNamedBufferData");
        _glNamedBufferData.setAccessible(true);
        glNamedBufferData = MethodHandles.publicLookup().unreflectGetter(_glNamedBufferData);

        Method _nglNamedBufferData = GL45.class.getDeclaredMethod("nglNamedBufferData", int.class, long.class, long.class, int.class, long.class);
        _nglNamedBufferData.setAccessible(true);
        nglNamedBufferData = MethodHandles.publicLookup().unreflect(_nglNamedBufferData);
    }

    @Override
    public void glNamedBufferData(int buffer, long data_size, long data, int usage) {
		ContextCapabilities caps = GLContext.getCapabilities();
		long function_pointer = (long) glNamedBufferData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglNamedBufferData.invokeExact(buffer, data_size, data, usage, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
            super.debugCheckError();
        } else {
            GL45.glNamedBufferData(buffer, data, usage);
            super.debugCheckError();
        }
    }

    //LWJGL2 doesn't expose glNamedBufferStorage with 64-bit data_size...
    private static final MethodHandle glNamedBufferStorage;
    private static final MethodHandle nglNamedBufferStorage;

    static {
        Field _glNamedBufferStorage = ContextCapabilities.class.getDeclaredField("glNamedBufferStorage");
        _glNamedBufferStorage.setAccessible(true);
        glNamedBufferStorage = MethodHandles.publicLookup().unreflectGetter(_glNamedBufferStorage);

        Method _nglNamedBufferStorage = GL45.class.getDeclaredMethod("nglNamedBufferStorage", int.class, long.class, long.class, int.class, long.class);
        _nglNamedBufferStorage.setAccessible(true);
        nglNamedBufferStorage = MethodHandles.publicLookup().unreflect(_nglNamedBufferStorage);
    }

    @Override
    public void glNamedBufferStorage(int buffer, long data_size, long data, int flags) {
		ContextCapabilities caps = GLContext.getCapabilities();
		long function_pointer = (long) glNamedBufferStorage.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglNamedBufferStorage.invokeExact(buffer, data_size, data, flags, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, data, flags);
            super.debugCheckError();
        } else {
            GL45.glNamedBufferStorage(buffer, data, flags);
            super.debugCheckError();
        }
    }

    //LWJGL2 doesn't expose glNamedBufferSubData with 64-bit data_size...
    private static final MethodHandle glNamedBufferSubData;
    private static final MethodHandle nglNamedBufferSubData;
    
    static {
        Field _glNamedBufferSubData = ContextCapabilities.class.getDeclaredField("glNamedBufferSubData");
        _glNamedBufferSubData.setAccessible(true);
        glNamedBufferSubData = MethodHandles.publicLookup().unreflectGetter(_glNamedBufferSubData);
        
        Method _nglNamedBufferSubData = GL45.class.getDeclaredMethod("nglNamedBufferSubData", int.class, long.class, long.class, long.class, long.class);
        _nglNamedBufferSubData.setAccessible(true);
        nglNamedBufferSubData = MethodHandles.publicLookup().unreflect(_nglNamedBufferSubData);
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, long data_size, long data) {
		ContextCapabilities caps = GLContext.getCapabilities();
		long function_pointer = (long) glNamedBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglNamedBufferSubData.invokeExact(buffer, offset, data_size, data, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else {
            GL45.glNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        }
    }
    
    //LWJGL2 doesn't expose glGetNamedBufferSubData with 64-bit data_size...
    private static final MethodHandle glGetNamedBufferSubData;
    private static final MethodHandle nglGetNamedBufferSubData;
    
    static {
        Field _glGetNamedBufferSubData = ContextCapabilities.class.getDeclaredField("glGetNamedBufferSubData");
        _glGetNamedBufferSubData.setAccessible(true);
        glGetNamedBufferSubData = MethodHandles.publicLookup().unreflectGetter(_glGetNamedBufferSubData);
        
        Method _nglGetNamedBufferSubData = GL45.class.getDeclaredMethod("nglGetNamedBufferSubData", int.class, long.class, long.class, long.class, long.class);
        _nglGetNamedBufferSubData.setAccessible(true);
        nglGetNamedBufferSubData = MethodHandles.publicLookup().unreflect(_nglGetNamedBufferSubData);
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, long data_size, long data) {
		ContextCapabilities caps = GLContext.getCapabilities();
		long function_pointer = (long) glGetNamedBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglGetNamedBufferSubData.invokeExact(buffer, offset, data_size, data, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glGetNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else {
            GL45.glGetNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        }
    }

    @Override
    public long glMapNamedBuffer(int buffer, int access) {
        if (this.GL_ARB_direct_state_access) {
            val res = PUnsafe.pork_directBufferAddress(ARBDirectStateAccess.glMapNamedBuffer(buffer, access, null));
            super.debugCheckError();
            return res;
        } else {
            val res = PUnsafe.pork_directBufferAddress(GL45.glMapNamedBuffer(buffer, access, null));
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer) {
        if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glMapNamedBuffer(buffer, access, length, oldBuffer);
            super.debugCheckError();
            return res;
        } else {
            val res = GL45.glMapNamedBuffer(buffer, access, length, oldBuffer);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long size, int access) {
        if (this.GL_ARB_direct_state_access) {
            val res = PUnsafe.pork_directBufferAddress(ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, size, access, null));
            super.debugCheckError();
            return res;
        } else {
            val res = PUnsafe.pork_directBufferAddress(GL45.glMapNamedBufferRange(buffer, offset, size, access, null));
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer) {
        if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
            super.debugCheckError();
            return res;
        } else {
            val res = GL45.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glFlushMappedNamedBufferRange(int buffer, long offset, long length) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
            super.debugCheckError();
        } else {
            GL45.glFlushMappedNamedBufferRange(buffer, offset, length);
            super.debugCheckError();
        }
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
            super.debugCheckError();
            return res;
        } else {
            val res = GL45.glUnmapNamedBuffer(buffer);
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
            super.debugCheckError();
        } else {
            GL45.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
            super.debugCheckError();
        }
    }

    @Override
    public int glCreateVertexArray() {
        if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glCreateVertexArrays();
            super.debugCheckError();
            return res;
        } else {
            val res = GL45.glCreateVertexArrays();
            super.debugCheckError();
            return res;
        }
    }

    @Override
    public void glVertexArrayElementBuffer(int vaobj, int buffer) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayElementBuffer(vaobj, buffer);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayElementBuffer(vaobj, buffer);
            super.debugCheckError();
        }
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glEnableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        } else {
            GL45.glEnableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        }
    }

    @Override
    public void glDisableVertexArrayAttrib(int vaobj, int index) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glDisableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        } else {
            GL45.glDisableVertexArrayAttrib(vaobj, index);
            super.debugCheckError();
        }
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
            super.debugCheckError();
        }
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
            super.debugCheckError();
        }
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
            super.debugCheckError();
        }
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
            super.debugCheckError();
        }
    }

    @Override
    public void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride) {
        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
            super.debugCheckError();
        }
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides) {
        if (buffers == null) {
            this.glVertexArrayVertexBuffers(vaobj, first, count, 0L, 0L, 0L);
        } else {
            long size = count * (long) (Integer.BYTES + Integer.BYTES + PUnsafe.addressSize());
            long address = DirectBufferHackery.allocateTemporary(size);
            try {
                long buffersAddress = address;
                long stridesAddress = buffersAddress + (long) count * Integer.BYTES;
                long offsetsAddress = stridesAddress + (long) count * Integer.BYTES;

                for (int i = 0; i < count; i++) {
                    PUnsafe.putInt(buffersAddress + (long) i * Integer.BYTES, buffers[i]);
                    PUnsafe.putInt(stridesAddress + (long) i * Integer.BYTES, strides[i]);
                    PUnsafe.putAddress(offsetsAddress + (long) i * PUnsafe.addressSize(), offsets[i]);
                }

                this.glVertexArrayVertexBuffers(vaobj, first, count, buffersAddress, offsetsAddress, stridesAddress);
            } finally {
                DirectBufferHackery.freeTemporary(address, size);
            }
        }
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides) {
        IntBuffer wrappedBuffers;
        PointerBuffer wrappedOffsets;
        IntBuffer wrappedStrides;
        if (buffers != 0L) {
            wrappedBuffers = DirectBufferHackery.wrapInt(buffers, count);
            wrappedOffsets = new PointerBuffer(DirectBufferHackery.wrapByte(offsets, count * PUnsafe.addressSize()));
            wrappedStrides = DirectBufferHackery.wrapInt(strides, count);
        } else {
            wrappedBuffers = null;
            wrappedOffsets = null;
            wrappedStrides = null;
        }

        if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayVertexBuffers(vaobj, first, count, wrappedBuffers, wrappedOffsets, wrappedStrides);
            super.debugCheckError();
        } else {
            GL45.glVertexArrayVertexBuffers(vaobj, first, count, wrappedBuffers, wrappedOffsets, wrappedStrides);
            super.debugCheckError();
        }
    }
}
