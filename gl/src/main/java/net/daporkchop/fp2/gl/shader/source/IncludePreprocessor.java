/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl.shader.source;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Processes {@code #include} directives in a GLSL source file.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class IncludePreprocessor {
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^#include <\"([^\"]+)\">");

    @NonNull
    private final ResourceProvider resourceProvider;

    @Getter
    private final StringBuilder buffer = new StringBuilder();
    @Getter
    private final List<SourceLocation> locations = new ArrayList<>();

    private final Set<Identifier> includedFilesWithPragmaOnce = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * @return a copy of this {@link IncludePreprocessor} in the same state
     */
    @Override
    public IncludePreprocessor clone() {
        IncludePreprocessor result = new IncludePreprocessor(this.resourceProvider);
        result.buffer.append(this.buffer);
        result.locations.addAll(this.locations);
        result.includedFilesWithPragmaOnce.addAll(this.includedFilesWithPragmaOnce);
        return result;
    }

    /**
     * Adds the version string for the current OpenGL context to the beginning of the shader source code.
     *
     * @param gl the OpenGL context
     */
    public IncludePreprocessor addVersionHeader(OpenGL gl) {
        checkState(this.locations.isEmpty(), "version header must be at top of shader source");

        this.buffer.append("#version ").append(gl.version().glsl()).append('\n');
        this.locations.add(SourceLocation.GENERATED);
        for (GLExtension extension : gl.nonCoreExtensions()) {
            if (extension.glsl()) {
                this.buffer.append("#extension ").append(extension.name()).append(" : require\n");
                this.locations.add(SourceLocation.GENERATED);
            }
        }
        return this;
    }

    /**
     * Defines the given preprocessor macros.
     *
     * @param defines the names and values of the preprocessor macros to define
     */
    public IncludePreprocessor define(Map<String, ?> defines) {
        checkState(!this.locations.isEmpty(), "defines may not be at top of shader source");

        for (Map.Entry<String, ?> entry : defines.entrySet()) {
            Object rawValue = entry.getValue();
            String value;
            if (rawValue instanceof Boolean) {
                value = ((boolean) rawValue) ? "1" : "0";
            } else {
                value = String.valueOf(rawValue);
            }

            this.buffer.append("#define ").append(entry.getKey()).append(' ').append(value).append('\n');
            this.locations.add(SourceLocation.GENERATED);
        }
        return this;
    }

    /**
     * Includes the source lines from the file with the given {@link Identifier}.
     *
     * @param file the {@link Identifier} of the source file to be included
     */
    public IncludePreprocessor include(Identifier file) throws IOException, ResourceNotFoundException {
        return this.include(file, null);
    }

    private IncludePreprocessor include(Identifier file, SourceLocation includedFrom) throws IOException, ResourceNotFoundException {
        if (this.includedFilesWithPragmaOnce.contains(file)) {
            //this source file contains a "#pragma once" and has already been included, don't include it again!
            return this;
        }

        int lineNo = 0;
        for (String line : this.resourceProvider.provideResourceAsLines(file)) {
            lineNo++;

            SourceLocation location = new SourceLocation(file, lineNo);
            if (line.startsWith("#include ")) {
                Matcher matcher = INCLUDE_PATTERN.matcher(line);
                checkArg(matcher.find(), location);
                this.include(Identifier.from(matcher.group(1)), location);
            } else if ("#pragma once".equals(line)) {
                //remember that the file contained a line with "#pragma once" so that we don't end up including it again
                this.includedFilesWithPragmaOnce.add(file);
            } else {
                this.buffer.append(line).append('\n');
                this.locations.add(location);
            }
        }
        return this;
    }
}
