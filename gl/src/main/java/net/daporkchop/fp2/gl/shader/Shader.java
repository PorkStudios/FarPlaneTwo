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

package net.daporkchop.fp2.gl.shader;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.source.Preprocessor;
import net.daporkchop.fp2.gl.shader.source.SourceLine;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Base class representing an unlinked OpenGL shader object.
 *
 * @author DaPorkchop_
 */
@Getter
public final class Shader implements AutoCloseable {
    private final OpenGL gl;
    private final ShaderType type;
    private final int id;

    public Shader(OpenGL gl, ShaderType type, ResourceProvider provider, Identifier sourceFile) throws ShaderCompilationException {
        this(gl, type, new Preprocessor(provider).appendLines(sourceFile).preprocess().lines());
    }

    public Shader(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull SourceLine... lines) throws ShaderCompilationException {
        //allocate new shader
        this.gl = gl;
        this.type = type;

        this.id = gl.glCreateShader(type.id());

        try {
            //set source and compile shader
            gl.glShaderSource(this.id, Stream.of(lines).map(SourceLine::text).collect(Collectors.joining("\n")));
            gl.glCompileShader(this.id);

            //check for errors
            if (gl.glGetShaderi(this.id, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new ShaderCompilationException(formatInfoLog(gl.glGetShaderInfoLog(this.id), lines));
            }
        } catch (Throwable t) { //clean up if something goes wrong
            gl.glDeleteShader(this.id);
            throw PUnsafe.throwException(t);
        }
    }

    @Override
    public void close() {
        this.gl.glDeleteShader(this.id);
    }

    //TODO: make this private
    public static String formatInfoLog(@NonNull String text, @NonNull SourceLine... lines) {
        try {
            for (Pattern pattern : new Pattern[]{ //different patterns for various error formats i've encountered so far
                    Pattern.compile("^(?<file>\\d+)\\((?<line>\\d+)\\) (?<text>: .+)", Pattern.MULTILINE),
                    Pattern.compile("^(?<file>\\d+):(?<line>\\d+)\\((?<row>\\d+)\\)(?<text>: .+)", Pattern.MULTILINE),
            }) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    StringBuffer buffer = new StringBuffer();
                    do {
                        SourceLine line = lines[Integer.parseInt(matcher.group("line")) - 1];
                        matcher.appendReplacement(buffer, Matcher.quoteReplacement("(" + line.location() + ':' + line.lineNumber() + ')' + matcher.group("text")));
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
}
