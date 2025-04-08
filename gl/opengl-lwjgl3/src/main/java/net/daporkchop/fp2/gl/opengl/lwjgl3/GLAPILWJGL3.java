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

package net.daporkchop.fp2.gl.opengl.lwjgl3;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.AnyMemoryRegion;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputCallback;
import net.daporkchop.lib.common.function.throwing.TPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBIndirectParameters;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBParallelShaderCompile;
import org.lwjgl.opengl.ARBSparseBuffer;
import org.lwjgl.opengl.ARBTextureBufferObject;
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
import org.lwjgl.opengl.GL41C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRParallelShaderCompile;
import org.lwjgl.system.Checks;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.lang.invoke.MethodHandles;
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GLAPILWJGL3 extends OpenGL {
    public static OpenGL forCurrent() {
        return new GLAPILWJGL3();
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

    private static GLCapabilities getICD() {
        //this is slower than it could be, but that's fine for now
        return GL.getCapabilities();
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
    public void glFlush() {
        GL11C.glFlush();
        super.debugCheckError();
    }

    @Override
    public boolean glGetBoolean(int pname) {
        val res = GL11C.glGetBoolean(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetBoolean(int pname, @NonNull ByteBuffer data) {
        GL11C.glGetBooleanv(pname, data);
        super.debugCheckError();
    }

    @Override
    public void glGetBoolean(int pname, boolean @NonNull [] data) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            //allocate temporary direct buffer for results
            ByteBuffer dataBuffer = stack.malloc(data.length);

            this.glGetBoolean(pname, dataBuffer);

            //copy result values into destination array
            for (int i = 0; i < data.length; i++) {
                data[i] = dataBuffer.get() != 0;
            }
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    @Override
    public int glGetInteger(int pname) {
        val res = GL11C.glGetInteger(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetInteger(int pname, @NonNull IntBuffer data) {
        GL11C.glGetIntegerv(pname, data);
        super.debugCheckError();
    }

    @Override
    public void glGetInteger(int pname, int @NonNull [] data) {
        GL11C.glGetIntegerv(pname, data);
        super.debugCheckError();
    }

    @Override
    public float glGetFloat(int pname) {
        val res = GL11C.glGetFloat(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetFloat(int pname, @NonNull FloatBuffer data) {
        GL11C.glGetFloatv(pname, data);
        super.debugCheckError();
    }

    @Override
    public void glGetFloat(int pname, float @NonNull [] data) {
        GL11C.glGetFloatv(pname, data);
        super.debugCheckError();
    }

    @Override
    public double glGetDouble(int pname) {
        val res = GL11C.glGetDouble(pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetDouble(int pname, @NonNull DoubleBuffer data) {
        GL11C.glGetDoublev(pname, data);
        super.debugCheckError();
    }

    @Override
    public void glGetDouble(int pname, double @NonNull [] data) {
        GL11C.glGetDoublev(pname, data);
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
    public void glViewport(int x, int y, int width, int height) {
        GL11C.glViewport(x, y, width, height);
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
    public void glBufferData(int target, @NonNull AnyMemoryRegion data, int usage) {
        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glBufferData(target, (ByteBuffer) memory, usage);
        } else if (memory instanceof short[]) {
            GL15C.glBufferData(target, (short[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL15C.glBufferData(target, (int[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL15C.glBufferData(target, (long[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL15C.glBufferData(target, (float[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL15C.glBufferData(target, (double[]) memory, usage);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
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
    public void glBufferSubData(int target, long offset, @NonNull AnyMemoryRegion data) {
        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glBufferSubData(target, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            GL15C.glBufferSubData(target, offset, (short[]) memory);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL15C.glBufferSubData(target, offset, (int[]) memory);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL15C.glBufferSubData(target, offset, (long[]) memory);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL15C.glBufferSubData(target, offset, (float[]) memory);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL15C.glBufferSubData(target, offset, (double[]) memory);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
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
    public void glGetBufferSubData(int target, long offset, @NonNull AnyMemoryRegion data) {
        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glGetBufferSubData(target, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            GL15C.glGetBufferSubData(target, offset, (short[]) memory);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL15C.glGetBufferSubData(target, offset, (int[]) memory);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL15C.glGetBufferSubData(target, offset, (long[]) memory);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL15C.glGetBufferSubData(target, offset, (float[]) memory);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL15C.glGetBufferSubData(target, offset, (double[]) memory);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public int glGetBufferParameteri(int target, int pname) {
        val res = GL15C.glGetBufferParameteri(target, pname);
        super.debugCheckError();
        return res;
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
        int[] res = PUnsafe.allocateUninitializedIntArray(count);
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
    public int glGetAttribLocation(int program, @NonNull CharSequence name) {
        val res = GL20C.glGetAttribLocation(program, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull IntBuffer size, @NonNull IntBuffer type) {
        val res = GL20C.glGetActiveAttrib(program, index, bufSize, size, type);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int bufSize, @NonNull int[] size, @NonNull int[] type) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            //allocate temporary direct buffer for results
            IntBuffer sizeBuffer = stack.mallocInt(1);
            IntBuffer typeBuffer = stack.mallocInt(1);

            val res = GL20C.glGetActiveAttrib(program, index, bufSize, sizeBuffer, typeBuffer);
            super.debugCheckError();

            //copy size and type into the destination arrays
            size[0] = sizeBuffer.get(0);
            type[0] = typeBuffer.get(0);
            return res;
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    @Override
    public int glGetUniformLocation(int program, @NonNull CharSequence name) {
        val res = GL20C.glGetUniformLocation(program, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUniform1i(int location, int v0) {
        GL20C.glUniform1i(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform2i(int location, int v0, int v1) {
        GL20C.glUniform2i(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform3i(int location, int v0, int v1, int v2) {
        GL20C.glUniform3i(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform4i(int location, int v0, int v1, int v2, int v3) {
        GL20C.glUniform4i(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1f(int location, float v0) {
        GL20C.glUniform1f(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        GL20C.glUniform2f(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        GL20C.glUniform3f(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        GL20C.glUniform4f(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1i(int location, IntBuffer value) {
        GL20C.glUniform1iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2i(int location, IntBuffer value) {
        GL20C.glUniform2iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3i(int location, IntBuffer value) {
        GL20C.glUniform3iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4i(int location, IntBuffer value) {
        GL20C.glUniform4iv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform1f(int location, FloatBuffer value) {
        GL20C.glUniform1fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2f(int location, FloatBuffer value) {
        GL20C.glUniform2fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3f(int location, FloatBuffer value) {
        GL20C.glUniform3fv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4f(int location, FloatBuffer value) {
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
    public boolean glGetBoolean(int pname, int idx) {
        val res = GL30C.glGetBooleani(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetBoolean(int pname, int idx, @NonNull ByteBuffer data) {
        GL30C.glGetBooleani_v(pname, idx, data);
        super.debugCheckError();
    }

    @Override
    public void glGetBoolean(int pname, int idx, boolean @NonNull [] data) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            //allocate temporary direct buffer for results
            ByteBuffer dataBuffer = stack.malloc(data.length);

            this.glGetBoolean(pname, idx, dataBuffer);

            //copy result values into destination array
            for (int i = 0; i < data.length; i++) {
                data[i] = dataBuffer.get() != 0;
            }
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    @Override
    public int glGetInteger(int pname, int idx) {
        val res = GL30C.glGetIntegeri(pname, idx);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetInteger(int pname, int idx, @NonNull IntBuffer data) {
        GL30C.glGetIntegeri_v(pname, idx, data);
        super.debugCheckError();
    }

    @Override
    public void glGetInteger(int pname, int idx, int @NonNull [] data) {
        GL30C.glGetIntegeri_v(pname, idx, data);
        super.debugCheckError();
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

    @Override
    public void glUniform1ui(int location, int v0) {
        GL30C.glUniform1ui(location, v0);
        super.debugCheckError();
    }

    @Override
    public void glUniform2ui(int location, int v0, int v1) {
        GL30C.glUniform2ui(location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glUniform3ui(int location, int v0, int v1, int v2) {
        GL30C.glUniform3ui(location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glUniform4ui(int location, int v0, int v1, int v2, int v3) {
        GL30C.glUniform4ui(location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glUniform1ui(int location, IntBuffer value) {
        GL30C.glUniform1uiv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform2ui(int location, IntBuffer value) {
        GL30C.glUniform2uiv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform3ui(int location, IntBuffer value) {
        GL30C.glUniform3uiv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glUniform4ui(int location, IntBuffer value) {
        GL30C.glUniform4uiv(location, value);
        super.debugCheckError();
    }

    @Override
    public void glGenerateMipmap(int target) {
        GL30C.glGenerateMipmap(target);
        super.debugCheckError();
    }

    @Override
    public int glGenRenderbuffer() {
        val res = GL30C.glGenRenderbuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {
        GL30C.glDeleteRenderbuffers(renderbuffer);
        super.debugCheckError();
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GL30C.glBindRenderbuffer(target, renderbuffer);
        super.debugCheckError();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GL30C.glRenderbufferStorage(target, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public int glGenFramebuffer() {
        val res = GL30C.glGenFramebuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        GL30C.glDeleteFramebuffers(framebuffer);
        super.debugCheckError();
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GL30C.glBindFramebuffer(target, framebuffer);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferTexture1D(int target, int attachment, int textarget, int texture, int level) {
        GL30C.glFramebufferTexture1D(target, attachment, textarget, texture, level);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GL30C.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferTexture3D(int target, int attachment, int textarget, int texture, int level, int layer) {
        GL30C.glFramebufferTexture3D(target, attachment, textarget, texture, level, layer);
        super.debugCheckError();
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GL30C.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        super.debugCheckError();
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        val res = GL30C.glCheckFramebufferStatus(target);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL30C.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
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

        GL31C.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        super.debugCheckError();
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        super.checkSupported(this.GL_ARB_draw_instanced, GLExtension.GL_ARB_draw_instanced);

        if (this.OpenGL31) { //use the core function if possible
            GL31C.glDrawArraysInstanced(mode, first, count, instancecount);
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
            GL31C.glDrawElementsInstanced(mode, count, type, indices, instancecount);
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
            GL31C.glTexBuffer(target, internalFormat, buffer);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBTextureBufferObject.glTexBufferARB(target, internalFormat, buffer);
            super.debugCheckError();
        }
    }

    @Override
    public int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31C.glGetUniformBlockIndex(program, uniformBlockName);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetActiveUniformBlocki(int program, int uniformBlockIndex, int pname) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31C.glGetActiveUniformBlocki(program, uniformBlockIndex, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex, int bufSize) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31C.glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        GL31C.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        super.debugCheckError();
    }

    @Override
    public int[] glGetUniformIndices(int program, CharSequence[] uniformNames) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            //allocate temporary direct buffer for results
            IntBuffer resultBuffer = stack.mallocInt(uniformNames.length);

            GL31C.glGetUniformIndices(program, uniformNames, resultBuffer);
            super.debugCheckError();

            //copy results to an array
            int[] result = PUnsafe.allocateUninitializedIntArray(uniformNames.length);
            resultBuffer.get(result);
            return result;
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    @Override
    public int glGetActiveUniformsi(int program, int uniformIndex, int pname) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31C.glGetActiveUniformsi(program, uniformIndex, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetActiveUniformName(int program, int uniformIndex, int bufSize) {
        super.checkSupported(this.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_uniform_buffer_object);

        val res = GL31C.glGetActiveUniformName(program, uniformIndex, bufSize);
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

        GL32C.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        super.debugCheckError();
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        super.checkSupported(this.GL_ARB_draw_elements_base_vertex, GLExtension.GL_ARB_draw_elements_base_vertex);

        GL32C.nglMultiDrawElementsBaseVertex(mode, count, type, indices, drawcount, basevertex);
        super.debugCheckError();
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        val res = GL32C.glFenceSync(condition, flags);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        val res = GL32C.glClientWaitSync(sync, flags, timeout);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetSync(long sync, int pname) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        val res = GL32C.glGetSynci(sync, pname, null);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteSync(long sync) {
        super.checkSupported(this.GL_ARB_sync, GLExtension.GL_ARB_sync);

        GL32C.glDeleteSync(sync);
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
            GL33C.glVertexAttribDivisor(index, divisor);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
            super.debugCheckError();
        }
    }

    @Override
    public int glGenSampler() {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        val res = GL33C.glGenSamplers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDeleteSampler(int sampler) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33C.glDeleteSamplers(sampler);
        super.debugCheckError();
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33C.glBindSampler(unit, sampler);
        super.debugCheckError();
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, int param) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33C.glSamplerParameteri(sampler, pname, param);
        super.debugCheckError();
    }

    @Override
    public void glSamplerParameter(int sampler, int pname, float param) {
        super.checkSupported(this.GL_ARB_sampler_objects, GLExtension.GL_ARB_sampler_objects);

        GL33C.glSamplerParameterf(sampler, pname, param);
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

        GL41C.glGetProgramBinary(program, length, binaryFormat, binary);
        super.debugCheckError();
    }

    @Override
    public void glGetProgramBinary(int program, int[] length, int @NonNull [] binaryFormat, byte @NonNull [] binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            //allocate temporary direct buffer for downloading the binary
            val binaryBuffer = stack.malloc(binary.length);
            this.glGetProgramBinary(program, length, binaryFormat, binaryBuffer);

            //copy the binary into the heap array
            binaryBuffer.get(binary);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    @Override
    public void glProgramBinary(int program, int binaryFormat, @NonNull ByteBuffer binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        GL41C.glProgramBinary(program, binaryFormat, binary);
        super.debugCheckError();
    }

    @Override
    public void glProgramBinary(int program, int binaryFormat, byte @NonNull [] binary) {
        super.checkSupported(this.GL_ARB_get_program_binary, GLExtension.GL_ARB_get_program_binary);

        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            this.glProgramBinary(program, binaryFormat, stack.bytes(binary));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    @Override
    public void glProgramUniform1i(int program, int location, int v0) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform1i(program, location, v0);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2i(int program, int location, int v0, int v1) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform2i(program, location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3i(int program, int location, int v0, int v1, int v2) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform3i(program, location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4i(int program, int location, int v0, int v1, int v2, int v3) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform4i(program, location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1ui(int program, int location, int v0) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform1ui(program, location, v0);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2ui(int program, int location, int v0, int v1) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform2ui(program, location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3ui(int program, int location, int v0, int v1, int v2) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform3ui(program, location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4ui(int program, int location, int v0, int v1, int v2, int v3) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform4ui(program, location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1f(int program, int location, float v0) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform1f(program, location, v0);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2f(int program, int location, float v0, float v1) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform2f(program, location, v0, v1);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3f(int program, int location, float v0, float v1, float v2) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform3f(program, location, v0, v1, v2);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4f(int program, int location, float v0, float v1, float v2, float v3) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform4f(program, location, v0, v1, v2, v3);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform1iv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform2iv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform3iv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4i(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform4iv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform1uiv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform2uiv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform3uiv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4ui(int program, int location, IntBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform4uiv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform1f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform1fv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform2f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform2fv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform3f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform3fv(program, location, value);
        super.debugCheckError();
    }

    @Override
    public void glProgramUniform4f(int program, int location, FloatBuffer value) {
        super.checkSupported(this.GL_ARB_separate_shader_objects, GLExtension.GL_ARB_separate_shader_objects);

        GL41C.glProgramUniform4fv(program, location, value);
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

        GL42C.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
        super.debugCheckError();
    }

    @Override
    public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices, int instancecount, int basevertex, int baseinstance) {
        super.checkSupported(this.GL_ARB_base_instance, GLExtension.GL_ARB_base_instance);

        GL42C.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex, baseinstance);
        super.debugCheckError();
    }

    @Override
    public void glMemoryBarrier(int barriers) {
        super.checkSupported(this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_shader_image_load_store);

        GL42C.glMemoryBarrier(barriers);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        super.checkSupported(this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_shader_image_load_store);

        GL42C.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        super.debugCheckError();
    }

    @Override
    public void glTexStorage1D(int target, int levels, int internalformat, int width) {
        super.checkSupported(this.GL_ARB_texture_storage, GLExtension.GL_ARB_texture_storage);

        GL42C.glTexStorage1D(target, levels, internalformat, width);
        super.debugCheckError();
    }

    @Override
    public void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        super.checkSupported(this.GL_ARB_texture_storage, GLExtension.GL_ARB_texture_storage);

        GL42C.glTexStorage2D(target, levels, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public void glTexStorage3D(int target, int levels, int internalformat, int width, int height, int depth) {
        super.checkSupported(this.GL_ARB_texture_storage, GLExtension.GL_ARB_texture_storage);

        GL42C.glTexStorage3D(target, levels, internalformat, width, height, depth);
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

        GL43C.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateBufferData(int buffer) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateBufferData(buffer);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateBufferSubData(int buffer, long offset, long length) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateBufferSubData(buffer, offset, length);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateFramebuffer(int target, int attachment) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateFramebuffer(target, attachment);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateFramebuffer(int target, int @NonNull [] attachments) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateFramebuffer(target, attachments);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateFramebuffer(int target, @NonNull IntBuffer attachments) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateFramebuffer(target, attachments);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int attachment, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateSubFramebuffer(target, attachment, x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int @NonNull [] attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, @NonNull IntBuffer attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateTexImage(int texture, int level) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateTexImage(texture, level);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateTexSubImage(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth) {
        super.checkSupported(this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_invalidate_subdata);

        GL43C.glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth);
        super.debugCheckError();
    }

    @Override
    public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
        super.checkSupported(this.GL_ARB_multi_draw_indirect, GLExtension.GL_ARB_multi_draw_indirect);

        GL43C.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        super.debugCheckError();
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride) {
        super.checkSupported(this.GL_ARB_multi_draw_indirect, GLExtension.GL_ARB_multi_draw_indirect);

        GL43C.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
        super.debugCheckError();
    }

    @Override
    public int glGetProgramInterfacei(int program, int programInterface, int pname) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        val res = GL43C.glGetProgramInterfacei(program, programInterface, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        val res = GL43C.glGetProgramResourceIndex(program, programInterface, name);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull IntBuffer props, IntBuffer length, @NonNull IntBuffer params) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        GL43C.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        super.debugCheckError();
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, @NonNull int[] props, int[] length, @NonNull int[] params) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        GL43C.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        super.debugCheckError();
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index, int bufSize) {
        super.checkSupported(this.GL_ARB_program_interface_query, GLExtension.GL_ARB_program_interface_query);

        val res = GL43C.glGetProgramResourceName(program, programInterface, index, bufSize);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        super.checkSupported(this.GL_ARB_shader_storage_buffer_object, GLExtension.GL_ARB_shader_storage_buffer_object);

        GL43C.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        super.debugCheckError();
    }

    @Override
    public void glClearBufferData(int target, int internalformat, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_clear_buffer_object);

        GL43C.glClearBufferData(target, internalformat, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glClearBufferSubData(int target, int internalformat, long offset, long size, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_clear_buffer_object);

        GL43C.glClearBufferSubData(target, internalformat, offset, size, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glObjectLabel(int identifier, int name, @NonNull CharSequence label) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glObjectLabel(identifier, name, label);
        super.debugCheckError();
    }

    @Override
    public void glObjectPtrLabel(long ptr, @NonNull CharSequence label) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glObjectPtrLabel(ptr, label);
        super.debugCheckError();
    }

    @Override
    public String glGetObjectLabel(int identifier, int name) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        val res = GL43C.glGetObjectLabel(identifier, name, this.limits().maxLabelLength());
        super.debugCheckError();
        return res;
    }

    @Override
    public String glGetObjectPtrLabel(long ptr) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        val res = GL43C.glGetObjectPtrLabel(ptr, this.limits().maxLabelLength());
        super.debugCheckError();
        return res;
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glDebugMessageControl(source, type, severity, ids, enabled);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageControl(int source, int type, int severity, int[] ids, boolean enabled) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glDebugMessageControl(source, type, severity, ids, enabled);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageInsert(int source, int type, int id, int severity, @NonNull CharSequence msg) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glDebugMessageInsert(source, type, id, severity, msg);
        super.debugCheckError();
    }

    @Override
    public void glDebugMessageCallback(GLDebugOutputCallback callback) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glDebugMessageCallback(callback != null
                ? GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> callback.handleMessage(source, type, id, severity, GLDebugMessageCallback.getMessage(length, message)))
                : null, 0L);
        super.debugCheckError();
    }

    @Override
    public void glPushDebugGroup(int source, int id, @NonNull CharSequence msg) {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glPushDebugGroup(source, id, msg);
        super.debugCheckError();
    }

    @Override
    public void glPopDebugGroup() {
        super.checkSupported(this.GL_KHR_debug, GLExtension.GL_KHR_debug);

        GL43C.glPopDebugGroup();
        super.debugCheckError();
    }

    //
    //
    // OpenGL 4.4
    //
    //

    @Override
    public void glBufferStorage(int target, long data_size, long data, int flags) {
        super.checkSupported(this.GL_ARB_buffer_storage, GLExtension.GL_ARB_buffer_storage);

        GL44C.nglBufferStorage(target, data_size, data, flags);
        super.debugCheckError();
    }

    @Override
    public void glBufferStorage(int target, @NonNull ByteBuffer data, int flags) {
        super.checkSupported(this.GL_ARB_buffer_storage, GLExtension.GL_ARB_buffer_storage);

        GL44C.glBufferStorage(target, data, flags);
        super.debugCheckError();
    }

    @Override
    public void glBufferStorage(int target, @NonNull AnyMemoryRegion data, int flags) {
        super.checkSupported(this.GL_ARB_buffer_storage, GLExtension.GL_ARB_buffer_storage);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glBufferStorage(target, (ByteBuffer) memory, flags);
        } else if (memory instanceof short[]) {
            GL44C.glBufferStorage(target, (short[]) memory, flags);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL44C.glBufferStorage(target, (int[]) memory, flags);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            //for some reason GL44C.glBufferStorage() doesn't have an overload for long[], so we'll implement it manually
            long __functionAddress = getICD().glBufferStorage;
            if (Checks.CHECKS) {
                Checks.check(__functionAddress);
            }
            JNI.callPPV(target, Integer.toUnsignedLong(((long[]) memory).length) << 3, (long[]) memory, flags, __functionAddress);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL44C.glBufferStorage(target, (float[]) memory, flags);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL44C.glBufferStorage(target, (double[]) memory, flags);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public void glBindBuffersBase(int target, int first, int count) {
        super.checkSupported(this.GL_ARB_multi_bind, GLExtension.GL_ARB_multi_bind);

        GL44C.nglBindBuffersBase(target, first, count, 0L);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull IntBuffer buffers) {
        super.checkSupported(this.GL_ARB_multi_bind, GLExtension.GL_ARB_multi_bind);

        GL44C.glBindBuffersBase(target, first, buffers);
        super.debugCheckError();
    }

    @Override
    public void glBindBuffersBase(int target, int first, @NonNull int[] buffers) {
        super.checkSupported(this.GL_ARB_multi_bind, GLExtension.GL_ARB_multi_bind);

        GL44C.glBindBuffersBase(target, first, buffers);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTextures(int first, int count) {
        super.checkSupported(this.GL_ARB_multi_bind & this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_multi_bind, GLExtension.GL_ARB_shader_image_load_store);

        GL44C.nglBindImageTextures(first, count, 0L);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTextures(int first, @NonNull IntBuffer textures) {
        super.checkSupported(this.GL_ARB_multi_bind & this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_multi_bind, GLExtension.GL_ARB_shader_image_load_store);

        GL44C.glBindImageTextures(first, textures);
        super.debugCheckError();
    }

    @Override
    public void glBindImageTextures(int first, int @NonNull [] textures) {
        super.checkSupported(this.GL_ARB_multi_bind & this.GL_ARB_shader_image_load_store, GLExtension.GL_ARB_multi_bind, GLExtension.GL_ARB_shader_image_load_store);

        GL44C.glBindImageTextures(first, textures);
        super.debugCheckError();
    }

    //
    //
    // OpenGL 4.5
    //
    //

    @Override
    public void glClipControl(int origin, int depth) {
        super.checkSupported(this.GL_ARB_clip_control, GLExtension.GL_ARB_clip_control);

        GL45C.glClipControl(origin, depth);
        super.debugCheckError();
    }

    @Override
    public int glCreateBuffer() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glCreateBuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glNamedBufferData(int buffer, long data_size, long data, int usage) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglNamedBufferData(buffer, data_size, data, usage);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glNamedBufferData(buffer, data, usage);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferData(int buffer, @NonNull AnyMemoryRegion data, int usage) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glNamedBufferData(buffer, (ByteBuffer) memory, usage);
        } else if (memory instanceof short[]) {
            GL45C.glNamedBufferData(buffer, (short[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45C.glNamedBufferData(buffer, (int[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL45C.glNamedBufferData(buffer, (long[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45C.glNamedBufferData(buffer, (float[]) memory, usage);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45C.glNamedBufferData(buffer, (double[]) memory, usage);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public void glNamedBufferStorage(int buffer, long data_size, long data, int flags) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_buffer_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_buffer_storage);

        GL45C.nglNamedBufferStorage(buffer, data_size, data, flags);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_buffer_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_buffer_storage);

        GL45C.glNamedBufferStorage(buffer, data, flags);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferStorage(int buffer, @NonNull AnyMemoryRegion data, int flags) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_buffer_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_buffer_storage);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glNamedBufferStorage(buffer, (ByteBuffer) memory, flags);
        } else if (memory instanceof short[]) {
            GL45C.glNamedBufferStorage(buffer, (short[]) memory, flags);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45C.glNamedBufferStorage(buffer, (int[]) memory, flags);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            //for some reason GL45C.glNamedBufferStorage() doesn't have an overload for long[], so we'll implement it manually
            long __functionAddress = getICD().glNamedBufferStorage;
            if (Checks.CHECKS) {
                Checks.check(__functionAddress);
            }
            JNI.callPPV(buffer, Integer.toUnsignedLong(((long[]) memory).length) << 3, (long[]) memory, flags, __functionAddress);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45C.glNamedBufferStorage(buffer, (float[]) memory, flags);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45C.glNamedBufferStorage(buffer, (double[]) memory, flags);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, long data_size, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglNamedBufferSubData(buffer, offset, data_size, data);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glNamedBufferSubData(buffer, offset, data);
        super.debugCheckError();
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, @NonNull AnyMemoryRegion data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glNamedBufferSubData(buffer, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            GL45C.glNamedBufferSubData(buffer, offset, (short[]) memory);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45C.glNamedBufferSubData(buffer, offset, (int[]) memory);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL45C.glNamedBufferSubData(buffer, offset, (long[]) memory);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45C.glNamedBufferSubData(buffer, offset, (float[]) memory);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45C.glNamedBufferSubData(buffer, offset, (double[]) memory);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, long data_size, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglGetNamedBufferSubData(buffer, offset, data_size, data);
        super.debugCheckError();
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glGetNamedBufferSubData(buffer, offset, data);
        super.debugCheckError();
    }

    @Override
    public void glGetNamedBufferSubData(int buffer, long offset, @NonNull AnyMemoryRegion data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val memory = data.memory;
        if (memory instanceof ByteBuffer) {
            this.glGetNamedBufferSubData(buffer, offset, (ByteBuffer) memory);
        } else if (memory instanceof short[]) {
            GL45C.glGetNamedBufferSubData(buffer, offset, (short[]) memory);
            super.debugCheckError();
        } else if (memory instanceof int[]) {
            GL45C.glGetNamedBufferSubData(buffer, offset, (int[]) memory);
            super.debugCheckError();
        } else if (memory instanceof long[]) {
            GL45C.glGetNamedBufferSubData(buffer, offset, (long[]) memory);
            super.debugCheckError();
        } else if (memory instanceof float[]) {
            GL45C.glGetNamedBufferSubData(buffer, offset, (float[]) memory);
            super.debugCheckError();
        } else if (memory instanceof double[]) {
            GL45C.glGetNamedBufferSubData(buffer, offset, (double[]) memory);
            super.debugCheckError();
        } else {
            throw new IllegalArgumentException(String.valueOf(memory));
        }
    }

    @Override
    public int glGetNamedBufferParameteri(int buffer, int pname) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glGetNamedBufferParameteri(buffer, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public long glMapNamedBuffer(int buffer, int access) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.nglMapNamedBuffer(buffer, access);
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glMapNamedBuffer(buffer, access, length, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long size, int access) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = PUnsafe.pork_directBufferAddress(GL45C.glMapNamedBufferRange(buffer, offset, size, access, null));
        super.debugCheckError();
        return res;
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glMapNamedBufferRange(buffer, offset, size, access, oldBuffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glFlushMappedNamedBufferRange(int buffer, long offset, long length) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glFlushMappedNamedBufferRange(buffer, offset, length);
        super.debugCheckError();
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glUnmapNamedBuffer(buffer);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_copy_buffer, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_copy_buffer);

        GL45C.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
        super.debugCheckError();
    }

    @Override
    public void glClearNamedBufferData(int buffer, int internalformat, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_clear_buffer_object);

        GL45C.glClearNamedBufferData(buffer, internalformat, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glClearNamedBufferSubData(int buffer, int internalformat, long offset, long size, int format, int type, ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_clear_buffer_object, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_clear_buffer_object);

        GL45C.glClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, data);
        super.debugCheckError();
    }

    @Override
    public int glCreateVertexArray() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glCreateVertexArrays();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glVertexArrayElementBuffer(int vaobj, int buffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glVertexArrayElementBuffer(vaobj, buffer);
        super.debugCheckError();
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glEnableVertexArrayAttrib(vaobj, index);
        super.debugCheckError();
    }

    @Override
    public void glDisableVertexArrayAttrib(int vaobj, int index) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glDisableVertexArrayAttrib(vaobj, index);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45C.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45C.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45C.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45C.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_vertex_attrib_binding, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_vertex_attrib_binding);

        GL45C.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
        super.debugCheckError();
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        if (buffers == null) {
            GL45C.nglVertexArrayVertexBuffers(vaobj, first, count, 0L, 0L, 0L);
            super.debugCheckError();
        } else {
            MemoryStack stack = MemoryStack.stackGet();
            int stackPointer = stack.getPointer();
            try {
                //allocate temporary direct buffer for offsets
                PointerBuffer offsetsBuffer = stack.pointers(offsets);

                GL45C.glVertexArrayVertexBuffers(vaobj, first, buffers, offsetsBuffer, strides);
                super.debugCheckError();
            } finally {
                stack.setPointer(stackPointer);
            }
        }
    }

    @Override
    public void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglVertexArrayVertexBuffers(vaobj, first, count, buffers, offsets, strides);
        super.debugCheckError();
    }

    @Override
    public int glCreateTexture(int target) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glCreateTextures(target);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glBindTextureUnit(int unit, int texture) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glBindTextureUnit(unit, texture);
        super.debugCheckError();
    }

    @Override
    public void glTextureParameter(int texture, int pname, int param) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glTextureParameteri(texture, pname, param);
        super.debugCheckError();
    }

    @Override
    public void glTextureParameter(int texture, int pname, float param) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glTextureParameterf(texture, pname, param);
        super.debugCheckError();
    }

    @Override
    public int glGetTextureParameterInteger(int texture, int pname) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glGetTextureParameteri(texture, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public float glGetTextureParameterFloat(int texture, int pname) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glGetTextureParameterf(texture, pname);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glTextureStorage1D(int texture, int levels, int internalformat, int width) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_texture_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_texture_storage);

        GL45C.glTextureStorage1D(texture, levels, internalformat, width);
        super.debugCheckError();
    }

    @Override
    public void glTextureStorage2D(int texture, int levels, int internalformat, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_texture_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_texture_storage);

        GL45C.glTextureStorage2D(texture, levels, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public void glTextureStorage3D(int texture, int levels, int internalformat, int width, int height, int depth) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_texture_storage, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_texture_storage);

        GL45C.glTextureStorage3D(texture, levels, internalformat, width, height, depth);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglTextureSubImage1D(texture, level, xoffset, width, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glTextureSubImage1D(texture, level, xoffset, width, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage3D(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.nglTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glTextureSubImage3D(int texture, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        super.debugCheckError();
    }

    @Override
    public void glGenerateTextureMipmap(int texture) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glGenerateTextureMipmap(texture);
        super.debugCheckError();
    }

    @Override
    public void glTextureBuffer(int texture, int internalformat, int buffer) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_transform_feedback2, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_transform_feedback2);

        GL45C.glTextureBuffer(texture, internalformat, buffer);
        super.debugCheckError();
    }

    @Override
    public int glCreateRenderbuffer() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glCreateRenderbuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glNamedRenderbufferStorage(int renderbuffer, int internalformat, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glNamedRenderbufferStorage(renderbuffer, internalformat, width, height);
        super.debugCheckError();
    }

    @Override
    public int glCreateFramebuffer() {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glCreateFramebuffers();
        super.debugCheckError();
        return res;
    }

    @Override
    public void glNamedFramebufferTexture(int framebuffer, int attachment, int texture, int level) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
        super.debugCheckError();
    }

    @Override
    public void glNamedFramebufferRenderbuffer(int framebuffer, int attachment, int renderbuffertarget, int renderbuffer) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer);
        super.debugCheckError();
    }

    @Override
    public int glCheckNamedFramebufferStatus(int framebuffer, int target) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        val res = GL45C.glCheckNamedFramebufferStatus(framebuffer, target);
        super.debugCheckError();
        return res;
    }

    @Override
    public void glBlitNamedFramebuffer(int readFramebuffer, int drawFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        super.checkSupported(this.GL_ARB_direct_state_access, GLExtension.GL_ARB_direct_state_access);

        GL45C.glBlitNamedFramebuffer(readFramebuffer, drawFramebuffer, srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferData(int framebuffer, int attachment) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45C.glInvalidateNamedFramebufferData(framebuffer, attachment);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferData(int framebuffer, int @NonNull [] attachments) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45C.glInvalidateNamedFramebufferData(framebuffer, attachments);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferData(int framebuffer, @NonNull IntBuffer attachments) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45C.glInvalidateNamedFramebufferData(framebuffer, attachments);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferSubData(int framebuffer, int attachment, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45C.glInvalidateNamedFramebufferSubData(framebuffer, attachment, x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferSubData(int framebuffer, int @NonNull [] attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45C.glInvalidateNamedFramebufferSubData(framebuffer, attachments, x, y, width, height);
        super.debugCheckError();
    }

    @Override
    public void glInvalidateNamedFramebufferSubData(int framebuffer, @NonNull IntBuffer attachments, int x, int y, int width, int height) {
        super.checkSupported(this.GL_ARB_direct_state_access & this.GL_ARB_invalidate_subdata, GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_invalidate_subdata);

        GL45C.glInvalidateNamedFramebufferSubData(framebuffer, attachments, x, y, width, height);
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

        if (this.OpenGL46) { //use the core function if possible
            GL46C.glMultiDrawArraysIndirectCount(mode, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else { //fall back to the ARB function
            ARBIndirectParameters.glMultiDrawArraysIndirectCountARB(mode, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        }
    }

    @Override
    public void glMultiDrawElementsIndirectCount(int mode, int type, long indirect, long drawcount, int maxdrawcount, int stride) {
        super.checkSupported(this.GL_ARB_indirect_parameters, GLExtension.GL_ARB_indirect_parameters);

        if (this.OpenGL46) { //use the core function if possible
            GL46C.glMultiDrawElementsIndirectCount(mode, type, indirect, drawcount, maxdrawcount, stride);
            super.debugCheckError();
        } else { //fall back to the ARB function
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

        ARBDebugOutput.glDebugMessageControlARB(source, type, severity, ids, enabled);
        super.debugCheckError();
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
                ? GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam) -> callback.handleMessage(source, type, id, severity, GLDebugMessageARBCallback.getMessage(length, message)))
                : null, 0L);
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

        ARBSparseBuffer.glNamedBufferPageCommitmentARB(buffer, offset, size, commit);
        super.debugCheckError();
    }

    @Override
    public void glMaxShaderCompilerThreadsARB(int count) {
        super.checkSupported(this.GL_ARB_parallel_shader_compile, GLExtension.GL_ARB_parallel_shader_compile);

        ARBParallelShaderCompile.glMaxShaderCompilerThreadsARB(count);
        super.debugCheckError();
    }

    @Override
    public void glMaxShaderCompilerThreadsKHR(int count) {
        super.checkSupported(this.GL_KHR_parallel_shader_compile, GLExtension.GL_KHR_parallel_shader_compile);

        KHRParallelShaderCompile.glMaxShaderCompilerThreadsKHR(count);
        super.debugCheckError();
    }
}
