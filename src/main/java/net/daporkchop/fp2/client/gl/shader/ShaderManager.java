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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.debug.util.DebugUtils;
import net.daporkchop.lib.binary.oio.StreamUtil;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.common.function.io.IOBiConsumer;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.misc.string.PStrings;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
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

    protected final LoadingCache<ShaderKey, ShaderProgram> SHADER_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<ShaderKey, ShaderProgram>() {
                @Override
                public ShaderProgram load(ShaderKey key) throws Exception {
                    ProgramMeta meta;
                    try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", BASE_PATH, key.programName))) {
                        checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", BASE_PATH, key);
                        meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
                    }

                    checkState(meta.vert != null, "Program \"%s\" has no vertex shaders!", key);
                    if (meta.xfb_varying != null) {
                        checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", key);
                    } else {
                        checkState(key.programName.contains("/xfb/") || meta.frag != null, "Program \"%s\" has no fragment shaders!", key);
                    }

                    return new ShaderProgram(
                            key,
                            get(meta.vert, key.defines, ShaderType.VERTEX),
                            meta.geom != null ? get(meta.geom, key.defines, ShaderType.GEOMETRY) : null,
                            meta.frag != null ? get(meta.frag, key.defines, ShaderType.FRAGMENT) : null,
                            meta.xfb_varying);
                }
            });

    /**
     * @see #get(String, Map)
     */
    public ShaderProgram get(@NonNull String programName) {
        return get(programName, ImmutableMap.of());
    }

    /**
     * Obtains a shader program with the given name.
     *
     * @param programName the name of the shader to get
     * @return the shader program with the given name
     */
    public ShaderProgram get(@NonNull String programName, @NonNull Map<String, String> defines) {
        return SHADER_CACHE.getUnchecked(new ShaderKey(programName, ImmutableMap.copyOf(defines)));
    }

    protected Shader get(@NonNull String[] names, @NonNull Map<String, String> defines, @NonNull ShaderType type) {
        return new Shader(type, names, Stream.concat(
                Stream.of(header(defines)),
                Arrays.stream(names)
                        .map((IOFunction<String, String>) fileName -> {
                            try (InputStream in = ShaderManager.class.getResourceAsStream(BASE_PATH + '/' + fileName)) {
                                checkState(in != null, "Unable to find shader file: \"%s\"!", fileName);
                                return new String(StreamUtil.toByteArray(in), StandardCharsets.UTF_8);
                            }
                        }))
                .toArray(String[]::new));
    }

    protected String header(@NonNull Map<String, String> defines) {
        return defines.entrySet().stream()
                .map(e -> "#define " + e.getKey() + ' ' + e.getValue())
                .collect(Collectors.joining("\n", "#version 440 core\n", "\n"));
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
        try {
            AtomicInteger shaderCount = new AtomicInteger();
            SHADER_CACHE.asMap().forEach((IOBiConsumer<ShaderKey, ShaderProgram>) (key, program) -> {
                ProgramMeta meta;
                try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", BASE_PATH, key.programName))) {
                    checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", BASE_PATH, key);
                    meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
                }

                checkState(meta.vert != null, "Program \"%s\" has no vertex shaders!", key);
                if (meta.xfb_varying != null) {
                    checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", key);
                } else {
                    checkState(key.programName.contains("/xfb/") || meta.frag != null, "Program \"%s\" has no fragment shaders!", key);
                }

                program.reload(
                        get(meta.vert, key.defines, ShaderType.VERTEX),
                        meta.geom != null ? get(meta.geom, key.defines, ShaderType.GEOMETRY) : null,
                        meta.frag != null ? get(meta.frag, key.defines, ShaderType.FRAGMENT) : null,
                        meta.xfb_varying);
                shaderCount.incrementAndGet();
            });
            DebugUtils.clientMsg("§a" + shaderCount.get() + " shaders successfully reloaded.");
        } catch (Exception e) {
            LOGGER.error("shader reload failed", e);
            DebugUtils.clientMsg("§cshaders reload failed (check console).");
        }
    }
}
