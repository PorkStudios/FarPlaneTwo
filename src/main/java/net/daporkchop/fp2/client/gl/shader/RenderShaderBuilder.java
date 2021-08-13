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
public final class RenderShaderBuilder extends ShaderManager.AbstractShaderBuilder<RenderShaderBuilder, RenderShaderProgram> {
    @NonNull
    @With
    protected final String programName;
    protected final Map<String, Object> defines;

    @SneakyThrows(IOException.class)
    protected ProgramMeta meta() {
        ProgramMeta meta;
        try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", ShaderManager.BASE_PATH, this.programName))) {
            checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", ShaderManager.BASE_PATH, this.programName);
            meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
        }

        checkState(meta.vert != null, "Program \"%s\" has no vertex shaders!", this.programName);
        if (meta.xfb_varying != null) {
            checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", this.programName);
        } else {
            checkState(meta.frag != null, "Program \"%s\" has no fragment shaders!", this.programName);
        }

        return meta;
    }

    @Override
    protected RenderShaderProgram supply() {
        ProgramMeta meta = this.meta();

        return new RenderShaderProgram(
                this.programName,
                meta.vert != null ? ShaderManager.get(meta.vert, ShaderManager.headers(this.defines), ShaderType.VERTEX) : null,
                meta.geom != null ? ShaderManager.get(meta.geom, ShaderManager.headers(this.defines), ShaderType.GEOMETRY) : null,
                meta.frag != null ? ShaderManager.get(meta.frag, ShaderManager.headers(this.defines), ShaderType.FRAGMENT) : null,
                null,
                meta.xfb_varying);
    }

    @Override
    protected void reload(@NonNull RenderShaderProgram program) {
        ProgramMeta meta = this.meta();

        program.reload(
                meta.vert != null ? ShaderManager.get(meta.vert, ShaderManager.headers(this.defines), ShaderType.VERTEX) : null,
                meta.geom != null ? ShaderManager.get(meta.geom, ShaderManager.headers(this.defines), ShaderType.GEOMETRY) : null,
                meta.frag != null ? ShaderManager.get(meta.frag, ShaderManager.headers(this.defines), ShaderType.FRAGMENT) : null,
                null,
                meta.xfb_varying);
    }

    /**
     * Metadata for a shader program.
     *
     * @author DaPorkchop_
     */
    private static final class ProgramMeta {
        public String[] vert;
        public String[] geom;
        public String[] frag;

        public String[] xfb_varying;
    }
}
