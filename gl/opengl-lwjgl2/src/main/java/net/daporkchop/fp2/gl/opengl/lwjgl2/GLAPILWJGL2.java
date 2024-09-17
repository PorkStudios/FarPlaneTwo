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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.opengl.lwjgl2.extra.ExtraFunctions;
import net.daporkchop.fp2.gl.opengl.lwjgl2.extra.ExtraFunctionsProvider;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputCallback;
import net.daporkchop.lib.common.function.throwing.TPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.BufferChecks;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBBaseInstance;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBDrawElementsBaseVertex;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBIndirectParameters;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBInvalidateSubdata;
import org.lwjgl.opengl.ARBMultiBind;
import org.lwjgl.opengl.ARBProgramInterfaceQuery;
import org.lwjgl.opengl.ARBSamplerObjects;
import org.lwjgl.opengl.ARBSeparateShaderObjects;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBSparseBuffer;
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
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.opengl.KHRDebug;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public final class GLAPILWJGL2 extends OpenGL {
    public static OpenGL forCurrent() {
        return new GLAPILWJGL2();
    }

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

    // OpenGL 4.1
    private final boolean OpenGL41;
    private final boolean GL_ARB_separate_shader_objects;

    // OpenGL 4.2
    private final boolean OpenGL42;
    private final boolean GL_ARB_base_instance;
    private final boolean GL_ARB_shader_image_load_store;

    // OpenGL 4.3
    private final boolean OpenGL43;
    private final boolean GL_ARB_compute_shader;
    private final boolean GL_ARB_invalidate_subdata;
    private final boolean GL_ARB_multi_draw_indirect;
    private final boolean GL_ARB_program_interface_query;
    private final boolean GL_ARB_shader_storage_buffer_object;
    private final boolean GL_KHR_debug;

    // OpenGL 4.4
    private final boolean OpenGL44;
    private final boolean GL_ARB_buffer_storage;
    private final boolean GL_ARB_multi_bind;

    // OpenGL 4.5
    private final boolean OpenGL45;
    private final boolean GL_ARB_clip_control;
    private final boolean GL_ARB_direct_state_access;

    // OpenGL 4.6
    private final boolean OpenGL46;
    private final boolean GL_ARB_indirect_parameters;

    // No OpenGL version
    private final boolean GL_ARB_debug_output;
    private final boolean GL_ARB_sparse_buffer;

    //funny workarounds
    private final long glMultiDrawElements;

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

        // OpenGL 4.1
        this.OpenGL41 = capabilities.OpenGL41;
        this.GL_ARB_separate_shader_objects = !capabilities.OpenGL41 && capabilities.GL_ARB_separate_shader_objects;

        // OpenGL 4.2
        this.OpenGL42 = capabilities.OpenGL42;
        this.GL_ARB_base_instance = !capabilities.OpenGL42 && capabilities.GL_ARB_base_instance;
        this.GL_ARB_shader_image_load_store = !capabilities.OpenGL42 && capabilities.GL_ARB_shader_image_load_store;

        // OpenGL 4.3
        this.OpenGL43 = capabilities.OpenGL43;
        this.GL_ARB_compute_shader = !capabilities.OpenGL43 && capabilities.GL_ARB_compute_shader;
        this.GL_ARB_invalidate_subdata = !capabilities.OpenGL43 && capabilities.GL_ARB_invalidate_subdata;
        this.GL_ARB_multi_draw_indirect = !capabilities.OpenGL43 && capabilities.GL_ARB_multi_draw_indirect;
        this.GL_ARB_program_interface_query = !capabilities.OpenGL43 && capabilities.GL_ARB_program_interface_query;
        this.GL_ARB_shader_storage_buffer_object = !capabilities.OpenGL43 && capabilities.GL_ARB_shader_storage_buffer_object;
        this.GL_KHR_debug = !capabilities.OpenGL43 && capabilities.GL_KHR_debug;

        // OpenGL 4.4
        this.OpenGL44 = capabilities.OpenGL44;
        this.GL_ARB_buffer_storage = !capabilities.OpenGL44 && capabilities.GL_ARB_buffer_storage;
        this.GL_ARB_multi_bind = !capabilities.OpenGL44 && capabilities.GL_ARB_multi_bind;

        // OpenGL 4.5
        this.OpenGL45 = capabilities.OpenGL45;
        this.GL_ARB_clip_control = !capabilities.OpenGL45 && capabilities.GL_ARB_clip_control;
        this.GL_ARB_direct_state_access = !capabilities.OpenGL45 && capabilities.GL_ARB_direct_state_access;

        // OpenGL 4.6
        this.OpenGL46 = false /*capabilities.OpenGL46*/;
        this.GL_ARB_indirect_parameters = !this.OpenGL46 && capabilities.GL_ARB_indirect_parameters;

        // No OpenGL version
        this.GL_ARB_debug_output = capabilities.GL_ARB_debug_output;
        this.GL_ARB_sparse_buffer = capabilities.GL_ARB_sparse_buffer;

        //funny workarounds
        this.glMultiDrawElements = getFunctionAddress("glMultiDrawElements");
    }

    @Override
    protected GLVersion determineVersion() {
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

    private static final MethodHandle GLContext_getFunctionAddress;

    static {
        Method _GLContext_getFunctionAddress = GLContext.class.getDeclaredMethod("getFunctionAddress", String.class);
        _GLContext_getFunctionAddress.setAccessible(true);
        GLContext_getFunctionAddress = MethodHandles.publicLookup().unreflect(_GLContext_getFunctionAddress);
    }

    private static long getFunctionAddress(@NonNull String name) {
        long addr = (long) GLContext_getFunctionAddress.invokeExact(name);
        if (addr == 0L) {
            throw new IllegalStateException("unable to find function " + name);
        }
        return addr;
    }

    private static final MethodHandle APIUtil_getBufferInt;

    private static final MethodHandle StateTracker_getIndirectBuffer;
    private static final MethodHandle StateTracker_createVAO; // (ContextCapabilities, int) -> void
    private static final MethodHandle StateTracker_setVAOElementArrayBuffer; // (ContextCapabilities, int, int) -> void

    static {
        Class<?> _APIUtil = Class.forName("org.lwjgl.opengl.APIUtil");
        Method _APIUtil_getBufferInt = _APIUtil.getDeclaredMethod("getBufferInt", ContextCapabilities.class);
        _APIUtil_getBufferInt.setAccessible(true);
        APIUtil_getBufferInt = MethodHandles.publicLookup().unreflect(_APIUtil_getBufferInt);
        
        Class<?> _StateTracker = Class.forName("org.lwjgl.opengl.StateTracker");
        Method _StateTracker_getReferences = _StateTracker.getDeclaredMethod("getReferences", ContextCapabilities.class);
        _StateTracker_getReferences.setAccessible(true);
        Class<?> _References = _StateTracker_getReferences.getReturnType();
        MethodHandle StateTracker_getReferences = MethodHandles.publicLookup().unreflect(_StateTracker_getReferences);

        Class<?> _BaseReferences = _References.getSuperclass();
        Field _BaseReferences_indirectBuffer = _BaseReferences.getDeclaredField("indirectBuffer");
        _BaseReferences_indirectBuffer.setAccessible(true);
        MethodHandle _BaseReferences_getIndirectBuffer = MethodHandles.publicLookup().unreflectGetter(_BaseReferences_indirectBuffer);
        MethodHandle _References_getIndirectBuffer = _BaseReferences_getIndirectBuffer.asType(MethodType.methodType(int.class, _References));
        StateTracker_getIndirectBuffer = MethodHandles.filterReturnValue(
                StateTracker_getReferences,
                _References_getIndirectBuffer);

        Field _ContextCapabilities_tracker = ContextCapabilities.class.getDeclaredField("tracker");
        _ContextCapabilities_tracker.setAccessible(true);
        Field _StateTracker_vaoMap = _StateTracker.getDeclaredField("vaoMap");
        _StateTracker_vaoMap.setAccessible(true);
        Class<?> _StateTracker_VaoState = Class.forName("org.lwjgl.opengl.StateTracker$VAOState");
        Constructor<?> _StateTracker_VaoState_$init$ = _StateTracker_VaoState.getDeclaredConstructor();
        _StateTracker_VaoState_$init$.setAccessible(true);
        Field _StateTracker_VaoState_elementArrayBuffer = _StateTracker_VaoState.getDeclaredField("elementArrayBuffer");
        _StateTracker_VaoState_elementArrayBuffer.setAccessible(true);

        Class<?> _FastIntMap = _StateTracker_vaoMap.getType();
        Method _FastIntMap_put = _FastIntMap.getDeclaredMethod("put", int.class, Object.class);
        _FastIntMap_put.setAccessible(true);
        Method _FastIntMap_get = _FastIntMap.getDeclaredMethod("get", int.class);
        _FastIntMap_get.setAccessible(true);

        MethodHandle _ContextCapabilities_getTracker = MethodHandles.publicLookup().unreflectGetter(_ContextCapabilities_tracker);
        MethodHandle _StateTracker_getVaoMap = MethodHandles.publicLookup().unreflectGetter(_StateTracker_vaoMap);
        MethodHandle _StateTracker_VaoState_setElementArrayBuffer = MethodHandles.publicLookup().unreflectSetter(_StateTracker_VaoState_elementArrayBuffer);

        MethodHandle _ContextCapabilities_tracker_vaoMap = MethodHandles.filterReturnValue(_ContextCapabilities_getTracker, _StateTracker_getVaoMap);

        StateTracker_createVAO =
                MethodHandles.collectArguments(
                                MethodHandles.filterArguments(
                                        MethodHandles.publicLookup().unreflect(_FastIntMap_put)
                                                .asType(MethodType.methodType(void.class, _FastIntMap, int.class, _StateTracker_VaoState)),
                                        0, _ContextCapabilities_tracker_vaoMap),
                                2, MethodHandles.publicLookup().unreflectConstructor(_StateTracker_VaoState_$init$));

        MethodHandle _StateTracker_getVAO = MethodHandles.filterArguments(
                MethodHandles.publicLookup().unreflect(_FastIntMap_get).asType(MethodType.methodType(_StateTracker_VaoState, _FastIntMap, int.class)),
                0, _ContextCapabilities_tracker_vaoMap);

        StateTracker_setVAOElementArrayBuffer = MethodHandles.collectArguments(
                _StateTracker_VaoState_setElementArrayBuffer,
                0, _StateTracker_getVAO);
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
    public boolean glIsEnabled(int cap) {
        val res = GL11.glIsEnabled(cap);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    @Override
    public void glFlush() {
        GL11.glFlush();
        super.debugCheckError();
    }

    @Override
    public boolean glGetBoolean(int pname) {
        val res = GL11.glGetBoolean(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetBoolean(int pname, @NonNull ByteBuffer data) {
        GL11.glGetBoolean(pname, DirectBufferHackery.wrapByte(DirectBufferHackery.address(data), 16)); //LWJGL2 will throw a fit if the buffer doesn't have at least 16 elements
        super.debugCheckError();
    }

    @Override
    public int glGetInteger(int pname) {
        val res = GL11.glGetInteger(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetInteger(int pname, @NonNull IntBuffer data) {
        GL11.glGetInteger(pname, DirectBufferHackery.wrapInt(DirectBufferHackery.address(data), 16)); //LWJGL2 will throw a fit if the buffer doesn't have at least 16 elements
        super.debugCheckError();
    }

    @Override
    public float glGetFloat(int pname) {
        val res = GL11.glGetFloat(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetFloat(int pname, @NonNull FloatBuffer data) {
        GL11.glGetFloat(pname, DirectBufferHackery.wrapFloat(DirectBufferHackery.address(data), 16)); //LWJGL2 will throw a fit if the buffer doesn't have at least 16 elements
        super.debugCheckError();
    }

    @Override
    public double glGetDouble(int pname) {
        val res = GL11.glGetDouble(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetDouble(int pname, @NonNull DoubleBuffer data) {
        GL11.glGetDouble(pname, DirectBufferHackery.wrapDouble(DirectBufferHackery.address(data), 16)); //LWJGL2 will throw a fit if the buffer doesn't have at least 16 elements
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

    //LWJGL2 doesn't expose glMultiDrawElements() at all, but it does have glShaderBinary() which happens to have a compatible function
    // signature. We're going to call nglShaderBinary(), but pass it the pointer to glMultiDrawElements() instead!
    private static final MethodHandle nglShaderBinary;

    static {
        Method _nglShaderBinary = GL41.class.getDeclaredMethod("nglShaderBinary", int.class, long.class, int.class, long.class, int.class, long.class);
        _nglShaderBinary.setAccessible(true);
        nglShaderBinary = MethodHandles.publicLookup().unreflect(_nglShaderBinary);
    }

    @Override
    public void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount) {
        nglShaderBinary.invokeExact(mode, count, type, indices, drawcount, this.glMultiDrawElements);
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
    public int glGetBufferParameteri(int target, int pname) {
        val res = GL15.glGetBufferParameteri(target, pname);
        super.debugCheckError();
        return res;
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
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        GL20.glGetProgram(program, pname, params);
        super.debugCheckError();
    }

    @Override
    public int[] glGetProgramiv(int program, int pname, int count) {
        IntBuffer buffer = (IntBuffer) APIUtil_getBufferInt.invokeExact(GLContext.getCapabilities());
        GL20.glGetProgram(program, pname, buffer);
        super.debugCheckError();
        int[] res = new int[count];
        buffer.slice().get(res);
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
    public int glGetAttribLocation(int program, @NonNull CharSequence name) {
        val res = GL20.glGetAttribLocation(program, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull IntBuffer size, @NonNull IntBuffer type) {
        long allocSize = 2L * Integer.BYTES;
        long address = DirectBufferHackery.allocateTemporary(allocSize);
        try {
            IntBuffer sizeType = DirectBufferHackery.wrapInt(address, 2);
            val res = GL20.glGetActiveAttrib(program, index, bufSize, sizeType);
            super.debugCheckError();

            //copy size and type into the destination buffers
            size.put(size.position(), sizeType.get(0));
            type.put(type.position(), sizeType.get(1));
            return res;
        } finally {
            DirectBufferHackery.freeTemporary(address, allocSize);
        }
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull int[] size, @NonNull int[] type) {
        long allocSize = 2L * Integer.BYTES;
        long address = DirectBufferHackery.allocateTemporary(allocSize);
        try {
            IntBuffer sizeType = DirectBufferHackery.wrapInt(address, 2);
            val res = GL20.glGetActiveAttrib(program, index, bufSize, sizeType);
            super.debugCheckError();

            //copy size and type into the destination arrays
            size[0] = sizeType.get(0);
            type[0] = sizeType.get(1);
            return res;
        } finally {
            DirectBufferHackery.freeTemporary(address, allocSize);
        }
    }

    @Override
    public int glGetUniformLocation(int program, @NonNull CharSequence name) {
        val res = GL20.glGetUniformLocation(program, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUniform1i(int location, int v0) {
        GL20.glUniform1i(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform2i(int location, int v0, int v1) {
        GL20.glUniform2i(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform3i(int location, int v0, int v1, int v2) {
        GL20.glUniform3i(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform4i(int location, int v0, int v1, int v2, int v3) {
        GL20.glUniform4i(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1f(int location, float v0) {
        GL20.glUniform1f(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        GL20.glUniform2f(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        GL20.glUniform3f(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        GL20.glUniform4f(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1i(int location, IntBuffer value) {
        GL20.glUniform1(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2i(int location, IntBuffer value) {
        GL20.glUniform2(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3i(int location, IntBuffer value) {
        GL20.glUniform3(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4i(int location, IntBuffer value) {
        GL20.glUniform4(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform1f(int location, FloatBuffer value) {
        GL20.glUniform1(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2f(int location, FloatBuffer value) {
        GL20.glUniform2(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3f(int location, FloatBuffer value) {
        GL20.glUniform3(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4f(int location, FloatBuffer value) {
        GL20.glUniform4(location, value);
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

    @Override
    public void glUniform1ui(int location, int v0) {
        GL30.glUniform1ui(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform2ui(int location, int v0, int v1) {
        GL30.glUniform2ui(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform3ui(int location, int v0, int v1, int v2) {
        GL30.glUniform3ui(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform4ui(int location, int v0, int v1, int v2, int v3) {
        GL30.glUniform4ui(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1ui(int location, IntBuffer value) {
        GL30.glUniform1u(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2ui(int location, IntBuffer value) {
        GL30.glUniform2u(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3ui(int location, IntBuffer value) {
        GL30.glUniform3u(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4ui(int location, IntBuffer value) {
        GL30.glUniform4u(location, value);
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
            GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
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
            GL31.glDrawArraysInstanced(mode, first, count, instancecount);
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
            GL31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
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
            GL31.glTexBuffer(target, internalFormat, buffer);
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
            val res = GL31.glGetUniformBlockIndex(program, uniformBlockName);
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
    public int glGetActiveUniformBlocki(int program, int uniformBlockIndex, int pname) {
        if (this.OpenGL31) {
            val res = GL31.glGetActiveUniformBlocki(program, uniformBlockIndex, pname);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetActiveUniformBlocki(program, uniformBlockIndex, pname);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_uniform_buffer_object));
        }
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex, int bufSize) {
        if (this.OpenGL31) {
            val res = GL31.glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_uniform_buffer_object));
        }
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        if (this.OpenGL31) {
            GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
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
                GL31.glGetUniformIndices(program, uniformNames, wrapped);
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
            val res = GL31.glGetActiveUniformsi(program, uniformIndex, pname);
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

    @Override
    public String glGetActiveUniformName(int program, int uniformIndex, int bufSize) {
        if (this.OpenGL31) {
            val res = GL31.glGetActiveUniformName(program, uniformIndex, bufSize);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_uniform_buffer_object) {
            val res = ARBUniformBufferObject.glGetActiveUniformName(program, uniformIndex, bufSize);
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
            GL32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
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
        if (this.OpenGL32 | this.GL_ARB_draw_elements_base_vertex) {
            this.extraFunctions.glMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_draw_elements_base_vertex));
        }
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        if (this.OpenGL32) {
            val res = GL32.glFenceSync(condition, flags).getPointer();
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sync) {
            val res = ARBSync.glFenceSync(condition, flags).getPointer();
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
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
        if (this.OpenGL32) {
            val res = GL32.glClientWaitSync((GLSync) GLSync_CTOR.invokeExact(sync), flags, timeout);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sync) {
            val res = ARBSync.glClientWaitSync((GLSync) GLSync_CTOR.invokeExact(sync), flags, timeout);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
        }
    }

    @Override
    public int glGetSync(long sync, int pname) {
        if (this.OpenGL32) {
            val res = GL32.glGetSynci((GLSync) GLSync_CTOR.invokeExact(sync), pname);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_sync) {
            val res = ARBSync.glGetSynci((GLSync) GLSync_CTOR.invokeExact(sync), pname);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_sync));
        }
    }

    @Override
    public void glDeleteSync(long sync) {
        if (this.OpenGL32) {
            GL32.glDeleteSync((GLSync) GLSync_CTOR.invokeExact(sync));
            super.debugCheckError();
        } else if (this.GL_ARB_sync) {
            ARBSync.glDeleteSync((GLSync) GLSync_CTOR.invokeExact(sync));
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
            GL33.glVertexAttribDivisor(index, divisor);
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
            val res = GL33.glGenSamplers();
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
            GL33.glDeleteSamplers(sampler);
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
            GL33.glBindSampler(unit, sampler);
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
            GL33.glSamplerParameteri(sampler, pname, param);
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
            GL33.glSamplerParameterf(sampler, pname, param);
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
    public void glProgramUniform1i(int program, int location, int v0) {
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
    public void glProgramUniform2i(int program, int location, int v0, int v1) {
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
    public void glProgramUniform3i(int program, int location, int v0, int v1, int v2) {
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
    public void glProgramUniform4i(int program, int location, int v0, int v1, int v2, int v3) {
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
    public void glProgramUniform1ui(int program, int location, int v0) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1ui(program, location, v0);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1ui(program, location, v0);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform2ui(int program, int location, int v0, int v1) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2ui(program, location, v0, v1);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2ui(program, location, v0, v1);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform3ui(int program, int location, int v0, int v1, int v2) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3ui(program, location, v0, v1, v2);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3ui(program, location, v0, v1, v2);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform4ui(int program, int location, int v0, int v1, int v2, int v3) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4ui(program, location, v0, v1, v2, v3);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4ui(program, location, v0, v1, v2, v3);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform1f(int program, int location, float v0) {
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
    public void glProgramUniform2f(int program, int location, float v0, float v1) {
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
    public void glProgramUniform3f(int program, int location, float v0, float v1, float v2) {
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
    public void glProgramUniform4f(int program, int location, float v0, float v1, float v2, float v3) {
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
    public void glProgramUniform1i(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform2i(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform3i(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform4i(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform1ui(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1u(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1u(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform2ui(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2u(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2u(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform3ui(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3u(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3u(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform4ui(int program, int location, IntBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4u(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4u(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform1f(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform1(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform2f(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform2(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform3f(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform3(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3(program, location, value);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_separate_shader_objects));
        }
    }

    @Override
    public void glProgramUniform4f(int program, int location, FloatBuffer value) {
        if (this.OpenGL41) {
            GL41.glProgramUniform4(program, location, value);
            super.debugCheckError();
        } else if (this.GL_ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4(program, location, value);
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
    public void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance) {
        if (this.OpenGL42) {
            GL42.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
            super.debugCheckError();
        } else if (this.GL_ARB_base_instance) {
            ARBBaseInstance.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_base_instance));
        }
    }

    @Override
    public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices, int instancecount, int basevertex, int baseinstance) {
        if (this.OpenGL42) {
            GL42.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex, baseinstance);
            super.debugCheckError();
        } else if (this.GL_ARB_base_instance) {
            ARBBaseInstance.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex, baseinstance);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_base_instance));
        }
    }

    @Override
    public void glMemoryBarrier(int barriers) {
        if (this.OpenGL42) {
            GL42.glMemoryBarrier(barriers);
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
    public void glInvalidateBufferData(int buffer) {
        if (this.OpenGL43) {
            GL43.glInvalidateBufferData(buffer);
            super.debugCheckError();
        } else if (this.GL_ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateBufferData(buffer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_invalidate_subdata));
        }
    }

    @Override
    public void glInvalidateBufferSubData(int buffer, long offset, long length) {
        if (this.OpenGL43) {
            GL43.glInvalidateBufferSubData(buffer, offset, length);
            super.debugCheckError();
        } else if (this.GL_ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateBufferSubData(buffer, offset, length);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_invalidate_subdata));
        }
    }

    private static final MethodHandle glMultiDrawArraysIndirect;
    private static final MethodHandle nglMultiDrawArraysIndirect;
    private static final MethodHandle nglMultiDrawArraysIndirectBO;

    static {
        Field _glMultiDrawArraysIndirect = ContextCapabilities.class.getDeclaredField("glMultiDrawArraysIndirect");
        _glMultiDrawArraysIndirect.setAccessible(true);
        glMultiDrawArraysIndirect = MethodHandles.publicLookup().unreflectGetter(_glMultiDrawArraysIndirect);

        Method _nglMultiDrawArraysIndirect = GL43.class.getDeclaredMethod("nglMultiDrawArraysIndirect", int.class, long.class, int.class, int.class, long.class);
        _nglMultiDrawArraysIndirect.setAccessible(true);
        nglMultiDrawArraysIndirect = MethodHandles.publicLookup().unreflect(_nglMultiDrawArraysIndirect);

        Method _nglMultiDrawArraysIndirectBO = GL43.class.getDeclaredMethod("nglMultiDrawArraysIndirectBO", int.class, long.class, int.class, int.class, long.class);
        _nglMultiDrawArraysIndirectBO.setAccessible(true);
        nglMultiDrawArraysIndirectBO = MethodHandles.publicLookup().unreflect(_nglMultiDrawArraysIndirectBO);
    }

    @Override
    public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
        if (this.OpenGL43 | this.GL_ARB_multi_draw_indirect) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glMultiDrawArraysIndirect.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);
            
            //switch between the two internal methods depending on whether or not a buffer is bound to GL_DRAW_INDIRECT_BUFFER
            // (this is necessary to avoid internal LWJGL2 throwing an exception if we try to call this with an actual raw memory address)
            if (!LWJGLUtil.CHECKS || (int) StateTracker_getIndirectBuffer.invokeExact(caps) == 0) {
                nglMultiDrawArraysIndirect.invokeExact(mode, indirect, primcount, stride, function_pointer);
                super.debugCheckError();
            } else {
                nglMultiDrawArraysIndirectBO.invokeExact(mode, indirect, primcount, stride, function_pointer);
                super.debugCheckError();
            }
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_multi_draw_indirect));
        }
    }

    private static final MethodHandle glMultiDrawElementsIndirect;
    private static final MethodHandle nglMultiDrawElementsIndirect;
    private static final MethodHandle nglMultiDrawElementsIndirectBO;

    static {
        Field _glMultiDrawElementsIndirect = ContextCapabilities.class.getDeclaredField("glMultiDrawElementsIndirect");
        _glMultiDrawElementsIndirect.setAccessible(true);
        glMultiDrawElementsIndirect = MethodHandles.publicLookup().unreflectGetter(_glMultiDrawElementsIndirect);

        Method _nglMultiDrawElementsIndirect = GL43.class.getDeclaredMethod("nglMultiDrawElementsIndirect", int.class, int.class, long.class, int.class, int.class, long.class);
        _nglMultiDrawElementsIndirect.setAccessible(true);
        nglMultiDrawElementsIndirect = MethodHandles.publicLookup().unreflect(_nglMultiDrawElementsIndirect);

        Method _nglMultiDrawElementsIndirectBO = GL43.class.getDeclaredMethod("nglMultiDrawElementsIndirectBO", int.class, int.class, long.class, int.class, int.class, long.class);
        _nglMultiDrawElementsIndirectBO.setAccessible(true);
        nglMultiDrawElementsIndirectBO = MethodHandles.publicLookup().unreflect(_nglMultiDrawElementsIndirectBO);
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride) {
        if (this.OpenGL43 | this.GL_ARB_multi_draw_indirect) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glMultiDrawElementsIndirect.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);

            //switch between the two internal methods depending on whether or not a buffer is bound to GL_DRAW_INDIRECT_BUFFER
            // (this is necessary to avoid internal LWJGL2 throwing an exception if we try to call this with an actual raw memory address)
            if ((int) StateTracker_getIndirectBuffer.invokeExact(caps) == 0) {
                nglMultiDrawElementsIndirect.invokeExact(mode, type, indirect, primcount, stride, function_pointer);
                super.debugCheckError();
            } else {
                nglMultiDrawElementsIndirectBO.invokeExact(mode, type, indirect, primcount, stride, function_pointer);
                super.debugCheckError();
            }
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_multi_draw_indirect));
        }
    }

    @Override
    public int glGetProgramInterfacei(int program, int programInterface, int pname) {
        if (this.OpenGL43) {
            val res = GL43.glGetProgramInterfacei(program, programInterface, pname);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_program_interface_query) {
            val res = ARBProgramInterfaceQuery.glGetProgramInterfacei(program, programInterface, pname);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_program_interface_query));
        }
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        if (this.OpenGL43) {
            val res = GL43.glGetProgramResourceIndex(program, programInterface, name);
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
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull IntBuffer props, IntBuffer length, @NonNull IntBuffer params) {
        if (this.OpenGL43) {
            GL43.glGetProgramResource(program, programInterface, index, props, length, params);
            super.debugCheckError();
        } else if (this.GL_ARB_program_interface_query) {
            ARBProgramInterfaceQuery.glGetProgramResource(program, programInterface, index, props, length, params);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_program_interface_query));
        }
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull int[] props, int[] length, @NonNull int[] params) {
        long size = (long) (props.length + params.length + (length != null ? length.length : 0)) * Integer.BYTES;
        long address = DirectBufferHackery.allocateTemporary(size);
        try {
            //prepare IntBuffers to wrap the temporary allocation
            IntBuffer propsBuffer = DirectBufferHackery.wrapInt(address, props.length);
            IntBuffer paramsBuffer = DirectBufferHackery.wrapInt(address + (long) props.length * Integer.BYTES, params.length);
            IntBuffer lengthBuffer = length != null
                    ? DirectBufferHackery.wrapInt(address + (long) (props.length + params.length) * Integer.BYTES, length.length)
                    : null;

            //copy the props array off-heap
            propsBuffer.put(props).clear();

            this.glGetProgramResourceiv(program, programInterface, index, propsBuffer, lengthBuffer, paramsBuffer);

            //copy the results back to the heap arrays
            paramsBuffer.get(params);
            if (length != null) {
                lengthBuffer.get(length);
            }
        } finally {
            DirectBufferHackery.freeTemporary(address, size);
        }
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index, int bufSize) {
        if (this.OpenGL43) {
            val res = GL43.glGetProgramResourceName(program, programInterface, index, bufSize);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_program_interface_query) {
            val res = ARBProgramInterfaceQuery.glGetProgramResourceName(program, programInterface, index, bufSize);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_program_interface_query));
        }
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        if (this.OpenGL43) {
            GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
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
            GL43.glObjectLabel(identifier, name, label);
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
            GL43.glObjectPtrLabel((GLSync) GLSync_CTOR.invokeExact(ptr), label);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glObjectPtrLabel((GLSync) GLSync_CTOR.invokeExact(ptr), label);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public String glGetObjectLabel(int identifier, int name) {
        int maxLabelLength = this.limits().maxLabelLength();
        if (this.OpenGL43) {
            val res = GL43.glGetObjectLabel(identifier, name, maxLabelLength);
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
            val res = GL43.glGetObjectPtrLabel((GLSync) GLSync_CTOR.invokeExact(ptr), maxLabelLength);
            super.debugCheckError();
            return res;
        } else if (this.GL_KHR_debug) {
            val res = KHRDebug.glGetObjectPtrLabel((GLSync) GLSync_CTOR.invokeExact(ptr), maxLabelLength);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        if (this.OpenGL43) {
            GL43.glDebugMessageControl(source, type, severity, ids, enabled);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glDebugMessageControl(source, type, severity, ids, enabled);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, int[] ids, boolean enabled) {
        if (ids == null) {
            this.glDebugMessageControl(source, type, severity, (IntBuffer) null, enabled);
        } else {
            long address = PUnsafe.allocateMemory(ids.length * (long) Long.BYTES);
            try {
                IntBuffer wrapped = DirectBufferHackery.wrapInt(address, ids.length);
                wrapped.put(ids).clear();
                this.glDebugMessageControl(source, type, severity, wrapped, enabled);
            } finally {
                PUnsafe.freeMemory(address);
            }
        }
    }

    @Override
    public void glDebugMessageInsert(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        if (this.OpenGL43) {
            GL43.glDebugMessageInsert(source, type, id, severity, msg);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glDebugMessageInsert(source, type, id, severity, msg);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public void glDebugMessageCallback(GLDebugOutputCallback callback) {
        val implCallback = callback != null
                ? new org.lwjgl.opengl.KHRDebugCallback(callback::handleMessage)
                : null;

        if (this.OpenGL43) {
            GL43.glDebugMessageCallback(implCallback);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glDebugMessageCallback(implCallback);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public void glPushDebugGroup(int source, int id, @NonNull CharSequence msg) {
        if (this.OpenGL43) {
            GL43.glPushDebugGroup(source, id, msg);
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glPushDebugGroup(source, id, msg);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
        }
    }

    @Override
    public void glPopDebugGroup() {
        if (this.OpenGL43) {
            GL43.glPopDebugGroup();
            super.debugCheckError();
        } else if (this.GL_KHR_debug) {
            KHRDebug.glPopDebugGroup();
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_KHR_debug));
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
        if (this.OpenGL44 | this.GL_ARB_buffer_storage) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glBufferStorage.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);
            nglBufferStorage.invokeExact(target, data_size, data, flags, function_pointer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_buffer_storage));
        }
    }

    @Override
    public void glBufferStorage(int target, @NonNull ByteBuffer data, int flags) {
        if (this.OpenGL44) {
            GL44.glBufferStorage(target, data, flags);
            super.debugCheckError();
        } else if (this.GL_ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_buffer_storage));
        }
    }

    @Override
    public void glBindBuffersBase(int target, int first, int count) {
        if (this.OpenGL44) {
            GL44.glBindBuffersBase(target, first, count, null);
            super.debugCheckError();
        } else if (this.GL_ARB_multi_bind) {
            ARBMultiBind.glBindBuffersBase(target, first, count, null);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_multi_bind));
        }
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull IntBuffer buffers) {
        if (this.OpenGL44) {
            GL44.glBindBuffersBase(target, first, buffers.remaining(), buffers);
            super.debugCheckError();
        } else if (this.GL_ARB_multi_bind) {
            ARBMultiBind.glBindBuffersBase(target, first, buffers.remaining(), buffers);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_multi_bind));
        }
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull int[] buffers) {
        long size = buffers.length * (long) Integer.BYTES;
        long address = DirectBufferHackery.allocateTemporary(size);
        try {
            DirectBufferHackery.wrapInt(address, buffers.length).put(buffers);
            this.glBindBuffersBase(target, first, DirectBufferHackery.wrapInt(address, buffers.length));
        } finally {
            DirectBufferHackery.freeTemporary(address, size);
        }
    }

    //
    //
    // OpenGL 4.5
    //
    //

    @Override
    public void glClipControl(int origin, int depth) {
        if (this.OpenGL45) {
            GL45.glClipControl(origin, depth);
            super.debugCheckError();
        } else if (this.GL_ARB_clip_control) {
            ARBClipControl.glClipControl(origin, depth);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_clip_control));
        }
    }

    @Override
    public int glCreateBuffer() {
        if (this.OpenGL45) {
            val res = GL45.glCreateBuffers();
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
        if (this.OpenGL45 | this.GL_ARB_direct_state_access) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glNamedBufferData.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);
            nglNamedBufferData.invokeExact(buffer, data_size, data, usage, function_pointer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage) {
        if (this.OpenGL45) {
            GL45.glNamedBufferData(buffer, data, usage);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
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
        if (this.OpenGL45 | this.GL_ARB_direct_state_access) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glNamedBufferStorage.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);
            nglNamedBufferStorage.invokeExact(buffer, data_size, data, flags, function_pointer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags) {
        if (this.OpenGL45) {
            GL45.glNamedBufferStorage(buffer, data, flags);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, data, flags);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
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
        if (this.OpenGL45 | this.GL_ARB_direct_state_access) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glNamedBufferSubData.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);
            nglNamedBufferSubData.invokeExact(buffer, offset, data_size, data, function_pointer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        if (this.OpenGL45) {
            GL45.glNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
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
        if (this.OpenGL45 | this.GL_ARB_direct_state_access) {
            ContextCapabilities caps = GLContext.getCapabilities();
            long function_pointer = (long) glGetNamedBufferSubData.invokeExact(caps);
            BufferChecks.checkFunctionAddress(function_pointer);
            nglGetNamedBufferSubData.invokeExact(buffer, offset, data_size, data, function_pointer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        if (this.OpenGL45) {
            GL45.glGetNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glGetNamedBufferSubData(buffer, offset, data);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public int glGetNamedBufferParameteri(int buffer, int pname) {
        if (this.OpenGL45) {
            val res = GL45.glGetNamedBufferParameteri(buffer, pname);
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glGetNamedBufferParameteri(buffer, pname);
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public long glMapNamedBuffer(int buffer, int access) {
        if (this.OpenGL45) {
            val res = PUnsafe.pork_directBufferAddress(GL45.glMapNamedBuffer(buffer, access, null));
            super.debugCheckError();
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = PUnsafe.pork_directBufferAddress(ARBDirectStateAccess.glMapNamedBuffer(buffer, access, null));
            super.debugCheckError();
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer) {
        if (this.OpenGL45) {
            val res = GL45.glMapNamedBuffer(buffer, access, length, oldBuffer);
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
            val res = PUnsafe.pork_directBufferAddress(GL45.glMapNamedBufferRange(buffer, offset, size, access, null));
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
            val res = GL45.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
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
            GL45.glFlushMappedNamedBufferRange(buffer, offset, length);
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
            val res = GL45.glUnmapNamedBuffer(buffer);
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
            GL45.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
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
            val res = GL45.glCreateVertexArrays();
            super.debugCheckError();
            if (LWJGLUtil.CHECKS) { // Prevents the workaround in glVertexArrayElementBuffer from throwing an NPE
                StateTracker_createVAO.invokeExact(GLContext.getCapabilities(), res);
            }
            return res;
        } else if (this.GL_ARB_direct_state_access) {
            val res = ARBDirectStateAccess.glCreateVertexArrays();
            super.debugCheckError();
            if (LWJGLUtil.CHECKS) { // Prevents the workaround in glVertexArrayElementBuffer from throwing an NPE
                StateTracker_createVAO.invokeExact(GLContext.getCapabilities(), res);
            }
            return res;
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    @Override
    public void glVertexArrayElementBuffer(int vaobj, int buffer) {
        if (this.OpenGL45) {
            GL45.glVertexArrayElementBuffer(vaobj, buffer);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayElementBuffer(vaobj, buffer);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }

        if (LWJGLUtil.CHECKS) {
            //LWJGL2 runtime checks will prevent us from using glDrawElements* with a buffer offset if the
            //  bound VAO doesn't have an element array buffer attached, but doesn't track element arrays
            //  attached to a VAO by this function. We'll do it ourself!
            StateTracker_setVAOElementArrayBuffer.invokeExact(GLContext.getCapabilities(), vaobj, buffer);
        }
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        if (this.OpenGL45) {
            GL45.glEnableVertexArrayAttrib(vaobj, index);
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
            GL45.glDisableVertexArrayAttrib(vaobj, index);
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
            GL45.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
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
            GL45.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
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
            GL45.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
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
            GL45.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
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
            GL45.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
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

        if (this.OpenGL45) {
            GL45.glVertexArrayVertexBuffers(vaobj, first, count, wrappedBuffers, wrappedOffsets, wrappedStrides);
            super.debugCheckError();
        } else if (this.GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayVertexBuffers(vaobj, first, count, wrappedBuffers, wrappedOffsets, wrappedStrides);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access));
        }
    }

    //
    //
    // OpenGL 4.6
    //
    //

    @Override
    public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawcount, int maxdrawcount, int stride) {
        /*if (this.OpenGL46) {
            GL46.glMultiDrawArraysIndirectCount(mode, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else*/ if (this.GL_ARB_indirect_parameters) {
            ARBIndirectParameters.glMultiDrawArraysIndirectCountARB(mode, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_indirect_parameters));
        }
    }

    @Override
    public void glMultiDrawElementsIndirectCount(int mode, int type, long indirect, long drawcount, int maxdrawcount, int stride) {
        /*if (this.OpenGL46) {
            GL46.glMultiDrawElementsIndirectCount(mode, type, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else*/ if (this.GL_ARB_indirect_parameters) {
            ARBIndirectParameters.glMultiDrawElementsIndirectCountARB(mode, type, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_indirect_parameters));
        }
    }

    //
    //
    // No OpenGL version
    //
    //

    @Override
    public void glDebugMessageControlARB(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        if (this.GL_ARB_debug_output) {
            ARBDebugOutput.glDebugMessageControlARB(source, type, severity, ids, enabled);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_debug_output));
        }
    }

    @Override
    public void glDebugMessageControlARB(int source, int type, int severity, int[] ids, boolean enabled) {
        if (ids == null) {
            this.glDebugMessageControlARB(source, type, severity, (IntBuffer) null, enabled);
        } else {
            long address = PUnsafe.allocateMemory(ids.length * (long) Long.BYTES);
            try {
                IntBuffer wrapped = DirectBufferHackery.wrapInt(address, ids.length);
                wrapped.put(ids).clear();
                this.glDebugMessageControlARB(source, type, severity, wrapped, enabled);
            } finally {
                PUnsafe.freeMemory(address);
            }
        }
    }

    @Override
    public void glDebugMessageInsertARB(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        if (this.GL_ARB_debug_output) {
            ARBDebugOutput.glDebugMessageInsertARB(source, type, id, severity, msg);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_debug_output));
        }
    }

    @Override
    public void glDebugMessageCallbackARB(GLDebugOutputCallback callback) {
        if (this.GL_ARB_debug_output) {
            ARBDebugOutput.glDebugMessageCallbackARB(callback != null
                    ? new ARBDebugOutputCallback(callback::handleMessage)
                    : null);
            super.debugCheckError();
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_debug_output));
        }
    }

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
            //LWJGL2 doesn't expose glNamedBufferPageCommitmentARB, so we're forced to emulate it by binding the buffer to an arbitrary binding point, calling
            // the non-DSA function and then restoring the original binding.
            //we could do a hacky thing to get the function pointer manually and then pass it to ARBSparseBuffer.nglBufferPageCommitmentARB (which conveniently
            // has the same signature as glNamedBufferPageCommitmentARB), but it's not clear to me if it's safe to acquire the function pointer without synchronizing
            // on GLContext.class and doing some internal context setting stuff which just really isn't worth the added effort.
            int old = this.glGetInteger(GL_ARRAY_BUFFER_BINDING);
            try {
                this.glBindBuffer(GL_ARRAY_BUFFER, buffer);
                this.glBufferPageCommitmentARB(GL_ARRAY_BUFFER, offset, size, commit);
            } finally {
                this.glBindBuffer(GL_ARRAY_BUFFER, old);
            }
        } else {
            throw new UnsupportedOperationException(super.unsupportedMsg(GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_sparse_buffer));
        }
    }
}
