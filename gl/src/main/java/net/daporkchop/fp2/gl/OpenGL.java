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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Provides access to the OpenGL API.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class OpenGL {
    public static final boolean DEBUG = Boolean.getBoolean("fp2.gl.opengl.debug");

    /**
     * @return an instance of {@link OpenGL} for accessing the current OpenGL context
     */
    public static OpenGL forCurrent() {
        return GlobalProperties.find(OpenGL.class, "opengl").<Supplier<? extends OpenGL>>getInstance("forCurrent.supplier").get();
    }

    private final GLVersion version;
    private final GLProfile profile;
    private final Set<GLExtension> extensions; //a set of the supported extensions, excluding those whose features are already available because they're core features in the current OpenGL version
    private final boolean forwardCompatibility;

    private final Limits limits;

    protected OpenGL() {
        this.version = this.determineVersion();

        { //get supported extensions
            Set<String> extensionNames;
            if (this.version.compareTo(GLVersion.OpenGL30) < 0) { //use old extensions field
                String extensions = this.glGetString(GL_EXTENSIONS);
                extensionNames = new HashSet<>(Arrays.asList(extensions.trim().split(" ")));
            } else { //use new indexed EXTENSIONS property
                extensionNames = IntStream.range(0, this.glGetInteger(GL_NUM_EXTENSIONS))
                        .mapToObj(i -> this.glGetString(GL_EXTENSIONS, i))
                        .collect(Collectors.toSet());
            }

            this.extensions = Stream.of(GLExtension.values())
                    .filter(extension -> !extension.core(this.version) && extensionNames.contains(extension.name()))
                    .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
        }

        { //get profile
            int contextFlags = 0;
            int contextProfileMask = 0;

            if (this.version.compareTo(GLVersion.OpenGL30) >= 0) { // >= 3.0, we can access context flags
                contextFlags = this.glGetInteger(GL_CONTEXT_FLAGS);

                if (this.version.compareTo(GLVersion.OpenGL32) >= 0) { // >= 3.2, we can access profile information
                    contextProfileMask = this.glGetInteger(GL_CONTEXT_PROFILE_MASK);
                }
            }

            boolean compat = (contextProfileMask & GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0;
            boolean core = (contextProfileMask & GL_CONTEXT_CORE_PROFILE_BIT) != 0;
            this.forwardCompatibility = (contextFlags & GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) != 0;

            if (this.version.compareTo(GLVersion.OpenGL31) <= 0) { // <= 3.1, profiles don't exist yet
                this.profile = GLProfile.UNKNOWN;
            } else { // >= 3.2
                assert !(compat && core) : "a context can't be using both core and compatibility profiles at once!";

                if (compat) {
                    this.profile = GLProfile.COMPAT;
                } else if (core) {
                    this.profile = GLProfile.CORE;
                } else {
                    this.profile = GLProfile.UNKNOWN;
                }
            }
        }

        this.limits = new Limits(this);
    }

    //
    //
    // UTILITIES
    //
    //

    /**
     * @return the maximum OpenGL version supported by the current context
     */
    protected abstract GLVersion determineVersion();

    /**
     * Checks if the features provided by the given OpenGL extension are supported by the current context.
     *
     * @param extension the OpenGL extension
     * @return {@code true} if the features provided by the given OpenGL extension are supported by the current context
     */
    public final boolean supports(GLExtension extension) {
        return extension.supported(this);
    }

    /**
     * If debugging is enabled, checks if an OpenGL error has occurred.
     *
     * @throws OpenGLException if debugging is enabled and an OpenGL error has occurred
     */
    public final void debugCheckError() {
        if (DEBUG) {
            this.checkError();
        }
    }

    /**
     * Checks if an OpenGL error has occurred.
     *
     * @throws OpenGLException if an OpenGL error has occurred
     */
    public final void checkError() {
        int errorCode = this.glGetError();
        if (errorCode != GL_NO_ERROR) {
            throw new OpenGLException(errorCode);
        }
    }

    //
    //
    // OpenGL 1.1
    //
    //

    public abstract void glEnable(int cap);

    public abstract void glDisable(int cap);

    public abstract int glGetError();

    public abstract boolean glGetBoolean(int pname);

    public abstract void glGetBoolean(int pname, long data);

    public abstract int glGetInteger(int pname);

    public abstract void glGetInteger(int pname, long data);

    public abstract float glGetFloat(int pname);

    public abstract void glGetFloat(int pname, long data);

    public abstract double glGetDouble(int pname);

    public abstract void glGetDouble(int pname, long data);

    public abstract String glGetString(int pname);

    public abstract void glDrawArrays(int mode, int first, int count);

    public abstract void glDrawElements(int mode, int count, int type, long indices);

    public abstract void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices);

    public abstract int glGenTexture();

    public abstract void glDeleteTexture(int texture);

    public abstract void glBindTexture(int target, int texture);

    public abstract void glTexParameter(int target, int pname, int param);

    public abstract void glTexParameter(int target, int pname, float param);

    public abstract int glGetTexParameterInteger(int target, int pname);

    public abstract float glGetTexParameterFloat(int target, int pname);

    public abstract void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data);

    public abstract void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data);

    public abstract void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data);

    public abstract void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data);

    public abstract void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data);

    public abstract void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data);

    public abstract void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data);

    public abstract void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data);

    public abstract void glClear(int mask);

    public abstract void glClearColor(float red, float green, float blue, float alpha);

    public abstract void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    public abstract void glClearDepth(double depth);

    public abstract void glDepthFunc(int func);

    public abstract void glDepthMask(boolean flag);

    public abstract void glClearStencil(int s);

    public abstract void glStencilFunc(int func, int ref, int mask);

    public abstract void glStencilMask(int mask);

    public abstract void glStencilOp(int sfail, int dpfail, int dppass);

    public abstract void glPushClientAttrib(int mask);

    public abstract void glPopClientAttrib();

    public abstract void glPushAttrib(int mask);

    public abstract void glPopAttrib();

    //
    //
    // OpenGL 1.2
    //
    //

    public abstract void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, long data);

    public abstract void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data);

    public abstract void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data);

    public abstract void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data);

    //
    //
    // OpenGL 1.3
    //
    //

    public abstract void glActiveTexture(int texture);

    //
    //
    // OpenGL 1.4
    //
    //

    public abstract void glMultiDrawArrays(int mode, long first, long count, int drawcount);

    public abstract void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount);

    public abstract void glBlendColor(float red, float green, float blue, float alpha);

    public abstract void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);

    //
    //
    // OpenGL 1.5
    //
    //

    public abstract int glGenBuffer();

    public abstract void glDeleteBuffer(int buffer);

    public abstract void glBindBuffer(int target, int buffer);

    public abstract void glBufferData(int target, long data_size, long data, int usage);

    public abstract void glBufferData(int target, @NonNull ByteBuffer data, int usage);

    public abstract void glBufferSubData(int target, long offset, long data_size, long data);

    public abstract void glBufferSubData(int target, long offset, @NonNull ByteBuffer data);

    public abstract void glGetBufferSubData(int target, long offset, long data_size, long data);

    public abstract void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data);

    public abstract long glMapBuffer(int target, int access);

    public abstract ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer oldBuffer);

    public abstract void glUnmapBuffer(int target);

    //
    //
    // OpenGL 2.0
    //
    //

    public abstract int glCreateShader(int type);

    public abstract void glDeleteShader(int shader);

    public abstract void glShaderSource(int shader, @NonNull CharSequence... source);

    public abstract void glCompileShader(int shader);

    public abstract int glGetShaderi(int shader, int pname);

    public abstract String glGetShaderInfoLog(int shader);

    public abstract int glCreateProgram();

    public abstract void glDeleteProgram(int program);

    public abstract void glAttachShader(int program, int shader);

    public abstract void glDetachShader(int program, int shader);

    public abstract void glLinkProgram(int program);

    public abstract int glGetProgrami(int program, int pname);

    public abstract String glGetProgramInfoLog(int program);

    public abstract void glUseProgram(int program);

    public abstract void glEnableVertexAttribArray(int index);

    public abstract void glDisableVertexAttribArray(int index);

    public abstract void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);

    public abstract void glBindAttribLocation(int program, int index, @NonNull CharSequence name);

    public abstract int glGetUniformLocation(int program, @NonNull CharSequence name);

    public abstract void glUniform(int location, int v0);

    public abstract void glUniform(int location, int v0, int v1);

    public abstract void glUniform(int location, int v0, int v1, int v2);

    public abstract void glUniform(int location, int v0, int v1, int v2, int v3);

    public abstract void glUniform(int location, float v0);

    public abstract void glUniform(int location, float v0, float v1);

    public abstract void glUniform(int location, float v0, float v1, float v2);

    public abstract void glUniform(int location, float v0, float v1, float v2, float v3);

    public abstract void glBlendEquationSeparate(int modeRGB, int modeAlpha);

    //
    //
    // OpenGL 3.0
    //
    //

    public abstract int glGetInteger(int pname, int idx);

    public abstract String glGetString(int pname, int idx);

    public abstract int glGenVertexArray();

    public abstract void glDeleteVertexArray(int array);

    public abstract void glBindVertexArray(int array);

    public abstract void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer);

    public abstract void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name);

    public abstract void glBindBufferBase(int target, int index, int buffer);

    public abstract void glBindBufferRange(int target, int index, int buffer, long offset, long size);

    public abstract void glBeginTransformFeedback(int primitiveMode);

    public abstract void glEndTransformFeedback();

    public abstract void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode);

    public abstract long glMapBufferRange(int target, long offset, long length, int access);

    public abstract ByteBuffer glMapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer);

    public abstract void glFlushMappedBufferRange(int target, long offset, long length);

    //
    //
    // OpenGL 3.1
    //
    //

    //GL_ARB_copy_buffer
    public abstract void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size);

    //GL_ARB_draw_instanced
    public abstract void glDrawArraysInstanced(int mode, int first, int count, int instancecount);

    //GL_ARB_texture_buffer_object
    public abstract void glTexBuffer(int target, int internalFormat, int buffer);

    //GL_ARB_uniform_buffer_object
    public abstract int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName);

    //GL_ARB_uniform_buffer_object
    public abstract void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding);

    //GL_ARB_uniform_buffer_object
    public abstract int[] glGetUniformIndices(int program, CharSequence[] uniformNames);

    //GL_ARB_uniform_buffer_object
    public final int glGetUniformIndices(int program, CharSequence uniformName) {
        return this.glGetUniformIndices(program, new CharSequence[]{ uniformName })[0];
    }

    //GL_ARB_uniform_buffer_object
    public abstract int glGetActiveUniformsi(int program, int uniformIndex, int pname);

    //
    //
    // OpenGL 3.2
    //
    //

    //GL_ARB_draw_elements_base_vertex
    public abstract void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex);

    //GL_ARB_draw_elements_base_vertex
    public abstract void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex);

    //GL_ARB_sync
    public abstract long glFenceSync(int condition, int flags);

    //GL_ARB_sync
    public abstract int glClientWaitSync(long sync, int flags, long timeout);

    //GL_ARB_sync
    public abstract int glGetSync(long sync, int pname);

    //GL_ARB_sync
    public abstract void glDeleteSync(long sync);

    //
    //
    // OpenGL 3.3
    //
    //

    //GL_ARB_instanced_arrays
    public abstract void glVertexAttribDivisor(int index, int divisor);

    //GL_ARB_sampler_objects
    public abstract int glGenSampler();

    //GL_ARB_sampler_objects
    public abstract void glDeleteSampler(int sampler);

    //GL_ARB_sampler_objects
    public abstract void glBindSampler(int unit, int sampler);

    //GL_ARB_sampler_objects
    public abstract void glSamplerParameter(int sampler, int pname, int param);

    //GL_ARB_sampler_objects
    public abstract void glSamplerParameter(int sampler, int pname, float param);

    //
    //
    // OpenGL 4.2
    //
    //

    //GL_ARB_shader_image_load_store
    public abstract void glMemoryBarrier(int barriers);

    //
    //
    // OpenGL 4.3
    //
    //

    //GL_ARB_multi_draw_indirect
    public abstract void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride);

    //GL_ARB_multi_draw_indirect
    public abstract void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride);

    //GL_ARB_program_interface_query
    public abstract int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name);

    //GL_ARB_shader_storage_buffer_object
    public abstract void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding);

    //
    //
    // OpenGL 4.5
    //
    //

    //GL_ARB_direct_state_access
    public abstract int glCreateBuffer();

    //GL_ARB_direct_state_access
    public abstract void glNamedBufferData(int buffer, long data_size, long data, int usage);

    //GL_ARB_direct_state_access
    public abstract void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage);

    //GL_ARB_direct_state_access
    public abstract void glNamedBufferSubData(int buffer, long offset, long data_size, long data);

    //GL_ARB_direct_state_access
    public abstract void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data);

    //GL_ARB_direct_state_access
    public abstract void glGetNamedBufferSubData(int buffer, long offset, long data_size, long data);

    //GL_ARB_direct_state_access
    public abstract void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data);

    //GL_ARB_direct_state_access
    public abstract long glMapNamedBuffer(int buffer, int access);

    //GL_ARB_direct_state_access
    public abstract ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer);

    //GL_ARB_direct_state_access
    public abstract long glMapNamedBufferRange(int buffer, long offset, long size, int access);

    //GL_ARB_direct_state_access
    public abstract ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer);

    //GL_ARB_direct_state_access
    public abstract void glFlushMappedNamedBufferRange(int buffer, long offset, long length);

    //GL_ARB_direct_state_access
    public abstract void glUnmapNamedBuffer(int buffer);

    //GL_ARB_direct_state_access
    public abstract void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size);

    //GL_ARB_direct_state_access
    public abstract int glCreateVertexArray();

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayElementBuffer(int vaobj, int buffer);

    //GL_ARB_direct_state_access
    public abstract void glEnableVertexArrayAttrib(int vaobj, int index);

    //GL_ARB_direct_state_access
    public abstract void glDisableVertexArrayAttrib(int vaobj, int index);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides);

    //GL_ARB_direct_state_access
    public abstract void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides);

    /**
     * Stores the upper limits for various features supported by an OpenGL context.
     *
     * @author DaPorkchop_
     */
    @Getter
    public static final class Limits {
        private final int maxFragmentColors;
        private final int maxShaderStorageBuffers;
        private final int maxTextureUnits;
        private final int maxVertexAttributes;
        private final int maxUniformBuffers;

        Limits(OpenGL gl) {
            this.maxFragmentColors = gl.glGetInteger(GL_MAX_DRAW_BUFFERS);
            this.maxShaderStorageBuffers = gl.supports(GLExtension.GL_ARB_shader_storage_buffer_object) ? gl.glGetInteger(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0;
            this.maxTextureUnits = gl.glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
            this.maxVertexAttributes = gl.glGetInteger(GL_MAX_VERTEX_ATTRIBS);
            this.maxUniformBuffers = gl.glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);
        }
    }
}
