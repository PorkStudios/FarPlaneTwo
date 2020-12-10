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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.binary.oio.StreamUtil;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.common.function.io.IOBiConsumer;
import net.daporkchop.lib.common.function.io.IOFunction;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
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

    protected final LoadingCache<String, ShaderProgram> SHADER_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, ShaderProgram>() {
                @Override
                public ShaderProgram load(String programName) throws Exception {
                    ProgramMeta meta;
                    try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", BASE_PATH, programName))) {
                        checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", BASE_PATH, programName);
                        meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
                    }

                    checkState(meta.vert != null, "Program \"%s\" has no vertex shaders!", programName);
                    if (meta.xfb_varying != null) {
                        checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", programName);
                    } else {
                        checkState(meta.frag != null, "Program \"%s\" has no fragment shaders!", programName);
                    }

                    return new ShaderProgram(
                            programName,
                            get(meta.vert, ShaderType.VERTEX),
                            meta.geom != null ? get(meta.geom, ShaderType.GEOMETRY) : null,
                            meta.frag != null ? get(meta.frag, ShaderType.FRAGMENT) : null,
                            meta.xfb_varying);
                }
            });

    /**
     * Obtains a shader program with the given name.
     *
     * @param programName the name of the shader to get
     * @return the shader program with the given name
     */
    public ShaderProgram get(@NonNull String programName) {
        return SHADER_CACHE.getUnchecked(programName);
    }

    protected Shader get(@NonNull String[] names, @NonNull ShaderType type) {
        return new Shader(type, names, Stream.concat(
                Stream.of("#version 430 core\n"), //add version prefix
                Arrays.stream(names)
                        .map((IOFunction<String, String>) fileName -> {
                            try (InputStream in = ShaderManager.class.getResourceAsStream(BASE_PATH + '/' + fileName)) {
                                checkState(in != null, "Unable to find shader file: \"%s\"!", fileName);
                                return new String(StreamUtil.toByteArray(in), StandardCharsets.UTF_8);
                            }
                        }))
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
        try {
            AtomicInteger shaderCount = new AtomicInteger();
            SHADER_CACHE.asMap().forEach((IOBiConsumer<String, ShaderProgram>) (programName, program) -> {
                ProgramMeta meta;
                try (InputStream in = ShaderManager.class.getResourceAsStream(PStrings.fastFormat("%s/prog/%s.json", BASE_PATH, programName))) {
                    checkArg(in != null, "Unable to find shader meta file: \"%s/prog/%s.json\"!", BASE_PATH, programName);
                    meta = GSON.fromJson(new UTF8FileReader(in), ProgramMeta.class);
                }

                checkState(meta.vert != null, "Program \"%s\" has no vertex shaders!", programName);
                if (meta.xfb_varying != null) {
                    checkState(meta.frag == null, "Program \"%s\" has both fragment shaders and transform feedback outputs!", programName);
                } else {
                    checkState(meta.frag != null, "Program \"%s\" has no fragment shaders!", programName);
                }

                program.reload(
                        get(meta.vert, ShaderType.VERTEX),
                        meta.geom != null ? get(meta.geom, ShaderType.GEOMETRY) : null,
                        meta.frag != null ? get(meta.frag, ShaderType.FRAGMENT) : null,
                        meta.xfb_varying);
                shaderCount.incrementAndGet();
            });
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§a" + shaderCount.get() + " shaders successfully reloaded."));
        } catch (Exception e) {
            LOGGER.error("shader reload failed", e);
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§cshaders reload failed (check console)."));
        }
    }
}
