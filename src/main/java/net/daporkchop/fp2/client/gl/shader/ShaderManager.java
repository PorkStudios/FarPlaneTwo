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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.debug.util.DebugUtils;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Manages loaded shaders.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderManager {
    protected final String BASE_PATH = "shaders";

    protected final LoadingCache<ResourceLocation, SourceLine[]> SOURCE_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<ResourceLocation, SourceLine[]>() {
                @Override
                public SourceLine[] load(@NonNull ResourceLocation location) throws IOException {
                    List<SourceLine> lines = new LinkedList<>();

                    //read each line and wrap it in a SourceLine
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(MC.resourceManager.getResource(location).getInputStream()))) {
                        for (String line; (line = reader.readLine()) != null; ) {
                            lines.add(new SourceLine(line, location, lines.size() + 1));
                        }
                    }

                    //strip comments
                    final StringBuilder builder = new StringBuilder();
                    boolean inMultilineComment = false;

                    for (ListIterator<SourceLine> itr = lines.listIterator(); itr.hasNext(); ) {
                        SourceLine line = itr.next();
                        String text = line.text();

                        builder.setLength(0);

                        ITERATE_CHARS:
                        for (int i = 0, len = text.length(); i < len; i++) {
                            char c = text.charAt(i);

                            if (inMultilineComment) { //we're currently in a multiline comment
                                if (c == '*' && i + 1 < len && text.charAt(i + 1) == '/') { //we reached the end of the comment
                                    inMultilineComment = false;
                                    i++;
                                } else {
                                    //no-op, skip the character
                                }
                            } else {
                                if (c == '/' && i + 1 < len) { //this is a potential comment start
                                    switch (text.charAt(i + 1)) {
                                        case '/': //single line comment
                                            break ITERATE_CHARS;
                                        case '*': //multiline comment
                                            inMultilineComment = true;
                                            i++;
                                            continue;
                                    }
                                }

                                //no special handling required for this char, copy it
                                builder.append(c);
                            }
                        }

                        text = builder.toString();
                        if (text.isEmpty() || text.trim().isEmpty()) { //line is empty or effectively empty, discard it
                            itr.remove();
                        } else { //update line with processed text
                            itr.set(line.withText(text));
                        }
                    }

                    return lines.toArray(new SourceLine[0]);
                }
            });

    protected final LoadingCache<AbstractShaderBuilder, ShaderProgram> SHADER_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(CacheLoader.from(AbstractShaderBuilder::supply));

    protected Map<String, Object> GLOBAL_DEFINES = Collections.emptyMap();

    public RenderShaderBuilder renderShaderBuilder(@NonNull String programName) {
        return new RenderShaderBuilder(programName, null, null, null, Collections.emptyMap());
    }

    public ComputeShaderBuilder computeShaderBuilder(@NonNull String programName) {
        return new ComputeShaderBuilder(programName, null, null, Collections.emptyMap());
    }

    /**
     * Obtains a shader program with the given name.
     *
     * @param programName the name of the shader to get
     * @return the shader program with the given name
     */
    @Deprecated
    public RenderShaderProgram get(@NonNull String programName) {
        return renderShaderBuilder(programName).link();
    }

    protected Shader get(@NonNull ResourceLocation name, @NonNull Map<String, Object> macros, @NonNull ShaderType type) {
        return new Shader(type, name, new Preprocessor(macros, SOURCE_CACHE)
                .appendLines(new SourceLine("#version 430 core", new ResourceLocation(MODID, "<auto-generated>"), 1))
                .appendLines(name)
                .preprocess()
                .lines());
    }

    protected void validateShaderCompile(int id, @NonNull ResourceLocation name, @NonNull SourceLine... lines) {
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = PStrings.fastFormat("Couldn't compile shader \"%s\":\n%s",
                    name,
                    formatInfoLog(glGetShaderInfoLog(id, glGetShaderi(id, GL_INFO_LOG_LENGTH)), lines));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }

    protected void validateProgramLink(@NonNull String name, int id) {
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            String error = PStrings.fastFormat("Couldn't link program \"%s\":\n%s", name, glGetProgramInfoLog(id, glGetProgrami(id, GL_INFO_LOG_LENGTH)));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }

    protected void validateProgramValidate(@NonNull String name, int id) {
        if (glGetProgrami(id, GL_VALIDATE_STATUS) == GL_FALSE) {
            String error = PStrings.fastFormat("Couldn't validate program \"%s\":\n%s", name, glGetProgramInfoLog(id, glGetProgrami(id, GL_INFO_LOG_LENGTH)));
            System.err.println(error);
            throw new IllegalStateException(error);
        }
    }

    protected String formatInfoLog(@NonNull String origText, @NonNull SourceLine... lines) {
        Matcher matcher = Pattern.compile("^(-?\\d+)\\((-?\\d+)\\) (: .+)", Pattern.MULTILINE).matcher(origText);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            SourceLine line = lines[Integer.parseInt(matcher.group(2)) - 1];
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("(" + line.location() + ':' + line.lineNumber() + ')' + matcher.group(3)));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    public DefinesChangeBatch changeDefines() {
        return new DefinesChangeBatch();
    }

    @SuppressWarnings("unchecked")
    public void reload(boolean message) {
        try {
            SHADER_CACHE.asMap().forEach(AbstractShaderBuilder::reload);
            if (message) {
                DebugUtils.clientMsg("§a" + SHADER_CACHE.size() + " shaders successfully reloaded.");
            }
        } catch (Exception e) {
            FP2_LOG.error("shader reload failed", e);
            DebugUtils.clientMsg("§cshaders reload failed (check console).");
        }
    }

    public static abstract class AbstractShaderBuilder<B extends AbstractShaderBuilder<B, S>, S extends ShaderProgram<S>> {
        public S link() {
            return uncheckedCast(SHADER_CACHE.getUnchecked(this));
        }

        public B define(@NonNull String name) {
            return this.define(name, "");
        }

        public abstract B define(@NonNull String name, @NonNull Object value);

        public abstract B undefine(@NonNull String name);

        protected abstract S supply();

        protected abstract void reload(@NonNull S program);

        @Override
        public abstract int hashCode();

        @Override
        public abstract boolean equals(Object obj);
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static final class DefinesChangeBatch {
        protected final Map<String, Object> originals = GLOBAL_DEFINES;
        protected final Map<String, Object> updated = new Object2ObjectOpenHashMap<>(this.originals);
        protected boolean dirty = false;

        public DefinesChangeBatch undefine(@NonNull String name) {
            if (this.updated.remove(name) != null) {
                this.dirty = true;
            }
            return this;
        }

        public DefinesChangeBatch define(@NonNull String name) {
            return this.define(name, "");
        }

        public DefinesChangeBatch define(@NonNull String name, @NonNull Object value) {
            if (!Objects.equals(this.updated.put(name, value), value)) {
                this.dirty = true;
            }
            return this;
        }

        public void apply() {
            if (GLOBAL_DEFINES != this.originals) {
                throw new ConcurrentModificationException();
            }

            if (this.dirty) { //avoid doing an expensive shader reload if nothing changed
                GLOBAL_DEFINES = ImmutableMap.copyOf(this.updated);
                reload(false);
            }
        }
    }
}
