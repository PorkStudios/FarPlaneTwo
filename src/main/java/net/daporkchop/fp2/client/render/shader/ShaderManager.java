/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.client.render.shader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.render.OpenGL;
import net.daporkchop.lib.binary.oio.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages loaded shaders.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderManager {
    protected final String BASE_PATH = "/assets/misc/shaders";
    protected long RELOAD_COUNTER = 0L;

    /**
     * Obtains a shader program with the given name.
     *
     * @param programName the name of the shader to get
     * @return the shader program with the given name
     */
    public ShaderProgram get(@NonNull String programName) {
        OpenGL.assertOpenGL();
        try {
            String fileName = String.format("%s/prog/%s.json", BASE_PATH, programName);
            JsonObject meta;
            try (InputStream in = ShaderManager.class.getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new IllegalStateException(String.format("Unable to find shader meta file: \"%s\"!", fileName));
                }
                meta = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
            }
            if (!meta.has("vert")) {
                throw new IllegalStateException(String.format("Shader \"%s\" has no vertex shader!", programName));
            } else if (!meta.has("frag")) {
                throw new IllegalStateException(String.format("Shader \"%s\" has no fragment shader!", programName));
            }
            return new ShaderProgram(
                    programName,
                    get(meta.get("vert").getAsString(), ShaderType.VERTEX),
                    get(meta.get("frag").getAsString(), ShaderType.FRAGMENT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Shader get(@NonNull String name, @NonNull ShaderType type) {
        OpenGL.assertOpenGL();

        try {
            String fileName = String.format("%s/%s/%s.%s", BASE_PATH, type.extension, name, type.extension);
            String metaFileName = fileName + ".json";
            JsonObject meta;
            try (InputStream in = ShaderManager.class.getResourceAsStream(metaFileName)) {
                if (in == null) {
                    throw new IllegalStateException(String.format("Unable to find meta file: \"%s\"!", metaFileName));
                }
                meta = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
            }
            String code;
            try (InputStream in = ShaderManager.class.getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new IllegalStateException(String.format("Unable to find shader file: \"%s\"!", fileName));
                }
                code = new String(StreamUtil.toByteArray(in), StandardCharsets.UTF_8);
            }
            return type.construct(name, code, meta);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void validate(@NonNull String name, int id, int type) {
        if (OpenGL.glGetShaderi(id, type) == OpenGL.GL_FALSE) {
            String error = String.format("Couldn't compile shader \"%s\": %s", name, OpenGL.glGetLogInfo(id));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }
}
