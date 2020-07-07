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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.client.render.render.OpenGL.*;

/**
 * The different types of shaders.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum ShaderType {
    VERTEX("vert", GL_VERTEX_SHADER) {
        @Override
        protected Shader construct(@NonNull String name, @NonNull String code, @NonNull JsonObject meta) {
            return new Shader(name, code, meta) {
                @Getter
                @Accessors(fluent = true)
                protected Collection<String> provides;

                @Override
                protected ShaderType type() {
                    return VERTEX;
                }

                @Override
                protected void load(@NonNull JsonObject meta) {
                    if (this.provides != null) {
                        throw new IllegalStateException("Already initialized!");
                    } else if (!meta.has("provides") || !meta.get("provides").isJsonArray()) {
                        throw new IllegalArgumentException(String.format("Metadata for vertex shader \"%s\" does not define provided variables!", this.name));
                    }
                    this.provides = new HashSet<>();
                    for (JsonElement element : meta.getAsJsonArray("provides")) {
                        this.provides.add(element.getAsString());
                    }
                }

                @Override
                protected Collection<String> requires() throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
        }
    },
    FRAGMENT("frag", GL_FRAGMENT_SHADER) {
        @Override
        protected Shader construct(@NonNull String name, @NonNull String code, @NonNull JsonObject meta) {
            return new Shader(name, code, meta) {
                @Getter
                @Accessors(fluent = true)
                protected Collection<String> requires;

                @Override
                protected ShaderType type() {
                    return FRAGMENT;
                }

                @Override
                protected void load(@NonNull JsonObject meta) {
                    if (this.requires != null) {
                        throw new IllegalStateException("Already initialized!");
                    } else if (!meta.has("requires") || !meta.get("requires").isJsonArray()) {
                        throw new IllegalArgumentException(String.format("Metadata for fragment shader \"%s\" does not define required variables!", this.name));
                    }
                    this.requires = new HashSet<>();
                    for (JsonElement element : meta.getAsJsonArray("requires")) {
                        this.requires.add(element.getAsString());
                    }
                }

                @Override
                protected void assertCompatible(@NonNull Shader counterpart) throws IllegalArgumentException {
                    super.assertCompatible(counterpart);
                    if (!counterpart.provides().containsAll(this.requires)) {
                        throw new IllegalArgumentException(String.format(
                                "Missing required parameters! Required: [%s], provided: [%s]",
                                this.requires.stream().collect(Collectors.joining(", ")),
                                counterpart.provides().stream().collect(Collectors.joining(", "))
                        ));
                    }
                }

                @Override
                protected Collection<String> provides() throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
        }
    };

    @Getter(AccessLevel.NONE)
    protected final Map<String, Shader> compiledShaders = new ConcurrentHashMap<>();
    @NonNull
    protected final String extension;
    protected final int openGlId;

    protected abstract Shader construct(@NonNull String name, @NonNull String code, @NonNull JsonObject meta);
}
