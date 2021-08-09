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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import net.daporkchop.fp2.client.gl.WorkGroupSize;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.common.misc.string.PStrings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class ComputeShaderBuilder extends ShaderManager.AbstractShaderBuilder {
    @NonNull
    @With
    protected final String programName;
    protected final Map<String, String> defines;

    @With
    protected final WorkGroupSize workGroupSize;

    protected String headers() {
        return ShaderManager.headers(this.defines) + PStrings.fastFormat(
                "layout(local_size_x = %d, local_size_y = %d, local_size_z = %d) in;\n",
                this.workGroupSize.x(), this.workGroupSize.y(), this.workGroupSize.z());
    }

    @Override
    public ShaderProgram link() {
        checkState(this.workGroupSize != null, "workGroupSize must be set!");
        return super.link();
    }

    @Override
    @SneakyThrows(IOException.class)
    protected ShaderProgram supply() {
        String[] names;
        try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", ShaderManager.BASE_PATH, this.programName))) {
            checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", ShaderManager.BASE_PATH, this.programName);
            names = GSON.fromJson(new UTF8FileReader(in), String[].class);
        }

        checkState(names != null, "Program \"%s\" has no shaders!", this.programName);

        return new ShaderProgram(
                this.programName,
                null, null, null,
                ShaderManager.get(names, this.headers(), ShaderType.COMPUTE),
                null);
    }

    @Override
    @SneakyThrows(IOException.class)
    protected void reload(@NonNull ShaderProgram program) {
        String[] names;
        try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", ShaderManager.BASE_PATH, this.programName))) {
            checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", ShaderManager.BASE_PATH, this.programName);
            names = GSON.fromJson(new UTF8FileReader(in), String[].class);
        }

        checkState(names != null, "Program \"%s\" has no shaders!", this.programName);

        program.reload(
                null, null, null,
                ShaderManager.get(names, this.headers(), ShaderType.COMPUTE),
                null);
    }
}
