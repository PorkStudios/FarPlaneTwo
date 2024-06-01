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
import net.daporkchop.fp2.gl.compute.ComputeWorkGroupCount;
import net.daporkchop.fp2.gl.compute.ComputeWorkGroupSize;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.fp2.gl.util.debug.GLDebugOutputCallback;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
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
    public static final boolean DEBUG_OUTPUT = Boolean.getBoolean("fp2.gl.opengl.debug.output");
    public static final boolean PRESERVE_BINDINGS = System.getProperty("fp2.gl.opengl.preserveBindings") == null || Boolean.getBoolean("fp2.gl.opengl.preserveBindings");

    /**
     * @return an instance of {@link OpenGL} for accessing the current OpenGL context
     */
    public static OpenGL forCurrent() {
        return (OpenGL) MethodHandles.publicLookup().findStatic(
                        GlobalProperties.find(OpenGL.class, "opengl").getClass("factory"),
                        "forCurrent",
                        MethodType.methodType(OpenGL.class))
                .invokeExact();
    }

    private final GLVersion version;
    private final GLProfile profile;
    private final GLExtensionSet extensions; //a set of the supported extensions, excluding those whose features are already available because they're core features in the current OpenGL version
    private final boolean forwardCompatibility;

    private final GLExtensionSet allExtensions; //a set of all the supported extensions

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

            BiFunction<GLExtensionSet, GLExtension, GLExtensionSet> extensionSetReducer = GLExtensionSet::add;
            BinaryOperator<GLExtensionSet> extensionSetMerger = GLExtensionSet::addAll;

            this.extensions = Stream.of(GLExtension.values())
                    .filter(extension -> !extension.core(this.version) && extensionNames.contains(extension.name()))
                    .reduce(GLExtensionSet.empty(), extensionSetReducer, extensionSetMerger);

            this.allExtensions = Stream.of(GLExtension.values())
                    .filter(extension -> extension.core(this.version) || extensionNames.contains(extension.name()))
                    .reduce(GLExtensionSet.empty(), extensionSetReducer, extensionSetMerger);
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
     * Checks if the features provided by the given OpenGL extensions are supported by the current context.
     *
     * @param extensions the OpenGL extensions
     * @return {@code true} if the features provided by the given OpenGL extensions are supported by the current context
     */
    public final boolean supports(GLExtensionSet extensions) {
        return this.allExtensions.containsAll(extensions);
    }

    /**
     * Checks that the features provided by the given OpenGL extensions are supported by the current context.
     *
     * @param extensions the OpenGL extensions
     * @throws UnsupportedOperationException if any of the given extensions is not supported
     */
    public final void checkSupported(GLExtensionSet extensions) {
        if (!this.allExtensions.containsAll(extensions)) {
            throw new UnsupportedOperationException(this.unsupportedMsg(extensions));
        }
    }

    /**
     * Gets a message to be used as an exception message if a function from an extension which is not supported by the current context is called.
     *
     * @param extension the unsupported extension
     * @return a {@link String} to be used as an exception message
     */
    protected final String unsupportedMsg(GLExtension extension) {
        return this.unsupportedMsg(Collections.singletonList(extension));
    }

    /**
     * Gets a message to be used as an exception message if a function from a pair of extensions which are not both supported by the current context is called.
     *
     * @param extension0 the unsupported extension
     * @param extension1 the unsupported extension
     * @return a {@link String} to be used as an exception message
     */
    protected final String unsupportedMsg(GLExtension extension0, GLExtension extension1) {
        return this.unsupportedMsg(Arrays.asList(extension0, extension1));
    }

    /**
     * Gets a message to be used as an exception message if a function from a set of extensions which are not all supported by the current context is called.
     *
     * @param extensions the unsupported extensions
     * @return a {@link String} to be used as an exception message
     */
    private String unsupportedMsg(GLExtensionSet extensions) {
        return this.unsupportedMsg(Arrays.asList(extensions.toArray()));
    }

    /**
     * Gets a message to be used as an exception message if a function from a pair of extensions which are not both supported by the current context is called.
     *
     * @param extensions a list of extensions, at least one of which is unsupported
     * @return a {@link String} to be used as an exception message
     */
    private String unsupportedMsg(List<GLExtension> extensions) {
        StringBuilder builder = new StringBuilder().append("requires ");

        boolean includeCoreVersion;
        if (extensions.stream().map(GLExtension::coreVersion).noneMatch(Objects::isNull)) {
            builder.append(extensions.stream().map(GLExtension::coreVersion).max(Comparator.naturalOrder()).get()).append(" or ");
            includeCoreVersion = false;
        } else {
            includeCoreVersion = true;
        }

        boolean first = true;
        for (GLExtension extension : extensions) {
            if (!first) {
                builder.append(" and ");
            }
            first = false;

            if (includeCoreVersion && extension.coreVersion() != null) {
                builder.append('(').append(extension.coreVersion()).append(" or ").append(extension.name()).append(')');
            } else {
                builder.append(extension.name());
            }
        }
        builder.append(" (current context: ").append(this).append(')');
        return builder.toString();
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

    @Override
    public final String toString() {
        return this.version + " " + this.profile + (this.forwardCompatibility ? " (forward compatibility)" : " ") + this.extensions;
    }

    //
    //
    // OpenGL 1.1
    //
    //

    /**
     * @since OpenGL 1.1
     */
    public abstract void glEnable(int cap);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDisable(int cap);

    /**
     * @since OpenGL 1.1
     */
    public abstract boolean glIsEnabled(int cap);

    /**
     * @since OpenGL 1.1
     */
    public abstract int glGetError();

    /**
     * @since OpenGL 1.1
     */
    public abstract void glFlush();

    /**
     * @since OpenGL 1.1
     */
    public abstract boolean glGetBoolean(int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glGetBoolean(int pname, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract int glGetInteger(int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glGetInteger(int pname, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract float glGetFloat(int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glGetFloat(int pname, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract double glGetDouble(int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glGetDouble(int pname, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract String glGetString(int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDrawArrays(int mode, int first, int count);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDrawElements(int mode, int count, int type, long indices);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices);

    /**
     * @since OpenGL 1.1
     */
    public abstract int glGenTexture();

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDeleteTexture(int texture);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glBindTexture(int target, int texture);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexParameter(int target, int pname, int param);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexParameter(int target, int pname, float param);

    /**
     * @since OpenGL 1.1
     */
    public abstract int glGetTexParameterInteger(int target, int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract float glGetTexParameterFloat(int target, int pname);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexImage1D(int target, int level, int internalformat, int width, int format, int type, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexImage2D(int target, int level, int internalformat, int width, int height, int format, int type, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glClear(int mask);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glClearColor(float red, float green, float blue, float alpha);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glClearDepth(double depth);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDepthFunc(int func);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glDepthMask(boolean flag);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glClearStencil(int s);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glStencilFunc(int func, int ref, int mask);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glStencilMask(int mask);

    /**
     * @since OpenGL 1.1
     */
    public abstract void glStencilOp(int sfail, int dpfail, int dppass);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_compatibility GL_ARB_compatibility}
     * @since OpenGL 1.1
     */
    @GLRequires(GLExtension.GL_ARB_compatibility)
    public abstract void glPushClientAttrib(int mask);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_compatibility GL_ARB_compatibility}
     * @since OpenGL 1.1
     */
    @GLRequires(GLExtension.GL_ARB_compatibility)
    public abstract void glPopClientAttrib();

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_compatibility GL_ARB_compatibility}
     * @since OpenGL 1.1
     */
    @GLRequires(GLExtension.GL_ARB_compatibility)
    public abstract void glPushAttrib(int mask);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_compatibility GL_ARB_compatibility}
     * @since OpenGL 1.1
     */
    @GLRequires(GLExtension.GL_ARB_compatibility)
    public abstract void glPopAttrib();

    //
    //
    // OpenGL 1.2
    //
    //

    /**
     * @since OpenGL 1.2
     */
    public abstract void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, long data);

    /**
     * @since OpenGL 1.2
     */
    public abstract void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.2
     */
    public abstract void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long data);

    /**
     * @since OpenGL 1.2
     */
    public abstract void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, @NonNull ByteBuffer data);

    //
    //
    // OpenGL 1.3
    //
    //

    /**
     * @since OpenGL 1.3
     */
    public abstract void glActiveTexture(int texture);

    //
    //
    // OpenGL 1.4
    //
    //

    /**
     * @since OpenGL 1.4
     */
    public abstract void glMultiDrawArrays(int mode, long first, long count, int drawcount);

    /**
     * @since OpenGL 1.4
     */
    public abstract void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount);

    /**
     * @since OpenGL 1.4
     */
    public abstract void glBlendColor(float red, float green, float blue, float alpha);

    /**
     * @since OpenGL 1.4
     */
    public abstract void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);

    //
    //
    // OpenGL 1.5
    //
    //

    /**
     * @since OpenGL 1.5
     */
    public abstract int glGenBuffer();

    /**
     * @since OpenGL 1.5
     */
    public abstract void glDeleteBuffer(int buffer);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glBindBuffer(int target, int buffer);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glBufferData(int target, long data_size, long data, int usage);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glBufferData(int target, @NonNull ByteBuffer data, int usage);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glBufferSubData(int target, long offset, long data_size, long data);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glBufferSubData(int target, long offset, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glGetBufferSubData(int target, long offset, long data_size, long data);

    /**
     * @since OpenGL 1.5
     */
    public abstract void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data);

    /**
     * @since OpenGL 1.5
     */
    public abstract int glGetBufferParameteri(int target, int pname);

    /**
     * @since OpenGL 1.5
     */
    public abstract long glMapBuffer(int target, int access);

    /**
     * @since OpenGL 1.5
     */
    public abstract ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer oldBuffer);

    /**
     * @since OpenGL 1.5
     */
    public abstract boolean glUnmapBuffer(int target);

    //
    //
    // OpenGL 2.0
    //
    //

    /**
     * @since OpenGL 2.0
     */
    public abstract int glCreateShader(int type);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glDeleteShader(int shader);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glShaderSource(int shader, @NonNull CharSequence... source);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glCompileShader(int shader);

    /**
     * @since OpenGL 2.0
     */
    public abstract int glGetShaderi(int shader, int pname);

    /**
     * @since OpenGL 2.0
     */
    public abstract String glGetShaderInfoLog(int shader);

    /**
     * @since OpenGL 2.0
     */
    public abstract int glCreateProgram();

    /**
     * @since OpenGL 2.0
     */
    public abstract void glDeleteProgram(int program);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glAttachShader(int program, int shader);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glDetachShader(int program, int shader);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glLinkProgram(int program);

    /**
     * @since OpenGL 2.0
     */
    public abstract int glGetProgrami(int program, int pname);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glGetProgramiv(int program, int pname, IntBuffer params);

    /**
     * @since OpenGL 2.0
     */
    public abstract int[] glGetProgramiv(int program, int pname, int count);

    /**
     * @since OpenGL 2.0
     */
    public abstract String glGetProgramInfoLog(int program);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUseProgram(int program);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glEnableVertexAttribArray(int index);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glDisableVertexAttribArray(int index);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glBindAttribLocation(int program, int index, @NonNull CharSequence name);

    /**
     * @since OpenGL 2.0
     */
    public abstract int glGetUniformLocation(int program, @NonNull CharSequence name);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, int v0);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, int v0, int v1);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, int v0, int v1, int v2);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, int v0, int v1, int v2, int v3);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, float v0);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, float v0, float v1);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, float v0, float v1, float v2);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform(int location, float v0, float v1, float v2, float v3);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform1(int location, IntBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform2(int location, IntBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform3(int location, IntBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform4(int location, IntBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform1(int location, FloatBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform2(int location, FloatBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform3(int location, FloatBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glUniform4(int location, FloatBuffer value);

    /**
     * @since OpenGL 2.0
     */
    public abstract void glBlendEquationSeparate(int modeRGB, int modeAlpha);

    //
    //
    // OpenGL 3.0
    //
    //

    /**
     * @since OpenGL 3.0
     */
    public abstract int glGetInteger(int pname, int idx);

    /**
     * @since OpenGL 3.0
     */
    public abstract String glGetString(int pname, int idx);

    /**
     * @since OpenGL 3.0
     */
    public abstract int glGenVertexArray();

    /**
     * @since OpenGL 3.0
     */
    public abstract void glDeleteVertexArray(int array);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glBindVertexArray(int array);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glBindFragDataLocation(int program, int colorNumber, @NonNull CharSequence name);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glBindBufferBase(int target, int index, int buffer);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glBindBufferRange(int target, int index, int buffer, long offset, long size);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glBeginTransformFeedback(int primitiveMode);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glEndTransformFeedback();

    /**
     * @since OpenGL 3.0
     */
    public abstract void glTransformFeedbackVaryings(int program, @NonNull CharSequence[] varyings, int bufferMode);

    /**
     * @since OpenGL 3.0
     */
    public abstract long glMapBufferRange(int target, long offset, long length, int access);

    /**
     * @since OpenGL 3.0
     */
    public abstract ByteBuffer glMapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer);

    /**
     * @since OpenGL 3.0
     */
    public abstract void glFlushMappedBufferRange(int target, long offset, long length);

    //
    //
    // OpenGL 3.1
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_copy_buffer GL_ARB_copy_buffer}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_copy_buffer)
    public abstract void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_draw_instanced GL_ARB_draw_instanced}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_draw_instanced)
    public abstract void glDrawArraysInstanced(int mode, int first, int count, int instancecount);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_draw_instanced GL_ARB_draw_instanced}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_draw_instanced)
    public abstract void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_texture_buffer_object GL_ARB_texture_buffer_object}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_texture_buffer_object)
    public abstract void glTexBuffer(int target, int internalFormat, int buffer);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_uniform_buffer_object GL_ARB_uniform_buffer_object}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    public abstract int glGetUniformBlockIndex(int program, @NonNull CharSequence uniformBlockName);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_uniform_buffer_object GL_ARB_uniform_buffer_object}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    public abstract void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_uniform_buffer_object GL_ARB_uniform_buffer_object}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    public abstract int[] glGetUniformIndices(int program, CharSequence[] uniformNames);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_uniform_buffer_object GL_ARB_uniform_buffer_object}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    public final int glGetUniformIndices(int program, CharSequence uniformName) {
        return this.glGetUniformIndices(program, new CharSequence[]{ uniformName })[0];
    }

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_uniform_buffer_object GL_ARB_uniform_buffer_object}
     * @since OpenGL 3.1
     */
    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    public abstract int glGetActiveUniformsi(int program, int uniformIndex, int pname);

    //
    //
    // OpenGL 3.2
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_draw_elements_base_vertex GL_ARB_draw_elements_base_vertex}
     * @since OpenGL 3.2
     */
    @GLRequires(GLExtension.GL_ARB_draw_elements_base_vertex)
    public abstract void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_draw_elements_base_vertex GL_ARB_draw_elements_base_vertex}
     * @since OpenGL 3.2
     */
    @GLRequires(GLExtension.GL_ARB_draw_elements_base_vertex)
    public abstract void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sync GL_ARB_sync}
     * @since OpenGL 3.2
     */
    @GLRequires(GLExtension.GL_ARB_sync)
    public abstract long glFenceSync(int condition, int flags);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sync GL_ARB_sync}
     * @since OpenGL 3.2
     */
    @GLRequires(GLExtension.GL_ARB_sync)
    public abstract int glClientWaitSync(long sync, int flags, long timeout);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sync GL_ARB_sync}
     * @since OpenGL 3.2
     */
    @GLRequires(GLExtension.GL_ARB_sync)
    public abstract int glGetSync(long sync, int pname);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sync GL_ARB_sync}
     * @since OpenGL 3.2
     */
    @GLRequires(GLExtension.GL_ARB_sync)
    public abstract void glDeleteSync(long sync);

    //
    //
    // OpenGL 3.3
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_instanced_arrays GL_ARB_instanced_arrays}
     * @since OpenGL 3.3
     */
    @GLRequires(GLExtension.GL_ARB_instanced_arrays)
    public abstract void glVertexAttribDivisor(int index, int divisor);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sampler_objects GL_ARB_sampler_objects}
     * @since OpenGL 3.3
     */
    @GLRequires(GLExtension.GL_ARB_sampler_objects)
    public abstract int glGenSampler();

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sampler_objects GL_ARB_sampler_objects}
     * @since OpenGL 3.3
     */
    @GLRequires(GLExtension.GL_ARB_sampler_objects)
    public abstract void glDeleteSampler(int sampler);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sampler_objects GL_ARB_sampler_objects}
     * @since OpenGL 3.3
     */
    @GLRequires(GLExtension.GL_ARB_sampler_objects)
    public abstract void glBindSampler(int unit, int sampler);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sampler_objects GL_ARB_sampler_objects}
     * @since OpenGL 3.3
     */
    @GLRequires(GLExtension.GL_ARB_sampler_objects)
    public abstract void glSamplerParameter(int sampler, int pname, int param);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sampler_objects GL_ARB_sampler_objects}
     * @since OpenGL 3.3
     */
    @GLRequires(GLExtension.GL_ARB_sampler_objects)
    public abstract void glSamplerParameter(int sampler, int pname, float param);

    //
    //
    // OpenGL 4.1
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, int v0);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, int v0, int v1);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, int v0, int v1, int v2);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, int v0, int v1, int v2, int v3);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, float v0);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, float v0, float v1);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, float v0, float v1, float v2);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform(int program, int location, float v0, float v1, float v2, float v3);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform1(int program, int location, IntBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform2(int program, int location, IntBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform3(int program, int location, IntBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform4(int program, int location, IntBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform1(int program, int location, FloatBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform2(int program, int location, FloatBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform3(int program, int location, FloatBuffer value);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_separate_shader_objects GL_ARB_separate_shader_objects}
     * @since OpenGL 4.1
     */
    @GLRequires(GLExtension.GL_ARB_separate_shader_objects)
    public abstract void glProgramUniform4(int program, int location, FloatBuffer value);

    //
    //
    // OpenGL 4.2
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_base_instance GL_ARB_base_instance}
     * @since OpenGL 4.2
     */
    @GLRequires(GLExtension.GL_ARB_base_instance)
    public abstract void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_base_instance GL_ARB_base_instance}
     * @since OpenGL 4.2
     */
    @GLRequires(GLExtension.GL_ARB_base_instance)
    public abstract void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices, int instancecount, int basevertex, int baseinstance);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_shader_image_load_store GL_ARB_shader_image_load_store}
     * @since OpenGL 4.2
     */
    @GLRequires(GLExtension.GL_ARB_shader_image_load_store)
    public abstract void glMemoryBarrier(int barriers);

    //
    //
    // OpenGL 4.3
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_compute_shader GL_ARB_compute_shader}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_ARB_compute_shader)
    public abstract void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_multi_draw_indirect GL_ARB_multi_draw_indirect}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_ARB_multi_draw_indirect)
    public abstract void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_multi_draw_indirect GL_ARB_multi_draw_indirect}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_ARB_multi_draw_indirect)
    public abstract void glMultiDrawElementsIndirect(int mode, int type, long indirect, int primcount, int stride);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_program_interface_query GL_ARB_program_interface_query}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_ARB_program_interface_query)
    public abstract int glGetProgramResourceIndex(int program, int programInterface, @NonNull CharSequence name);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_shader_storage_buffer_object GL_ARB_shader_storage_buffer_object}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_ARB_shader_storage_buffer_object)
    public abstract void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract void glObjectLabel(int identifier, int name, @NonNull CharSequence label);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract void glObjectPtrLabel(long ptr, @NonNull CharSequence label);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract String glGetObjectLabel(int identifier, int name);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract String glGetObjectPtrLabel(long ptr);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract void glDebugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract void glDebugMessageControl(int source, int type, int severity, int[] ids, boolean enabled);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract void glDebugMessageInsert(int source, int type, int id, int severity, @NonNull CharSequence msg);

    /**
     * @apiNote requires {@link GLExtension#GL_KHR_debug GL_KHR_debug}
     * @since OpenGL 4.3
     */
    @GLRequires(GLExtension.GL_KHR_debug)
    public abstract void glDebugMessageCallback(GLDebugOutputCallback callback);

    //
    //
    // OpenGL 4.4
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_buffer_storage GL_ARB_buffer_storage}
     * @since OpenGL 4.4
     */
    @GLRequires(GLExtension.GL_ARB_buffer_storage)
    public abstract void glBufferStorage(int target, long data_size, long data, int flags);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_buffer_storage GL_ARB_buffer_storage}
     * @since OpenGL 4.4
     */
    @GLRequires(GLExtension.GL_ARB_buffer_storage)
    public abstract void glBufferStorage(int target, @NonNull ByteBuffer data, int flags);

    //
    //
    // OpenGL 4.5
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract int glCreateBuffer();

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glNamedBufferData(int buffer, long data_size, long data, int usage);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glNamedBufferData(int buffer, @NonNull ByteBuffer data, int usage);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_buffer_storage GL_ARB_buffer_storage}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_buffer_storage)
    public abstract void glNamedBufferStorage(int buffer, long data_size, long data, int flags);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_buffer_storage GL_ARB_buffer_storage}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_buffer_storage)
    public abstract void glNamedBufferStorage(int buffer, @NonNull ByteBuffer data, int flags);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glNamedBufferSubData(int buffer, long offset, long data_size, long data);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glGetNamedBufferSubData(int buffer, long offset, long data_size, long data);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glGetNamedBufferSubData(int buffer, long offset, @NonNull ByteBuffer data);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract int glGetNamedBufferParameteri(int buffer, int pname);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract long glMapNamedBuffer(int buffer, int access);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract ByteBuffer glMapNamedBuffer(int buffer, int access, long length, ByteBuffer oldBuffer);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract long glMapNamedBufferRange(int buffer, long offset, long size, int access);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract ByteBuffer glMapNamedBufferRange(int buffer, long offset, long size, int access, ByteBuffer oldBuffer);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glFlushMappedNamedBufferRange(int buffer, long offset, long length);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract boolean glUnmapNamedBuffer(int buffer);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract int glCreateVertexArray();

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayElementBuffer(int vaobj, int buffer);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glEnableVertexArrayAttrib(int vaobj, int index);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glDisableVertexArrayAttrib(int vaobj, int index);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized, int relativeoffset);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayVertexBuffers(int vaobj, int first, int count, int[] buffers, long[] offsets, int[] strides);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access}
     * @since OpenGL 4.5
     */
    @GLRequires(GLExtension.GL_ARB_direct_state_access)
    public abstract void glVertexArrayVertexBuffers(int vaobj, int first, int count, long buffers, long offsets, long strides);

    //
    //
    // No OpenGL version
    //
    //

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_debug_output GL_ARB_debug_output}
     */
    @GLRequires(GLExtension.GL_ARB_debug_output)
    public abstract void glDebugMessageControlARB(int source, int type, int severity, IntBuffer ids, boolean enabled);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_debug_output GL_ARB_debug_output}
     */
    @GLRequires(GLExtension.GL_ARB_debug_output)
    public abstract void glDebugMessageControlARB(int source, int type, int severity, int[] ids, boolean enabled);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_debug_output GL_ARB_debug_output}
     */
    @GLRequires(GLExtension.GL_ARB_debug_output)
    public abstract void glDebugMessageInsertARB(int source, int type, int id, int severity, @NonNull CharSequence msg);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_debug_output GL_ARB_debug_output}
     */
    @GLRequires(GLExtension.GL_ARB_debug_output)
    public abstract void glDebugMessageCallbackARB(GLDebugOutputCallback callback);

    /**
     * @apiNote requires {@link GLExtension#GL_ARB_sparse_buffer GL_ARB_sparse_buffer}
     */
    @GLRequires(GLExtension.GL_ARB_sparse_buffer)
    public abstract void glBufferPageCommitmentARB(int target, long offset, long size, boolean commit);

    /**
     * @apiNote requires both {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access} and {@link GLExtension#GL_ARB_sparse_buffer GL_ARB_sparse_buffer}
     */
    @GLRequires({ GLExtension.GL_ARB_direct_state_access, GLExtension.GL_ARB_sparse_buffer })
    public abstract void glNamedBufferPageCommitmentARB(int buffer, long offset, long size, boolean commit);

    /**
     * Stores the upper limits for various features supported by an OpenGL context.
     *
     * @author DaPorkchop_
     */
    @Getter
    public static final class Limits {
        private final int maxFragmentColors;
        private final int maxTextureUnits;
        private final int maxVertexAttributes;

        @GLRequires(GLExtension.GL_ARB_shader_storage_buffer_object)
        private final int maxShaderStorageBuffers;

        @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
        private final int maxUniformBuffers;
        @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
        private final int maxUniformBlockSize;

        @GLRequires(GLExtension.GL_ARB_compute_shader)
        private final int maxComputeWorkGroupInvocations;
        @GLRequires(GLExtension.GL_ARB_compute_shader)
        private final ComputeWorkGroupSize maxComputeWorkGroupSize;
        @GLRequires(GLExtension.GL_ARB_compute_shader)
        private final ComputeWorkGroupCount maxComputeWorkGroupCount;

        @GLRequires(GLExtension.GL_KHR_debug)
        private final int maxLabelLength;

        @GLRequires(GLExtension.GL_ARB_sparse_buffer)
        private final int sparseBufferPageSizeARB;

        Limits(OpenGL gl) {
            this.maxFragmentColors = gl.glGetInteger(GL_MAX_DRAW_BUFFERS);
            this.maxTextureUnits = gl.glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
            this.maxVertexAttributes = gl.glGetInteger(GL_MAX_VERTEX_ATTRIBS);

            this.maxShaderStorageBuffers = gl.supports(GLExtension.GL_ARB_shader_storage_buffer_object) ? gl.glGetInteger(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0;

            if (gl.supports(GLExtension.GL_ARB_uniform_buffer_object)) {
                this.maxUniformBuffers = gl.glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);
                this.maxUniformBlockSize = gl.glGetInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
            } else {
                this.maxUniformBuffers = this.maxUniformBlockSize = 0;
            }

            if (gl.supports(GLExtension.GL_ARB_compute_shader)) {
                this.maxComputeWorkGroupInvocations = gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
                this.maxComputeWorkGroupSize = new ComputeWorkGroupSize(gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0), gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1), gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2));
                this.maxComputeWorkGroupCount = new ComputeWorkGroupCount(gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0), gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1), gl.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2));
            } else {
                this.maxComputeWorkGroupInvocations = 0;
                this.maxComputeWorkGroupSize = null;
                this.maxComputeWorkGroupCount = null;
            }

            this.maxLabelLength = gl.supports(GLExtension.GL_KHR_debug) ? gl.glGetInteger(GL_MAX_LABEL_LENGTH) : 0;

            this.sparseBufferPageSizeARB = gl.supports(GLExtension.GL_ARB_sparse_buffer) ? gl.glGetInteger(GL_SPARSE_BUFFER_PAGE_SIZE_ARB) : 0;
        }
    }
}
