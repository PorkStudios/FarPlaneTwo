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

package net.daporkchop.fp2.client.gl.shader;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.lib.unsafe.PCleaner;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL20.*;

/**
 * Container for an actual vertex/geometry/fragment shader.
 *
 * @author DaPorkchop_
 */
@Getter
class Shader {
    protected final ShaderType type;
    protected final int id;

    protected Shader(@NonNull ShaderType type, @NonNull Identifier name/*, @NonNull SourceLine... lines*/) {
        this.type = type;

        //allocate shader
        this.id = glCreateShader(type.id);

        //allow garbage-collection of shader
        int id = this.id;
        PCleaner.cleaner(this, () -> Minecraft.getMinecraft().addScheduledTask(() -> glDeleteShader(id)));

        //set shader source code
        //glShaderSource(id, Stream.of(lines).map(SourceLine::text).collect(Collectors.joining("\n")));

        //compile and validate shader
        glCompileShader(id);
        //ShaderManager.validateShaderCompile(this.id, name, lines);
    }
}
