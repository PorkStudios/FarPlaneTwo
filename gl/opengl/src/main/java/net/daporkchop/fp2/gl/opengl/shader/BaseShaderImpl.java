/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.gl.opengl.shader;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.shader.source.SourceLine;
import net.daporkchop.fp2.gl.shader.BaseShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseShaderImpl<L extends BaseLayout> implements BaseShader<L> {
    protected final OpenGL gl;

    protected final L layout;

    protected final int type;
    protected final int id;

    public BaseShaderImpl(@NonNull ShaderBuilderImpl<?, L> builder, int type, @NonNull SourceLine... lines) throws ShaderCompilationException {
        //allocate new shader
        this.gl = builder.gl;
        this.type = type;

        this.layout = builder.layout;

        GLAPI api = this.gl.api();
        this.id = api.glCreateShader(type);

        //set source and compile shader
        api.glShaderSource(this.id, Stream.of(lines).map(SourceLine::text).collect(Collectors.joining("\n")));
        api.glCompileShader(this.id);

        //check for errors
        if (api.glGetShaderi(this.id, GL_COMPILE_STATUS) == GL_FALSE) {
            //get and format shader log
            String log = api.glGetShaderInfoLog(this.id);
            String formattedLog = this.formatInfoLog(log, lines);

            //delete shader
            api.glDeleteShader(this.id);

            throw new ShaderCompilationException(formattedLog);
        }

        //register resource for cleaning
        this.gl.resourceArena().register(this, this.id, api::glDeleteShader);
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    protected String formatInfoLog(@NonNull String text, @NonNull SourceLine... lines) {
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

        return text;
    }
}
