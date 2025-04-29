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

package net.daporkchop.fp2.gl.shader;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.source.IncludePreprocessor;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.closeable.QuietCloseable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base class representing an unlinked OpenGL shader object.
 *
 * @author DaPorkchop_
 */
@Getter
public final class Shader extends GLObject.Normal {
    public static Shader compile(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull ResourceProvider resourceProvider, @NonNull Identifier path) throws ShaderCompilationException {
        return compile(gl, type, new IncludePreprocessor(gl, resourceProvider).addVersionHeader().include(path));
    }

    public static Shader compile(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull IncludePreprocessor source) throws ShaderCompilationException {
        return compile(gl, type, source.buffer(), source.locations());
    }

    public static Shader compile(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull CharSequence source) throws ShaderCompilationException {
        return compile(gl, type, source, null);
    }

    private static Shader compile(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull CharSequence source, List<Identifier> sourceLocations) throws ShaderCompilationException {
        try (CompileTask task = new CompileTask(gl, type, source, sourceLocations)) {
            return task.join();
        }
    }

    public static CompileTask compileAsync(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull ResourceProvider resourceProvider, @NonNull Identifier path) {
        return compileAsync(gl, type, new IncludePreprocessor(gl, resourceProvider).addVersionHeader().include(path));
    }

    public static CompileTask compileAsync(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull IncludePreprocessor source) {
        return compileAsync(gl, type, source.buffer(), source.locations());
    }

    public static CompileTask compileAsync(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull CharSequence source) {
        return compileAsync(gl, type, source, null);
    }

    private static CompileTask compileAsync(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull CharSequence source, List<Identifier> sourceLocations) {
        return new CompileTask(gl, type, source, sourceLocations);
    }

    private final ShaderType type;

    Shader(@NonNull OpenGL gl, @NonNull ShaderType type) {
        super(gl, gl.glCreateShader(type.id()));
        this.type = type;
    }

    @Override
    protected void delete() {
        this.gl.glDeleteShader(this.id);
    }

    @Override
    protected int debugLabelNamespace() {
        return GL_SHADER;
    }

    static String formatInfoLog(@NonNull String text, List<Identifier> sourceLocations) {
        if (sourceLocations == null) {
            return text;
        }

        try {
            for (Pattern pattern : new Pattern[]{ //different patterns for various error formats i've encountered so far
                    Pattern.compile("^(?<file>\\d+)\\((?<line>\\d+)\\) (?<text>: .+)", Pattern.MULTILINE),
                    Pattern.compile("^(?<file>\\d+):(?<line>\\d+)\\((?<row>\\d+)\\)(?<text>: .+)", Pattern.MULTILINE),
            }) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    StringBuffer buffer = new StringBuffer();
                    do {
                        int fileNumber = Integer.parseInt(matcher.group("file"));
                        int lineNumber = Integer.parseInt(matcher.group("line"));
                        matcher.appendReplacement(buffer, Matcher.quoteReplacement("(" + sourceLocations.get(fileNumber) + ':' + lineNumber + ')' + matcher.group("text")));
                    } while (matcher.find());
                    matcher.appendTail(buffer);

                    text = buffer.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return text;
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class CompileTask implements QuietCloseable {
        final OpenGL gl;
        final List<Identifier> sourceLocations;
        Shader shader;

        CompileTask(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull CharSequence source, List<Identifier> sourceLocations) {
            try {
                this.gl = gl;
                this.sourceLocations = sourceLocations;

                this.shader = new Shader(gl, type);

                //set source and compile shader
                gl.glShaderSource(this.shader.id(), source);
                gl.glCompileShader(this.shader.id());
            } catch (Throwable t) { //clean up if something goes wrong
                throw PResourceUtil.closeSuppressed(t, this);
            }
        }

        @Override
        public void close() {
            PResourceUtil.close(this.shader);
        }

        /**
         * Checks if this shader compilation task has been completed yet.
         *
         * @return {@code true} if this shader has finished compilation, {@code false} otherwise
         * @throws UnsupportedOperationException if neither {@link GLExtension#GL_ARB_parallel_shader_compile GL_ARB_parallel_shader_compile} nor {@link GLExtension#GL_KHR_parallel_shader_compile GL_KHR_parallel_shader_compile} is supported
         * @apiNote requires either {@link GLExtension#GL_ARB_parallel_shader_compile GL_ARB_parallel_shader_compile} or {@link GLExtension#GL_KHR_parallel_shader_compile GL_KHR_parallel_shader_compile}
         */
        public boolean isComplete() {
            checkState(this.shader != null, "already finished!");

            boolean arb = this.gl.supports(GLExtension.GL_ARB_parallel_shader_compile);
            boolean khr = this.gl.supports(GLExtension.GL_KHR_parallel_shader_compile);
            if (arb || khr) {
                int pname = arb ? GL_COMPLETION_STATUS_ARB : GL_COMPLETION_STATUS_KHR;
                return this.gl.glGetShaderi(this.shader.id(), pname) == GL_TRUE;
            } else {
                throw new UnsupportedOperationException("neither " + GLExtension.GL_ARB_parallel_shader_compile + " nor " + GLExtension.GL_KHR_parallel_shader_compile + " are supported! " + this.gl);
            }
        }

        /**
         * Waits for the shader to finish compiling and returns the compiled shader.
         * <p>
         * When this function returns successfully, ownership of the returned {@link Shader} instance is transferred to the caller.
         *
         * @return the compiled {@link Shader} instance
         * @throws ShaderCompilationException if the shader compilation failed
         */
        public Shader join() throws ShaderCompilationException {
            checkState(this.shader != null, "already finished!");

            //check for errors!
            //  this will block if the shader is being compiled asynchronously
            if (this.gl.glGetShaderi(this.shader.id(), GL_COMPILE_STATUS) == GL_FALSE) {
                throw new ShaderCompilationException(formatInfoLog(this.gl.glGetShaderInfoLog(this.shader.id()), this.sourceLocations));
            }

            //move ownership of the Shader instance to the caller
            Shader shader = this.shader;
            this.shader = null; //make close() a no-op
            return shader;
        }
    }
}
