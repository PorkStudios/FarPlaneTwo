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

package net.daporkchop.fp2.client.gl.shader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderer;
import net.daporkchop.lib.binary.oio.StreamUtil;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.misc.string.PStrings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Manages loaded shaders.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderManager {
    protected final String BASE_PATH = "/assets/fp2/shaders";

    /**
     * Obtains a shader program with the given name.
     *
     * @param programName the name of the shader to get
     * @return the shader program with the given name
     */
    public ShaderProgram get(@NonNull String programName) {
        try {
            String fileName = PStrings.fastFormat("%s/prog/%s.json", BASE_PATH, programName);
            JsonObject meta;
            try (InputStream in = ShaderManager.class.getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new IllegalStateException(PStrings.fastFormat("Unable to find shader meta file: \"%s\"!", fileName));
                }
                meta = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
            }
            if (!meta.has("vert")) {
                throw new IllegalStateException(PStrings.fastFormat("Shader \"%s\" has no vertex shader!", programName));
            } else if (!meta.has("frag")) {
                throw new IllegalStateException(PStrings.fastFormat("Shader \"%s\" has no fragment shader!", programName));
            }
            return new ShaderProgram(
                    programName,
                    get(StreamSupport.stream(meta.getAsJsonArray("vert").spliterator(), false).map(JsonElement::getAsString).toArray(String[]::new), ShaderType.VERTEX),
                    meta.has("geom") ? get(StreamSupport.stream(meta.getAsJsonArray("vert").spliterator(), false).map(JsonElement::getAsString).toArray(String[]::new), ShaderType.VERTEX) : null,
                    get(StreamSupport.stream(meta.getAsJsonArray("frag").spliterator(), false).map(JsonElement::getAsString).toArray(String[]::new), ShaderType.FRAGMENT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Shader get(@NonNull String[] names, @NonNull ShaderType type) {
        return type.construct(names, Arrays.stream(names)
                .map((IOFunction<String, String>) fileName -> {
                    try (InputStream in = ShaderManager.class.getResourceAsStream(BASE_PATH + '/' + fileName)) {
                        checkState(in != null, "Unable to find shader file: \"%s\"!", fileName);
                        return new String(StreamUtil.toByteArray(in), StandardCharsets.UTF_8);
                    }
                })
                .toArray(String[]::new));
    }

    protected void validateShaderCompile(@NonNull String name, int id) {
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            int size = glGetShaderi(id, GL_INFO_LOG_LENGTH);
            String error = PStrings.fastFormat("Couldn't compile shader \"%s\":\n%s", name, glGetShaderInfoLog(id, size));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }

    protected void validateProgramLink(@NonNull String name, int id) {
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            int size = glGetProgrami(id, GL_INFO_LOG_LENGTH);
            String error = PStrings.fastFormat("Couldn't compile shader \"%s\":\n%s", name, glGetProgramInfoLog(id, size));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }

    protected void validateProgramValidate(@NonNull String name, int id) {
        if (glGetProgrami(id, GL_VALIDATE_STATUS) == GL_FALSE) {
            int size = glGetProgrami(id, GL_INFO_LOG_LENGTH);
            String error = PStrings.fastFormat("Couldn't compile shader \"%s\":\n%s", name, glGetProgramInfoLog(id, size));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }

    public void reload() {
        //TODO: actually reload all shaders rather than doing it manually
        HeightmapRenderer.reloadHeightShader(true);
    }
}
