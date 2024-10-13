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
import net.daporkchop.fp2.gl.shader.source.IncludePreprocessor;
import net.daporkchop.fp2.gl.shader.source.SourceLocation;
import net.daporkchop.fp2.gl.util.GLObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Base class representing an unlinked OpenGL shader object.
 *
 * @author DaPorkchop_
 */
@Getter
public final class Shader extends GLObject.Normal {
    private final ShaderType type;

    public Shader(OpenGL gl, ShaderType type, ResourceProvider provider, Identifier sourceFile) throws ShaderCompilationException {
        this(gl, type, new IncludePreprocessor(provider).addVersionHeader(gl).include(sourceFile));
    }

    public Shader(OpenGL gl, ShaderType type, IncludePreprocessor source) throws ShaderCompilationException {
        super(gl, gl.glCreateShader(type.id()));
        this.type = type;

        try {
            //set source and compile shader
            gl.glShaderSource(this.id, source.buffer());
            gl.glCompileShader(this.id);

            //check for errors
            if (gl.glGetShaderi(this.id, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new ShaderCompilationException(formatInfoLog(gl.glGetShaderInfoLog(this.id), source.locations()));
            }
        } catch (Throwable t) { //clean up if something goes wrong
            gl.glDeleteShader(this.id);
            throw t;
        }
    }

    @Override
    protected void delete() {
        this.gl.glDeleteShader(this.id);
    }

    @Override
    public void setDebugLabel(@NonNull CharSequence label) {
        this.gl.glObjectLabel(GL_SHADER, this.id, label);
    }

    @Override
    public String getDebugLabel() {
        return this.gl.glGetObjectLabel(GL_SHADER, this.id);
    }

    //TODO: make this private
    public static String formatInfoLog(String text, List<SourceLocation> locations) {
        try {
            for (Pattern pattern : new Pattern[]{ //different patterns for various error formats i've encountered so far
                    Pattern.compile("^(?<file>\\d+)\\((?<line>\\d+)\\) (?<text>: .+)", Pattern.MULTILINE),
                    Pattern.compile("^(?<file>\\d+):(?<line>\\d+)\\((?<row>\\d+)\\)(?<text>: .+)", Pattern.MULTILINE),
            }) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    StringBuffer buffer = new StringBuffer();
                    do {
                        SourceLocation location = locations.get(Integer.parseInt(matcher.group("line")) - 1);
                        matcher.appendReplacement(buffer, Matcher.quoteReplacement("(" + location.location() + ':' + location.lineNumber() + ')' + matcher.group("text")));
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
