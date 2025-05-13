/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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
import net.daporkchop.fp2.common.util.NIOBufferUtil;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.opengl.lwjgl2.extra.ExtraFunctions;
import net.daporkchop.fp2.gl.opengl.lwjgl2.extra.ExtraFunctionsProvider;
import net.daporkchop.fp2.gl.util.AnyMemoryRegion;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputCallback;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.function.throwing.TPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.BufferChecks;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.MemoryUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBIndirectParameters;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBSparseBuffer;
import org.lwjgl.opengl.ARBTextureBufferObject;
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
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Comparator;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class GLAPILWJGL2 extends OpenGL {
    public static OpenGL forCurrent() {
        return new GLAPILWJGL2();
    }

    private final ExtraFunctions extraFunctions;

    //funny workarounds
    private final long glMultiDrawElements;
    private final long glMaxShaderCompilerThreadsARB;
    private final long glMaxShaderCompilerThreadsKHR;

    public GLAPILWJGL2() {
        checkState(!this.OpenGL46, "LWJGL2 doesn't support OpenGL 4.6!");

        this.extraFunctions = ExtraFunctionsProvider.INSTANCE.get();

        //funny workarounds
        this.glMultiDrawElements = getFunctionAddress("glMultiDrawElements");
        this.glMaxShaderCompilerThreadsARB = this.GL_ARB_parallel_shader_compile ? getFunctionAddress("glMaxShaderCompilerThreadsARB") : 0L;
        this.glMaxShaderCompilerThreadsKHR = this.GL_KHR_parallel_shader_compile ? getFunctionAddress("glMaxShaderCompilerThreadsKHR") : 0L;
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

    private static final MethodHandle APIUtil_getBufferByte; // (ContextCapabilities, int) -> ByteBuffer
    private static final MethodHandle APIUtil_getBufferInt; // (ContextCapabilities) -> IntBuffer
    private static final MethodHandle APIUtil_getBufferLong; // (ContextCapabilities) -> LongBuffer
    private static final MethodHandle APIUtil_getBufferFloat; // (ContextCapabilities) -> FloatBuffer
    private static final MethodHandle APIUtil_getBufferDouble; // (ContextCapabilities) -> DoubleBuffer

    static {
        Class<?> _APIUtil = Class.forName("org.lwjgl.opengl.APIUtil");

        Method _APIUtil_getBufferByte = _APIUtil.getDeclaredMethod("getBufferByte", ContextCapabilities.class, int.class);
        _APIUtil_getBufferByte.setAccessible(true);
        APIUtil_getBufferByte = MethodHandles.publicLookup().unreflect(_APIUtil_getBufferByte);

        Method _APIUtil_getBufferInt = _APIUtil.getDeclaredMethod("getBufferInt", ContextCapabilities.class);
        _APIUtil_getBufferInt.setAccessible(true);
        APIUtil_getBufferInt = MethodHandles.publicLookup().unreflect(_APIUtil_getBufferInt);

        Method _APIUtil_getBufferLong = _APIUtil.getDeclaredMethod("getBufferLong", ContextCapabilities.class);
        _APIUtil_getBufferLong.setAccessible(true);
        APIUtil_getBufferLong = MethodHandles.publicLookup().unreflect(_APIUtil_getBufferLong);

        Method _APIUtil_getBufferFloat = _APIUtil.getDeclaredMethod("getBufferFloat", ContextCapabilities.class);
        _APIUtil_getBufferFloat.setAccessible(true);
        APIUtil_getBufferFloat = MethodHandles.publicLookup().unreflect(_APIUtil_getBufferFloat);

        Method _APIUtil_getBufferDouble = _APIUtil.getDeclaredMethod("getBufferDouble", ContextCapabilities.class);
        _APIUtil_getBufferDouble.setAccessible(true);
        APIUtil_getBufferDouble = MethodHandles.publicLookup().unreflect(_APIUtil_getBufferDouble);
    }

    private ByteBuffer getBufferByte(@NotNegative int capacity) {
        return (ByteBuffer) APIUtil_getBufferByte.invokeExact(GLContext.getCapabilities(), capacity);
    }

    private ByteBuffer getBufferByteDuplicateExactly(@NotNegative int capacity) {
        return (ByteBuffer) this.getBufferByte(capacity).duplicate().position(0).limit(capacity);
    }

    private IntBuffer getBufferInt(@NotNegative int capacity) {
        IntBuffer buffer = (IntBuffer) APIUtil_getBufferInt.invokeExact(GLContext.getCapabilities());
        checkArg(capacity <= buffer.capacity(), "capacity (%s) <= %s", capacity, buffer.capacity());
        return buffer;
    }

    private LongBuffer getBufferLong(@NotNegative int capacity) {
        LongBuffer buffer = (LongBuffer) APIUtil_getBufferLong.invokeExact(GLContext.getCapabilities());
        checkArg(capacity <= buffer.capacity(), "capacity (%s) <= %s", capacity, buffer.capacity());
        return buffer;
    }

    private FloatBuffer getBufferFloat(@NotNegative int capacity) {
        FloatBuffer buffer = (FloatBuffer) APIUtil_getBufferFloat.invokeExact(GLContext.getCapabilities());
        checkArg(capacity <= buffer.capacity(), "capacity (%s) <= %s", capacity, buffer.capacity());
        return buffer;
    }

    private DoubleBuffer getBufferDouble(@NotNegative int capacity) {
        DoubleBuffer buffer = (DoubleBuffer) APIUtil_getBufferDouble.invokeExact(GLContext.getCapabilities());
        checkArg(capacity <= buffer.capacity(), "capacity (%s) <= %s", capacity, buffer.capacity());
        return buffer;
    }

    //
    // getBufferForUpload
    // (upload using typed buffer)
    //

    private ByteBuffer getBufferForUpload(byte[] data) {
        ByteBuffer dataBuffer = this.getBufferByte(data.length).duplicate();
        dataBuffer.put(data).flip();
        return dataBuffer;
    }

    private ShortBuffer getBufferForUpload(short[] data) {
        ShortBuffer dataBuffer = this.getBufferByte(data.length * Short.BYTES).asShortBuffer();
        dataBuffer.put(data).flip();
        return dataBuffer;
    }

    private IntBuffer getBufferForUpload(int[] data) {
        IntBuffer dataBuffer = this.getBufferByte(data.length * Integer.BYTES).asIntBuffer();
        dataBuffer.put(data).flip();
        return dataBuffer;
    }

    private ByteBuffer getBufferForUpload(long[] data) {
        ByteBuffer dataBuffer = this.getBufferByteDuplicateExactly(data.length * Long.BYTES);
        dataBuffer.asLongBuffer().put(data);
        return dataBuffer;
    }

    private FloatBuffer getBufferForUpload(float[] data) {
        FloatBuffer dataBuffer = this.getBufferByte(data.length * Float.BYTES).asFloatBuffer();
        dataBuffer.put(data).flip();
        return dataBuffer;
    }

    private DoubleBuffer getBufferForUpload(double[] data) {
        DoubleBuffer dataBuffer = this.getBufferByte(data.length * Double.BYTES).asDoubleBuffer();
        dataBuffer.put(data).flip();
        return dataBuffer;
    }

    //
    // begin/finishHeapDownload
    // (download using typed buffer)
    //

    private ByteBuffer beginHeapDownload(byte[] data) {
        return this.getBufferByteDuplicateExactly(data.length);
    }

    private void finishHeapDownload(byte[] data, ByteBuffer dataBuffer) {
        dataBuffer.get(data);
    }

    private ShortBuffer beginHeapDownload(short[] data) {
        return (ShortBuffer) this.getBufferByte(data.length * Short.BYTES).asShortBuffer().position(0).limit(data.length);
    }

    private void finishHeapDownload(short[] data, ShortBuffer dataBuffer) {
        dataBuffer.get(data);
    }

    private IntBuffer beginHeapDownload(int[] data) {
        return (IntBuffer) this.getBufferByte(data.length * Integer.BYTES).asIntBuffer().position(0).limit(data.length);
    }

    private void finishHeapDownload(int[] data, IntBuffer dataBuffer) {
        dataBuffer.get(data);
    }

    private ByteBuffer beginHeapDownload(long[] data) {
        return this.getBufferByteDuplicateExactly(data.length * Long.BYTES);
    }

    private void finishHeapDownload(long[] data, ByteBuffer dataBuffer) {
        dataBuffer.asLongBuffer().get(data);
    }

    private FloatBuffer beginHeapDownload(float[] data) {
        return (FloatBuffer) this.getBufferByte(data.length * Float.BYTES).asFloatBuffer().position(0).limit(data.length);
    }

    private void finishHeapDownload(float[] data, FloatBuffer dataBuffer) {
        dataBuffer.get(data);
    }

    private DoubleBuffer beginHeapDownload(double[] data) {
        return (DoubleBuffer) this.getBufferByte(data.length * Double.BYTES).asDoubleBuffer().position(0).limit(data.length);
    }

    private void finishHeapDownload(double[] data, DoubleBuffer dataBuffer) {
        dataBuffer.get(data);
    }

    private static final MethodHandle StateTracker_getIndirectBuffer;
    private static final MethodHandle StateTracker_createVAO; // (ContextCapabilities, int) -> void
    private static final MethodHandle StateTracker_setVAOElementArrayBuffer; // (ContextCapabilities, int, int) -> void

    static {
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
    public void glGetBoolean(int pname, boolean @NonNull [] data) {
        //we can safely use APIUtil.getBufferByte() here: GL11.glGetBoolean() doesn't use it
        ByteBuffer dataBuffer = this.getBufferByte(Math.max(data.length, 16)).duplicate();

        GL11.glGetBoolean(pname, dataBuffer);
        super.debugCheckError();

        //copy result values into destination array
        for (int i = 0; i < data.length; i++) {
            data[i] = dataBuffer.get() != 0;
        }
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
    public void glGetInteger(int pname, int @NonNull [] data) {
        //we can safely use APIUtil.getBufferInt() here: GL11.glGetInteger() doesn't use it
        IntBuffer dataBuffer = this.getBufferInt(Math.max(data.length, 16)).duplicate();

        GL11.glGetInteger(pname, dataBuffer);
        super.debugCheckError();

        dataBuffer.get(data);
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
    public void glGetFloat(int pname, float @NonNull [] data) {
        //we can safely use APIUtil.getBufferFloat() here: GL11.glGetFloat() doesn't use it
        FloatBuffer dataBuffer = this.getBufferFloat(Math.max(data.length, 16)).duplicate();

        GL11.glGetFloat(pname, dataBuffer);
        super.debugCheckError();

        dataBuffer.get(data);
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
    public void glGetDouble(int pname, double @NonNull [] data) {
        //we can safely use APIUtil.getBufferDouble() here: GL11.glGetDouble() doesn't use it
        DoubleBuffer dataBuffer = this.getBufferDouble(Math.max(data.length, 16)).duplicate();

        GL11.glGetDouble(pname, dataBuffer);
        super.debugCheckError();

        dataBuffer.get(data);
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
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glPushClientAttrib(int mask) {
        super.checkSupported(this.GL_ARB_compatibility, GLExtension.GL_ARB_compatibility);

        GL11.glPushClientAttrib(mask);
        super.debugCheckError();
    }

    @Override
    public void glPopClientAttrib() {
        super.checkSupported(this.GL_ARB_compatibility, GLExtension.GL_ARB_compatibility);

        GL11.glPopClientAttrib();
        super.debugCheckError();
    }

    @Override
    public void glPushAttrib(int mask) {
        super.checkSupported(this.GL_ARB_compatibility, GLExtension.GL_ARB_compatibility);

        GL11.glPushAttrib(mask);
        super.debugCheckError();
    }

    @Override
    public void glPopAttrib() {
        super.checkSupported(this.GL_ARB_compatibility, GLExtension.GL_ARB_compatibility);

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

    @Override
    public void glBufferData(int target, @NonNull AnyMemoryRegion data, int usage) {
        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glBufferData(target, (ByteBuffer) memory, usage);
        } else if (memory instanceof short[]) {
            GL15.glBufferData(target, this.getBufferForUpload((short[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL15.glBufferData(target, this.getBufferForUpload((int[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL15.glBufferData(target, this.getBufferForUpload((long[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL15.glBufferData(target, this.getBufferForUpload((float[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL15.glBufferData(target, this.getBufferForUpload((double[]) memory), usage);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
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

    @Override
    public void glBufferSubData(int target, long offset, @NonNull AnyMemoryRegion data) {
        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glBufferSubData(target, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            GL15.glBufferSubData(target, offset, this.getBufferForUpload((short[]) memory));
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL15.glBufferSubData(target, offset, this.getBufferForUpload((int[]) memory));
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL15.glBufferSubData(target, offset, this.getBufferForUpload((long[]) memory));
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL15.glBufferSubData(target, offset, this.getBufferForUpload((float[]) memory));
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL15.glBufferSubData(target, offset, this.getBufferForUpload((double[]) memory));
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
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
    public void glGetBufferSubData(int target, long offset, @NonNull AnyMemoryRegion data) {
        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glGetBufferSubData(target, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            val dataArray = (short[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL15.glGetBufferSubData(target, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof int[]) {
            val dataArray = (int[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL15.glGetBufferSubData(target, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof long[]) {
            val dataArray = (long[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL15.glGetBufferSubData(target, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof float[]) {
            val dataArray = (float[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL15.glGetBufferSubData(target, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof double[]) {
            val dataArray = (double[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL15.glGetBufferSubData(target, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
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
        IntBuffer buffer = this.getBufferInt(count);
        GL20.glGetProgram(program, pname, buffer);
        super.debugCheckError();
        return NIOBufferUtil.toArrayCount(buffer.duplicate(), count);
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
        //get temporary direct buffer for storing the results
        //we can safely use APIUtil.getBufferInt() here: GL20.glGetActiveAttrib() uses getBufferByte() and getLengths(), but getBufferInt() is untouched
        IntBuffer sizeType = this.getBufferInt(2);
        val res = GL20.glGetActiveAttrib(program, index, bufSize, sizeType);
        super.debugCheckError();

        //copy size and type into the destination buffers
        size.put(size.position(), sizeType.get(0));
        type.put(type.position(), sizeType.get(1));
        return res;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull int[] size, @NonNull int[] type) {
        //get temporary direct buffer for storing the results
        //we can safely use APIUtil.getBufferInt() here: GL20.glGetActiveAttrib() uses getBufferByte() and getLengths(), but getBufferInt() is untouched
        IntBuffer sizeType = this.getBufferInt(2);
        val res = GL20.glGetActiveAttrib(program, index, bufSize, sizeType);
        super.debugCheckError();

        //copy size and type into the destination arrays
        size[0] = sizeType.get(0);
        type[0] = sizeType.get(1);
        return res;
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
    public boolean glGetBoolean(int pname, int idx) {
        val res = GL30.glGetBoolean(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetBoolean(int pname, int idx, @NonNull ByteBuffer data) {
        GL30.glGetBoolean(pname, idx, DirectBufferHackery.wrapByte(DirectBufferHackery.address(data), 4)); //LWJGL2 will throw a fit if the buffer doesn't have at least 4 elements
        super.debugCheckError();
    }

    @Override
    public void glGetBoolean(int pname, int idx, boolean @NonNull [] data) {
        //we can safely use APIUtil.getBufferByte() here: GL30.glGetBoolean() doesn't use it
        ByteBuffer dataBuffer = this.getBufferByte(Math.max(data.length, 4)).duplicate();

        GL30.glGetBoolean(pname, idx, dataBuffer);
        super.debugCheckError();

        //copy result values into destination array
        for (int i = 0; i < data.length; i++) {
            data[i] = dataBuffer.get() != 0;
        }
    }

    @Override
    public int glGetInteger(int pname, int idx) {
        val res = GL30.glGetInteger(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetInteger(int pname, int idx, @NonNull IntBuffer data) {
        GL30.glGetInteger(pname, idx, DirectBufferHackery.wrapInt(DirectBufferHackery.address(data), 4)); //LWJGL2 will throw a fit if the buffer doesn't have at least 4 elements
        super.debugCheckError();
    }

    @Override
    public void glGetInteger(int pname, int idx, int @NonNull [] data) {
        //we can safely use APIUtil.getBufferInt() here: GL30.glGetInteger() doesn't use it
        IntBuffer dataBuffer = this.getBufferInt(Math.max(data.length, 4)).duplicate();

        GL30.glGetInteger(pname, idx, dataBuffer);
        super.debugCheckError();

        dataBuffer.get(data);

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

    @Override
    public void glGenerateMipmap(int target) {
        GL30.glGenerateMipmap(target);
        super.debugCheckError();
    }

    @Override
    public int glGenRenderbuffer() {
        val res = GL30.glGenRenderbuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {
        GL30.glDeleteRenderbuffers(renderbuffer);
        super.debugCheckError();
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GL30.glBindRenderbuffer(target, renderbuffer);
        super.debugCheckError();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GL30.glRenderbufferStorage(target, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public int glGenFramebuffer() {
        val res = GL30.glGenFramebuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        GL30.glDeleteFramebuffers(framebuffer);
        super.debugCheckError();
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GL30.glBindFramebuffer(target, framebuffer);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferTexture1D(int target, int attachment, int textarget, int texture, int level) {
        GL30.glFramebufferTexture1D(target, attachment, textarget, texture, level);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferTexture3D(int target, int attachment, int textarget, int texture, int level, int layer) {
        GL30.glFramebufferTexture3D(target, attachment, textarget, texture, level, layer);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        super.debugCheckError();
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        val res = GL30.glCheckFramebufferStatus(target);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 3.1
    //
    //

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        super.checkSupported(this.GL_ARB_copy_buffer, GLExtension.GL_ARB_copy_buffer);

        GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        super.debugCheckError();
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        super.checkSupported(this.GL_ARB_draw_instanced, GLExtension.GL_ARB_draw_instanced);

        if (this.OpenGL31) { //use the core function if possible
            GL31.glDrawArraysInstanced(mode, first, count, instancecount);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, instancecount);
            super.debugCheckError();
        }
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
        super.checkSupported(this.GL_ARB_draw_instanced, GLExtension.GL_ARB_draw_instanced);

        if (this.OpenGL31) { //use the core function if possible
            GL31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, count, type, indices, instancecount);
            super.debugCheckError();
        }
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        super.checkSupported(this.GL_ARB_texture_buffer_object, GLExtension.GL_ARB_texture_buffer_object);

        if (this.OpenGL31) { //use the core function if possible
            GL31.glTexBuffer(target, internalFormat, buffer);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBTextureBufferObject.glTexBufferARB(target, internalFormat, buffer);
            super.debugCheckError();
        }
    }

    @Override
    public int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31.glGetUniformBlockIndex(program, uniformBlockName);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetActiveUniformBlocki(int program, int uniformBlockIndex, int pname) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31.glGetActiveUniformBlocki(program, uniformBlockIndex, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex, int bufSize) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31.glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        super.debugCheckError();
    }

    @Override
    public int[] glGetUniformIndices(int program, CharSequence[] uniformNames) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        //get temporary direct buffer for storing the results
        //we can safely use APIUtil.getBufferInt() here: GL31.glGetUniformIndices() only uses getBufferByte()
        IntBuffer tmpBuffer = this.getBufferInt(uniformNames.length);
        GL31.glGetUniformIndices(program, uniformNames, tmpBuffer);
        super.debugCheckError();

        return NIOBufferUtil.toArrayCount(tmpBuffer.duplicate(), uniformNames.length);
    }

    @Override
    public int glGetActiveUniformsi(int program, int uniformIndex, int pname) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31.glGetActiveUniformsi(program, uniformIndex, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveUniformName(int program, int uniformIndex, int bufSize) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31.glGetActiveUniformName(program, uniformIndex, bufSize);
        super.debugCheckError();
        return res;
    }

    //
    //
    // OpenGL 3.2
    //
    //

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        super.checkSupported(this.GL_ARB_draw_elements_base_vertex, GLExtension.GL_ARB_draw_elements_base_vertex);

        GL32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        super.debugCheckError();
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        super.checkSupported(this.GL_ARB_draw_elements_base_vertex, GLExtension.GL_ARB_draw_elements_base_vertex);

        this.extraFunctions.glMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
        super.debugCheckError();
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        val res = GL32.glFenceSync(condition, flags).getPointer();
        super.debugCheckError();
        return res;
    }

    private static final MethodHandle GLSync_CTOR;

    static {
        Constructor<GLSync> ctor = GLSync.class.getDeclaredConstructor(long.class);
        ctor.setAccessible(true);
        GLSync_CTOR = MethodHandles.publicLookup().unreflectConstructor(ctor);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        val res = GL32.glClientWaitSync((GLSync) GLSync_CTOR.invokeExact(sync), flags, timeout);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetSync(long sync, int pname) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        val res = GL32.glGetSynci((GLSync) GLSync_CTOR.invokeExact(sync), pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteSync(long sync) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        GL32.glDeleteSync((GLSync) GLSync_CTOR.invokeExact(sync));
        super.debugCheckError();
    }

    //
    //
    // OpenGL 3.3
    //
    //

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        super.checkSupported(this.GL_ARB_instanced_arrays, GLExtension.GL_ARB_instanced_arrays);

        if (this.OpenGL33) { //use the core function if possible
            GL33.glVertexAttribDivisor(index, divisor);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
            super.debugCheckError();
        }
    }

    @Override
    public int glGenSampler() {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        val res = GL33.glGenSamplers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteSampler(int sampler) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33.glDeleteSamplers(sampler);
        super.debugCheckError();
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33.glBindSampler(unit, sampler);
        super.debugCheckError();
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, int param) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33.glSamplerParameteri(sampler, pname, param);
        super.debugCheckError();
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, float param) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33.glSamplerParameterf(sampler, pname, param);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 4.1
    //
    //

    @Override
    public void glGetProgramBinary(int program, int[] length, int @NonNull [] binaryFormat, @NonNull ByteBuffer binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        //get temporary direct buffer for storing the return values
        //we can safely use APIUtil.getBufferInt() here: GL41.glGetProgramBinary() doesn't use it
        IntBuffer lengthFormatBuffer = this.getBufferInt(2);
        IntBuffer lengthBuffer = NIOBufferUtil.duplicateRange(lengthFormatBuffer, 0, 1);
        IntBuffer binaryFormatBuffer = NIOBufferUtil.duplicateRange(lengthFormatBuffer, 1, 1);

        GL41.glGetProgramBinary(program, lengthBuffer, binaryFormatBuffer, binary);
        super.debugCheckError();

        //copy binary length and format into the destination arrays
        if (length != null) {
            length[0] = lengthBuffer.get(0);
        }
        binaryFormat[0] = binaryFormatBuffer.get(0);
    }

    @Override
    public void glGetProgramBinary(int program, int[] length, int @NonNull [] binaryFormat, byte @NonNull [] binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        //get temporary direct buffer for downloading the binary
        //we can safely use APIUtil.getBufferByte() here: neither GL41.glGetProgramBinary() nor the ByteBuffer overload of glGetProgramBinary() use it
        val dataBuffer = this.beginHeapDownload(binary);
        this.glGetProgramBinary(program, length, binaryFormat, dataBuffer);
        this.finishHeapDownload(binary, dataBuffer);
    }

    @Override
    public void glProgramBinary(int program, int binaryFormat, @NonNull ByteBuffer binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        GL41.glProgramBinary(program, binaryFormat, binary);
        super.debugCheckError();
    }

    @Override
    public void glProgramBinary(int program, int binaryFormat, byte @NonNull [] binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        this.glProgramBinary(program, binaryFormat, this.getBufferForUpload(binary));
    }

    @Override
    public void glProgramUniform1i(int program, int location, int v0) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform1i(program, location, v0);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2i(int program, int location, int v0, int v1) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform2i(program, location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3i(int program, int location, int v0, int v1, int v2) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform3i(program, location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4i(int program, int location, int v0, int v1, int v2, int v3) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform4i(program, location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1ui(int program, int location, int v0) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform1ui(program, location, v0);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2ui(int program, int location, int v0, int v1) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform2ui(program, location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3ui(int program, int location, int v0, int v1, int v2) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform3ui(program, location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4ui(int program, int location, int v0, int v1, int v2, int v3) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform4ui(program, location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1f(int program, int location, float v0) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform1f(program, location, v0);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2f(int program, int location, float v0, float v1) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform2f(program, location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3f(int program, int location, float v0, float v1, float v2) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform3f(program, location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4f(int program, int location, float v0, float v1, float v2, float v3) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform4f(program, location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform1(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform2(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform3(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform4(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform1u(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform2u(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform3u(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform4u(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform1(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform2(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform3(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41.glProgramUniform4(program, location, value);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 4.2
    //
    //

    @Override
    public void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance) {
        super.checkSupported(this.GL_ARB_base_instance, GLExtension.GL_ARB_base_instance);

        GL42.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
        super.debugCheckError();
    }

    @Override
    public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices, int instancecount, int basevertex, int baseinstance) {
        super.checkSupported(this.GL_ARB_base_instance, GLExtension.GL_ARB_base_instance);

        GL42.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex, baseinstance);
        super.debugCheckError();
    }

    @Override
    public void glMemoryBarrier(int barriers) {
        super.checkSupported(this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_shader_image_load_store);

        GL42.glMemoryBarrier(barriers);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        super.checkSupported(this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_shader_image_load_store);

        GL42.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        super.debugCheckError();
    }

    @Override
    public void glTexStorage1D(int target, int levels, int internalformat, int width) {
        super.checkSupported(this.GL_ARB_texture_storage, GLExtension.GL_ARB_texture_storage);

        GL42.glTexStorage1D(target, levels, internalformat, width);
        super.debugCheckError();
    }

    @Override
    public void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        super.checkSupported(this.GL_ARB_texture_storage, GLExtension.GL_ARB_texture_storage);

        GL42.glTexStorage2D(target, levels, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public void glTexStorage3D(int target, int levels, int internalformat, int width, int height, int depth) {
        super.checkSupported(this.GL_ARB_texture_storage, GLExtension.GL_ARB_texture_storage);

        GL42.glTexStorage3D(target, levels, internalformat, width, height, depth);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 4.3
    //
    //

    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        super.checkSupported(this.GL_ARB_compute_shader, GLExtension.GL_ARB_compute_shader);

        GL43.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
        super.debugCheckError();
    }

    @Override
    public void glCopyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int srcWidth, int srcHeight, int srcDepth) {
        super.checkSupported(this.GL_ARB_copy_image, GLExtension.GL_ARB_copy_image);

        GL43.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateBufferData(int buffer) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43.glInvalidateBufferData(buffer);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateBufferSubData(int buffer, long offset, long length) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43.glInvalidateBufferSubData(buffer, offset, length);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateFramebuffer(int target, int attachment) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL43.glInvalidateFramebuffer() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(1).duplicate();
        attachmentsBuffer.put(attachment).flip();
        this.glInvalidateFramebuffer(target, attachmentsBuffer);
    }

    @Override
    public void glInvalidateFramebuffer(int target, int @NonNull [] attachments) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL43.glInvalidateFramebuffer() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(attachments.length).duplicate();
        attachmentsBuffer.put(attachments).flip();
        this.glInvalidateFramebuffer(target, attachmentsBuffer);
    }

    @Override
    public void glInvalidateFramebuffer(int target, @NonNull IntBuffer attachments) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43.glInvalidateFramebuffer(target, attachments);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int attachment, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL43.glInvalidateSubFramebuffer() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(1).duplicate();
        attachmentsBuffer.put(attachment).flip();
        this.glInvalidateSubFramebuffer(target, attachmentsBuffer, x, y, width, height);
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int @NonNull [] attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL43.glInvalidateSubFramebuffer() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(attachments.length).duplicate();
        attachmentsBuffer.put(attachments).flip();
        this.glInvalidateSubFramebuffer(target, attachmentsBuffer, x, y, width, height);
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, @NonNull IntBuffer attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateTexImage(int texture, int level) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43.glInvalidateTexImage(texture, level);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateTexSubImage(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43.glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth);
        super.debugCheckError();
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
        super.checkSupported(this.GL_ARB_multi_draw_indirect, GLExtension.GL_ARB_multi_draw_indirect);

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
        super.checkSupported(this.GL_ARB_multi_draw_indirect, GLExtension.GL_ARB_multi_draw_indirect);

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
    }

    @Override
    public int glGetProgramInterfacei(int program, int programInterface, int pname) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        val res = GL43.glGetProgramInterfacei(program, programInterface, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        val res = GL43.glGetProgramResourceIndex(program, programInterface, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull IntBuffer props, IntBuffer length, @NonNull IntBuffer params) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        if (LWJGLUtil.CHECKS) {
            checkArg(props.remaining() == params.remaining(), "props (%s) and params (%s) must have the same length!", props.remaining(), params.remaining());
        }

        GL43.glGetProgramResource(program, programInterface, index, props, length, params);
        super.debugCheckError();
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull int[] props, int[] length, @NonNull int[] params) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        //get temporary direct buffer for storing the arguments and results
        //we can safely use APIUtil.getBufferInt() here: GL43.glGetProgramResource() doesn't use it
        IntBuffer tmpBuffer = this.getBufferInt(props.length + params.length + (length != null ? 1 : 0));

        //copy props array to the direct buffer, and prepare IntBuffers for the params and length data to be returned into
        IntBuffer propsBuffer = (IntBuffer) tmpBuffer.duplicate().put(props).position(0).limit(props.length);
        IntBuffer paramsBuffer = (IntBuffer) tmpBuffer.duplicate().position(props.length).limit(props.length + params.length);
        IntBuffer lengthBuffer = length != null ? (IntBuffer) tmpBuffer.duplicate().position(props.length + params.length).limit(props.length + params.length + 1) : null;

        //copy the parameters to the direct buffer
        propsBuffer.put(props).flip();

        this.glGetProgramResourceiv(program, programInterface, index, propsBuffer, lengthBuffer, paramsBuffer);

        //copy the results back to the heap arrays
        paramsBuffer.get(params);
        if (length != null) {
            lengthBuffer.get(length);
        }
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index, int bufSize) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        val res = GL43.glGetProgramResourceName(program, programInterface, index, bufSize);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        super.checkSupported(this.GL_ARB_shader_storage_buffer_object, GLExtension.GL_ARB_shader_storage_buffer_object);

        GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        super.debugCheckError();
    }

    //LWJGL2 doesn't let us call these functions with a null data buffer
    private static final MethodHandle glClearBufferData;
    private static final MethodHandle nglClearBufferData;
    private static final MethodHandle glClearBufferSubData;
    private static final MethodHandle nglClearBufferSubData;

    static {
        Field _glClearBufferData = ContextCapabilities.class.getDeclaredField("glClearBufferData");
        _glClearBufferData.setAccessible(true);
        glClearBufferData = MethodHandles.publicLookup().unreflectGetter(_glClearBufferData);

        Method _nglClearBufferData = GL43.class.getDeclaredMethod("nglClearBufferData", int.class, int.class, int.class, int.class, long.class, long.class);
        _nglClearBufferData.setAccessible(true);
        nglClearBufferData = MethodHandles.publicLookup().unreflect(_nglClearBufferData);

        Field _glClearBufferSubData = ContextCapabilities.class.getDeclaredField("glClearBufferSubData");
        _glClearBufferSubData.setAccessible(true);
        glClearBufferSubData = MethodHandles.publicLookup().unreflectGetter(_glClearBufferSubData);

        Method _nglClearBufferSubData = GL43.class.getDeclaredMethod("nglClearBufferSubData", int.class, int.class, long.class, long.class, int.class, int.class, long.class, long.class);
        _nglClearBufferSubData.setAccessible(true);
        nglClearBufferSubData = MethodHandles.publicLookup().unreflect(_nglClearBufferSubData);
    }

    @Override
    public void glClearBufferData(int target, int internalformat, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_clear_buffer_object);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glClearBufferData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);

        nglClearBufferData.invokeExact(target, internalformat, format, type, MemoryUtil.getAddressSafe(data), function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glClearBufferSubData(int target, int internalformat, long offset, long size, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_clear_buffer_object);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glClearBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);

        nglClearBufferSubData.invokeExact(target, internalformat, offset, size, format, type, MemoryUtil.getAddressSafe(data), function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glObjectLabel(int identifier, int name, @NonNull CharSequence label) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glObjectLabel(identifier, name, label);
        super.debugCheckError();
    }

    @Override
    public void glObjectPtrLabel(long ptr, @NonNull CharSequence label) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glObjectPtrLabel((GLSync) GLSync_CTOR.invokeExact(ptr), label);
        super.debugCheckError();
    }

    @Override
    public String glGetObjectLabel(int identifier, int name) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        val res = GL43.glGetObjectLabel(identifier, name, this.limits().maxLabelLength());
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetObjectPtrLabel(long ptr) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        val res = GL43.glGetObjectPtrLabel((GLSync) GLSync_CTOR.invokeExact(ptr), this.limits().maxLabelLength());
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glDebugMessageControl(source, type, severity, ids, enabled);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, int[] ids, boolean enabled) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        if (ids == null) {
            this.glDebugMessageControl(source, type, severity, (IntBuffer) null, enabled);
        } else {
            //get temporary direct buffer for storing the arguments
            //we can safely use APIUtil.getBufferInt() here: GL43.glDebugMessageControl() doesn't use it
            IntBuffer idsBuffer = this.getBufferInt(ids.length).duplicate();
            idsBuffer.put(ids).flip();
            this.glDebugMessageControl(source, type, severity, idsBuffer, enabled);
        }
    }

    @Override
    public void glDebugMessageInsert(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glDebugMessageInsert(source, type, id, severity, msg);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageCallback(GLDebugOutputCallback callback) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glDebugMessageCallback(callback != null
                ? new org.lwjgl.opengl.KHRDebugCallback(callback::handleMessage)
                : null);
        super.debugCheckError();
    }

    @Override
    public void glPushDebugGroup(int source, int id, @NonNull CharSequence msg) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glPushDebugGroup(source, id, msg);
        super.debugCheckError();
    }

    @Override
    public void glPopDebugGroup() {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43.glPopDebugGroup();
        super.debugCheckError();
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
        super.checkSupported(this.GL_ARB_buffer_storage, GLExtension.GL_ARB_buffer_storage);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glBufferStorage.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglBufferStorage.invokeExact(target, data_size, data, flags, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glBufferStorage(int target, @NonNull ByteBuffer data, int flags) {
        super.checkSupported(this.GL_ARB_buffer_storage, GLExtension.GL_ARB_buffer_storage);

        GL44.glBufferStorage(target, data, flags);
        super.debugCheckError();
    }

    @Override
    public void glBufferStorage(int target, @NonNull AnyMemoryRegion data, int flags) {
        super.checkSupported(this.GL_ARB_buffer_storage, GLExtension.GL_ARB_buffer_storage);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glBufferStorage(target, (ByteBuffer) memory, flags);
        } else if (memory instanceof short[]) {
            GL44.glBufferStorage(target, this.getBufferForUpload((short[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL44.glBufferStorage(target, this.getBufferForUpload((int[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL44.glBufferStorage(target, this.getBufferForUpload((long[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL44.glBufferStorage(target, this.getBufferForUpload((float[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL44.glBufferStorage(target, this.getBufferForUpload((double[]) memory), flags);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public void glClearTexImage(int texture, int level, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_clear_texture, GLExtension.GL_ARB_clear_texture);

        GL44.glClearTexImage(texture, level, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glClearTexSubImage(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_clear_texture, GLExtension.GL_ARB_clear_texture);

        GL44.glClearTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffersBase(int target, int first, int count) {
        super.checkSupported(this.GL_ARB_multi_bind, GLExtension.GL_ARB_multi_bind);

        GL44.glBindBuffersBase(target, first, count, null);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull IntBuffer buffers) {
        super.checkSupported(this.GL_ARB_multi_bind, GLExtension.GL_ARB_multi_bind);

        GL44.glBindBuffersBase(target, first, buffers.remaining(), buffers);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull int[] buffers) {
        super.checkSupported(this.GL_ARB_multi_bind, GLExtension.GL_ARB_multi_bind);

        //get temporary direct buffer for storing the arguments
        //we can safely use APIUtil.getBufferInt() here: GL44.glBindBuffersBase() doesn't use it
        IntBuffer buffersBuffer = this.getBufferInt(buffers.length).duplicate();
        buffersBuffer.put(buffers).flip();
        this.glBindBuffersBase(target, first, buffersBuffer);
    }

    @Override
    public void glBindImageTextures(int first, int count) {
        super.checkSupported(this.GL_ARB_multi_bind & this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_multi_bind, GLExtension.GL_ARB_shader_image_load_store);

        GL44.glBindImageTextures(first, count, null);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTextures(int first, @NonNull IntBuffer textures) {
        super.checkSupported(this.GL_ARB_multi_bind & this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_multi_bind, GLExtension.GL_ARB_shader_image_load_store);

        GL44.glBindImageTextures(first, textures.remaining(), textures);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTextures(int first, int @NonNull [] textures) {
        super.checkSupported(this.GL_ARB_multi_bind & this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_multi_bind, GLExtension.GL_ARB_shader_image_load_store);

        //get temporary direct buffer for storing the arguments
        //we can safely use APIUtil.getBufferInt() here: GL44.glBindImageTextures() doesn't use it
        IntBuffer texturesBuffer = this.getBufferInt(textures.length).duplicate();
        texturesBuffer.put(textures).flip();
        this.glBindImageTextures(first, texturesBuffer);
    }

    //
    //
    // OpenGL 4.5
    //
    //

    @Override
    public void glClipControl(int origin, int depth) {
        super.checkSupported(this.GL_ARB_clip_control, GLExtension.GL_ARB_clip_control);

        GL45.glClipControl(origin, depth);
        super.debugCheckError();
    }

    @Override
    public int glCreateBuffer() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glCreateBuffers();
        super.debugCheckError();
        return res;
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
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glNamedBufferData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglNamedBufferData.invokeExact(buffer, data_size, data, usage, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glNamedBufferData(buffer, data, usage);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull AnyMemoryRegion data, int usage) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glNamedBufferData(buffer, (ByteBuffer) memory, usage);
        } else if (memory instanceof short[]) {
            GL45.glNamedBufferData(buffer, this.getBufferForUpload((short[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45.glNamedBufferData(buffer, this.getBufferForUpload((int[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL45.glNamedBufferData(buffer, this.getBufferForUpload((long[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45.glNamedBufferData(buffer, this.getBufferForUpload((float[]) memory), usage);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45.glNamedBufferData(buffer, this.getBufferForUpload((double[]) memory), usage);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
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
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_buffer_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_buffer_storage);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glNamedBufferStorage.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglNamedBufferStorage.invokeExact(buffer, data_size, data, flags, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_buffer_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_buffer_storage);

        GL45.glNamedBufferStorage(buffer, data, flags);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull AnyMemoryRegion data, int flags) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_buffer_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_buffer_storage);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glNamedBufferStorage(buffer, (ByteBuffer) memory, flags);
        } else if (memory instanceof short[]) {
            GL45.glNamedBufferStorage(buffer, this.getBufferForUpload((short[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45.glNamedBufferStorage(buffer, this.getBufferForUpload((int[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL45.glNamedBufferStorage(buffer, this.getBufferForUpload((long[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45.glNamedBufferStorage(buffer, this.getBufferForUpload((float[]) memory), flags);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45.glNamedBufferStorage(buffer, this.getBufferForUpload((double[]) memory), flags);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
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
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glNamedBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglNamedBufferSubData.invokeExact(buffer, offset, data_size, data, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glNamedBufferSubData(buffer, offset, data);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull AnyMemoryRegion data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glNamedBufferSubData(buffer, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            GL45.glNamedBufferSubData(buffer, offset, this.getBufferForUpload((short[]) memory));
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45.glNamedBufferSubData(buffer, offset, this.getBufferForUpload((int[]) memory));
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL45.glNamedBufferSubData(buffer, offset, this.getBufferForUpload((long[]) memory));
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45.glNamedBufferSubData(buffer, offset, this.getBufferForUpload((float[]) memory));
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45.glNamedBufferSubData(buffer, offset, this.getBufferForUpload((double[]) memory));
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
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
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glGetNamedBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);
        nglGetNamedBufferSubData.invokeExact(buffer, offset, data_size, data, function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glGetNamedBufferSubData(buffer, offset, data);
        super.debugCheckError();
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull AnyMemoryRegion data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glGetNamedBufferSubData(buffer, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            val dataArray = (short[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL45.glGetNamedBufferSubData(buffer, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof int[]) {
            val dataArray = (int[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL45.glGetNamedBufferSubData(buffer, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof long[]) {
            val dataArray = (long[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL45.glGetNamedBufferSubData(buffer, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof float[]) {
            val dataArray = (float[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL45.glGetNamedBufferSubData(buffer, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else if (memory instanceof double[]) {
            val dataArray = (double[]) memory;
            val dataBuffer = this.beginHeapDownload(dataArray);
            GL45.glGetNamedBufferSubData(buffer, offset, dataBuffer);
            super.debugCheckError();
            this.finishHeapDownload(dataArray, dataBuffer);
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public int glGetNamedBufferParameteri(int buffer, int pname) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glGetNamedBufferParameteri(buffer, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public long glMapNamedBuffer(int buffer, int access) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = PUnsafe.pork_directBufferAddress(GL45.glMapNamedBuffer(buffer, access, null));
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glMapNamedBuffer(buffer, access, length, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long size, int access) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = PUnsafe.pork_directBufferAddress(GL45.glMapNamedBufferRange(buffer, offset, size, access, null));
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glFlushMappedNamedBufferRange(int buffer, long offset, long length) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glFlushMappedNamedBufferRange(buffer, offset, length);
        super.debugCheckError();
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glUnmapNamedBuffer(buffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_copy_buffer, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_copy_buffer);

        GL45.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
        super.debugCheckError();
    }

    //LWJGL2 doesn't let us call these functions with a null data buffer
    private static final MethodHandle glClearNamedBufferData;
    private static final MethodHandle nglClearNamedBufferData;
    private static final MethodHandle glClearNamedBufferSubData;
    private static final MethodHandle nglClearNamedBufferSubData;

    static {
        Field _glClearNamedBufferData = ContextCapabilities.class.getDeclaredField("glClearNamedBufferData");
        _glClearNamedBufferData.setAccessible(true);
        glClearNamedBufferData = MethodHandles.publicLookup().unreflectGetter(_glClearNamedBufferData);

        Method _nglClearNamedBufferData = GL45.class.getDeclaredMethod("nglClearNamedBufferData", int.class, int.class, int.class, int.class, long.class, long.class);
        _nglClearNamedBufferData.setAccessible(true);
        nglClearNamedBufferData = MethodHandles.publicLookup().unreflect(_nglClearNamedBufferData);

        Field _glClearNamedBufferSubData = ContextCapabilities.class.getDeclaredField("glClearNamedBufferSubData");
        _glClearNamedBufferSubData.setAccessible(true);
        glClearNamedBufferSubData = MethodHandles.publicLookup().unreflectGetter(_glClearNamedBufferSubData);

        Method _nglClearNamedBufferSubData = GL45.class.getDeclaredMethod("nglClearNamedBufferSubData", int.class, int.class, long.class, long.class, int.class, int.class, long.class, long.class);
        _nglClearNamedBufferSubData.setAccessible(true);
        nglClearNamedBufferSubData = MethodHandles.publicLookup().unreflect(_nglClearNamedBufferSubData);
    }

    @Override
    public void glClearNamedBufferData(int buffer, int internalformat, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_clear_buffer_object);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glClearNamedBufferData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);

        nglClearNamedBufferData.invokeExact(buffer, internalformat, format, type, MemoryUtil.getAddressSafe(data), function_pointer);
        super.debugCheckError();
    }

    @Override
    public void glClearNamedBufferSubData(int buffer, int internalformat, long offset, long size, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_clear_buffer_object);

        ContextCapabilities caps = GLContext.getCapabilities();
        long function_pointer = (long) glClearNamedBufferSubData.invokeExact(caps);
        BufferChecks.checkFunctionAddress(function_pointer);

        nglClearNamedBufferSubData.invokeExact(buffer, internalformat, offset, size, format, type, MemoryUtil.getAddressSafe(data), function_pointer);
        super.debugCheckError();
    }

    @Override
    public int glCreateVertexArray() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glCreateVertexArrays();
        super.debugCheckError();
        if (LWJGLUtil.CHECKS) { // Prevents the workaround in glVertexArrayElementBuffer from throwing an NPE
            StateTracker_createVAO.invokeExact(GLContext.getCapabilities(), res);
        }
        return res;
    }

    @Override
    public void glVertexArrayElementBuffer(int vaobj, int buffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glVertexArrayElementBuffer(vaobj, buffer);
        super.debugCheckError();

        if (LWJGLUtil.CHECKS) {
            //LWJGL2 runtime checks will prevent us from using glDrawElements* with a buffer offset if the
            //  bound VAO doesn't have an element array buffer attached, but doesn't track element arrays
            //  attached to a VAO by this function. We'll do it ourself!
            StateTracker_setVAOElementArrayBuffer.invokeExact(GLContext.getCapabilities(), vaobj, buffer);
        }
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glEnableVertexArrayAttrib(vaobj, index);
        super.debugCheckError();
    }

    @Override
    public void glDisableVertexArrayAttrib(int vaobj, int index) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glDisableVertexArrayAttrib(vaobj, index);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_multi_bind, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_multi_bind);

        if (buffers == null) {
            this.glVertexArrayVertexBuffers(vaobj, first, count, 0L, 0L, 0L);
        } else {
            //get temporary direct buffer for storing the results
            //we can safely use APIUtil.getBufferByte() here: GL45.glVertexArrayVertexBuffers() doesn't use either of them
            ByteBuffer tmpBuffer = this.getBufferByte(count * (Integer.BYTES + Integer.BYTES + PUnsafe.addressSize()));

            IntBuffer buffersBuffer = (IntBuffer) tmpBuffer.asIntBuffer().position(0).limit(count);
            IntBuffer stridesBuffer = (IntBuffer) tmpBuffer.asIntBuffer().position(count).limit(2 * count);
            PointerBuffer offsetsBuffer = new PointerBuffer((ByteBuffer) tmpBuffer.duplicate().position(2 * count * Integer.BYTES).limit(2 * count * Integer.BYTES + count * PUnsafe.addressSize()));

            //copy the heap arrays to the direct buffer
            buffersBuffer.put(buffers, 0, count).flip();
            stridesBuffer.put(strides, 0, count).flip();
            offsetsBuffer.put(offsets, 0, count).flip();

            this.glVertexArrayVertexBuffers(vaobj, first, count, MemoryUtil.getAddress(buffersBuffer), MemoryUtil.getAddress(offsetsBuffer), MemoryUtil.getAddress(stridesBuffer));
        }
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_multi_bind, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_multi_bind);

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

        GL45.glVertexArrayVertexBuffers(vaobj, first, count, wrappedBuffers, wrappedOffsets, wrappedStrides);
        super.debugCheckError();
    }

    @Override
    public int glCreateSampler() {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_sampler_objects, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_sampler_objects);

        val res = GL45.glCreateSamplers();
        super.debugCheckError();
        return res;
    }

    @Override
    public int glCreateTexture(int target) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glCreateTextures(target);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glBindTextureUnit(int unit, int texture) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glBindTextureUnit(unit, texture);
        super.debugCheckError();
    }

    @Override
    public void glTextureParameter(int texture, int pname, int param) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glTextureParameteri(texture, pname, param);
        super.debugCheckError();
    }

    @Override
    public void glTextureParameter(int texture, int pname, float param) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glTextureParameterf(texture, pname, param);
        super.debugCheckError();
    }

    @Override
    public int glGetTextureParameterInteger(int texture, int pname) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glGetTextureParameteri(texture, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public float glGetTextureParameterFloat(int texture, int pname) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glGetTextureParameterf(texture, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glTextureStorage1D(int texture, int levels, int internalformat, int width) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_texture_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_texture_storage);

        GL45.glTextureStorage1D(texture, levels, internalformat, width);
        super.debugCheckError();
    }

    @Override
    public void glTextureStorage2D(int texture, int levels, int internalformat, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_texture_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_texture_storage);

        GL45.glTextureStorage2D(texture, levels, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public void glTextureStorage3D(int texture, int levels, int internalformat, int width, int height, int depth) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_texture_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_texture_storage);

        GL45.glTextureStorage3D(texture, levels, internalformat, width, height, depth);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        this.glTextureSubImage1D(texture, level, xoffset, width, format, type,
                DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage1DStorage(format, type, width)));
    }

    @Override
    public void glTextureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glTextureSubImage1D(texture, level, xoffset, width, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        this.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type,
                DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage2DStorage(format, type, width, height)));
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage3D(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        this.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type,
                DirectBufferHackery.wrapByte(data, UtilsLWJGL2.calculateTexImage3DStorage(format, type, width, height, depth)));
    }

    @Override
    public void glTextureSubImage3D(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glGenerateTextureMipmap(int texture) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glGenerateTextureMipmap(texture);
        super.debugCheckError();
    }

    @Override
    public void glTextureBuffer(int texture, int internalformat, int buffer) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_transform_feedback2, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_transform_feedback2);

        GL45.glTextureBuffer(texture, internalformat, buffer);
        super.debugCheckError();
    }

    @Override
    public int glCreateRenderbuffer() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glCreateRenderbuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glNamedRenderbufferStorage(int renderbuffer, int internalformat, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glNamedRenderbufferStorage(renderbuffer, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public int glCreateFramebuffer() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glCreateFramebuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glNamedFramebufferTexture(int framebuffer, int attachment, int texture, int level) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
        super.debugCheckError();
    }

    @Override
    public void glNamedFramebufferRenderbuffer(int framebuffer, int attachment, int renderbuffertarget, int renderbuffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer);
        super.debugCheckError();
    }

    @Override
    public int glCheckNamedFramebufferStatus(int framebuffer, int target) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45.glCheckNamedFramebufferStatus(framebuffer, target);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glBlitNamedFramebuffer(int readFramebuffer, int drawFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45.glBlitNamedFramebuffer(readFramebuffer, drawFramebuffer, srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferData(int framebuffer, int attachment) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL45.glInvalidateNamedFramebufferData() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(1).duplicate();
        attachmentsBuffer.put(attachment).flip();
        this.glInvalidateNamedFramebufferData(framebuffer, attachmentsBuffer);
    }

    @Override
    public void glInvalidateNamedFramebufferData(int framebuffer, int @NonNull [] attachments) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL45.glInvalidateNamedFramebufferData() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(attachments.length).duplicate();
        attachmentsBuffer.put(attachments).flip();
        this.glInvalidateNamedFramebufferData(framebuffer, attachmentsBuffer);
    }

    @Override
    public void glInvalidateNamedFramebufferData(int framebuffer, @NonNull IntBuffer attachments) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45.glInvalidateNamedFramebufferData(framebuffer, attachments);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferSubData(int framebuffer, int attachment, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL45.glInvalidateNamedFramebufferSubData() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(1).duplicate();
        attachmentsBuffer.put(attachment).flip();
        this.glInvalidateNamedFramebufferSubData(framebuffer, attachmentsBuffer, x, y, width, height);
    }

    @Override
    public void glInvalidateNamedFramebufferSubData(int framebuffer, int @NonNull [] attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        //we can safely use APIUtil.getBufferInt() here: GL45.glInvalidateNamedFramebufferSubData() doesn't use it
        IntBuffer attachmentsBuffer = this.getBufferInt(attachments.length).duplicate();
        attachmentsBuffer.put(attachments).flip();
        this.glInvalidateNamedFramebufferSubData(framebuffer, attachmentsBuffer, x, y, width, height);
    }

    @Override
    public void glInvalidateNamedFramebufferSubData(int framebuffer, @NonNull IntBuffer attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45.glInvalidateNamedFramebufferSubData(framebuffer, attachments, x, y, width, height);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 4.6
    //
    //

    @Override
    public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawcount, int maxdrawcount, int stride) {
        super.checkSupported(this.GL_ARB_indirect_parameters, GLExtension.GL_ARB_indirect_parameters);

        /*if (this.OpenGL46) { //use the core function if possible
            GL46.glMultiDrawArraysIndirectCount(mode, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else*/
        { //fall back to the ARB function
            ARBIndirectParameters.glMultiDrawArraysIndirectCountARB(mode, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        }
    }

    @Override
    public void glMultiDrawElementsIndirectCount(int mode, int type, long indirect, long drawcount, int maxdrawcount, int stride) {
        super.checkSupported(this.GL_ARB_indirect_parameters, GLExtension.GL_ARB_indirect_parameters);

        /*if (this.OpenGL46) { //use the core function if possible
            GL46.glMultiDrawElementsIndirectCount(mode, type, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else*/
        { //fall back to the ARB function
            ARBIndirectParameters.glMultiDrawElementsIndirectCountARB(mode, type, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        }
    }

    //
    //
    // No OpenGL version
    //
    //

    @Override
    public void glDebugMessageControlARB(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        super.checkSupported(this.GL_ARB_debug_output, GLExtension.GL_ARB_debug_output);

        ARBDebugOutput.glDebugMessageControlARB(source, type, severity, ids, enabled);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageControlARB(int source, int type, int severity, int[] ids, boolean enabled) {
        super.checkSupported(this.GL_ARB_debug_output, GLExtension.GL_ARB_debug_output);

        if (ids == null) {
            this.glDebugMessageControlARB(source, type, severity, (IntBuffer) null, enabled);
        } else {
            //get temporary direct buffer for storing the arguments
            //we can safely use APIUtil.getBufferInt() here: ARBDebugOutput.glDebugMessageControlARB() doesn't use it
            IntBuffer idsBuffer = this.getBufferInt(ids.length).duplicate();
            idsBuffer.put(ids).flip();
            this.glDebugMessageControlARB(source, type, severity, idsBuffer, enabled);
        }
    }

    @Override
    public void glDebugMessageInsertARB(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        super.checkSupported(this.GL_ARB_debug_output, GLExtension.GL_ARB_debug_output);

        ARBDebugOutput.glDebugMessageInsertARB(source, type, id, severity, msg);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageCallbackARB(GLDebugOutputCallback callback) {
        super.checkSupported(this.GL_ARB_debug_output, GLExtension.GL_ARB_debug_output);

        ARBDebugOutput.glDebugMessageCallbackARB(callback != null
                ? new ARBDebugOutputCallback(callback::handleMessage)
                : null);
        super.debugCheckError();
    }

    @Override
    public void glBufferPageCommitmentARB(int target, long offset, long size, boolean commit) {
        super.checkSupported(this.GL_ARB_sparse_buffer, GLExtension.GL_ARB_sparse_buffer);

        ARBSparseBuffer.glBufferPageCommitmentARB(target, offset, size, commit);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferPageCommitmentARB(int buffer, long offset, long size, boolean commit) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_sparse_buffer, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_sparse_buffer);

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
    }

    //LWJGL2 doesn't expose glMaxShaderCompilerThreadsARB/KHR() at all, but it does have a bunch of other functions with compatible function signatures. I'm going to use GL41.nglBindProgramPipeline() since we already reflect into that
    // class. We're going to call nglBindProgramPipeline(), but pass it the pointer to glMaxShaderCompilerThreadsARB/KHR() instead!
    private static final MethodHandle nglBindProgramPipeline;

    static {
        Method _nglBindProgramPipeline = GL41.class.getDeclaredMethod("nglBindProgramPipeline", int.class, long.class);
        _nglBindProgramPipeline.setAccessible(true);
        nglBindProgramPipeline = MethodHandles.publicLookup().unreflect(_nglBindProgramPipeline);
    }

    @Override
    public void glMaxShaderCompilerThreadsARB(int count) {
        super.checkSupported(this.GL_ARB_parallel_shader_compile, GLExtension.GL_ARB_parallel_shader_compile);

        nglBindProgramPipeline.invokeExact(count, this.glMaxShaderCompilerThreadsARB);
        super.debugCheckError();
    }

    @Override
    public void glMaxShaderCompilerThreadsKHR(int count) {
        super.checkSupported(this.GL_KHR_parallel_shader_compile, GLExtension.GL_KHR_parallel_shader_compile);

        nglBindProgramPipeline.invokeExact(count, this.glMaxShaderCompilerThreadsKHR);
        super.debugCheckError();
    }
}
