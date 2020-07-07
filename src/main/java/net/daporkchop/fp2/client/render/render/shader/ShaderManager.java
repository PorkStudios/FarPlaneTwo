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

package net.daporkchop.fp2.client.render.render.shader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.binary.oio.StreamUtil;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.fp2.client.render.render.OpenGL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages loaded shaders.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderManager {
    protected final String                     BASE_PATH       = "/assets/misc/shaders";
    protected final Map<String, ShaderProgram> LINKED_PROGRAMS = new ConcurrentHashMap<>();
    protected       long                       RELOAD_COUNTER  = 0L;

    /**
     * Obtains a shader program with the given name.
     *
     * @param programName the name of the shader to get
     * @return the shader program with the given name
     */
    public ShaderProgram get(@NonNull String programName) {
        OpenGL.assertOpenGL();
        ShaderProgram program = LINKED_PROGRAMS.computeIfAbsent(programName, (IOFunction<String, ShaderProgram>) ShaderManager::doGet);
        program.usages++;
        return program;
    }

    private ShaderProgram doGet(@NonNull String name) throws IOException {
        return doGet(name, false);
    }

    private ShaderProgram doGet(@NonNull String name, boolean bypassCache) throws IOException {
        String fileName = String.format("%s/prog/%s.json", BASE_PATH, name);
        JsonObject meta;
        try (InputStream in = ShaderManager.class.getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalStateException(String.format("Unable to find shader meta file: \"%s\"!", fileName));
            }
            meta = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
        }
        if (!meta.has("vert")) {
            throw new IllegalStateException(String.format("Shader \"%s\" has no vertex shader!", name));
        } else if (!meta.has("frag")) {
            throw new IllegalStateException(String.format("Shader \"%s\" has no fragment shader!", name));
        }
        String cacheName = bypassCache ? "_reload_" + String.valueOf(RELOAD_COUNTER++) : null;
        return new ShaderProgram(
                name,
                get(meta.get("vert").getAsString(), cacheName, ShaderType.VERTEX),
                get(meta.get("frag").getAsString(), cacheName, ShaderType.FRAGMENT)
        );
    }

    /**
     * Reloads the given shader program. The program will be disposed, and then replaced with a freshly loaded version from disk.
     * <p>
     * This is only present for debug purposes, and is highly likely to break things.
     *
     * @param program the program to reload
     * @return the reloaded shader program
     */
    public ShaderProgram reload(@NonNull ShaderProgram program) {
        OpenGL.assertOpenGL();
        program.release();
        try {
            program = doGet(program.name, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LINKED_PROGRAMS.put(program.name, program);
        return program.incrementUsages();
    }

    protected Shader get(@NonNull String name, String cacheName, @NonNull ShaderType type) {
        OpenGL.assertOpenGL();
        if (cacheName == null) {
            cacheName = name;
        } else {
            cacheName = name + cacheName;
        }
        return type.compiledShaders.computeIfAbsent(cacheName, (IOFunction<String, Shader>) aaaaaa_uselessParam -> {
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
        });
    }

    protected void validate(@NonNull String name, int id, int type) {
        if (OpenGL.glGetShaderi(id, type) == OpenGL.GL_FALSE) {
            String error = String.format("Couldn't compile shader \"%s\": %s", name, OpenGL.glGetLogInfo(id));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }
}
