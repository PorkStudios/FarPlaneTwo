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

package net.daporkchop.fp2.common.util;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Provides binary resources based on {@link Identifier}s.
 *
 * @author DaPorkchop_
 */
@FunctionalInterface
public interface ResourceProvider {
    static ResourceProvider selectingByNamespace(@NonNull String namespace, @NonNull ResourceProvider matches, @NonNull ResourceProvider notMatches) {
        return new ResourceProvider() {
            @Override
            public InputStream provideResourceAsStream(@NonNull Identifier id) throws IOException {
                return namespace.equals(id.namespace()) ? matches.provideResourceAsStream(id) : notMatches.provideResourceAsStream(id);
            }

            @Override
            public Reader provideResourceAsReader(@NonNull Identifier id) throws IOException {
                return namespace.equals(id.namespace()) ? matches.provideResourceAsReader(id) : notMatches.provideResourceAsReader(id);
            }

            @Override
            public Reader provideResourceAsReader(@NonNull Identifier id, @NonNull Charset charset) throws IOException {
                return namespace.equals(id.namespace()) ? matches.provideResourceAsReader(id, charset) : notMatches.provideResourceAsReader(id, charset);
            }
        };
    }

    static ResourceProvider loadingClassResources(@NonNull Class<?> clazz) {
        return id -> {
            InputStream stream = clazz.getResourceAsStream(id.path());
            if (stream != null) {
                return stream;
            }
            throw new ResourceNotFoundException(id);
        };
    }

    /**
     * Gets the resource with the provided {@link Identifier} as an {@link InputStream}.
     *
     * @param id the resource id
     * @return an {@link InputStream} containing the resource data
     * @throws ResourceNotFoundException if no resource with the given id could be found
     */
    InputStream provideResourceAsStream(@NonNull Identifier id) throws IOException;

    /**
     * Gets the resource with the provided {@link Identifier} as a {@link Reader} using the {@code UTF-8} charset.
     *
     * @param id the resource id
     * @return a {@link Reader} containing the resource data
     * @throws ResourceNotFoundException if no resource with the given id could be found
     */
    default Reader provideResourceAsReader(@NonNull Identifier id) throws IOException {
        return this.provideResourceAsReader(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets the resource with the provided {@link Identifier} as a {@link Reader} using the given {@link Charset}.
     *
     * @param id      the resource id
     * @param charset the {@link Charset}
     * @return a {@link Reader} containing the resource data
     * @throws ResourceNotFoundException if no resource with the given id could be found
     */
    default Reader provideResourceAsReader(@NonNull Identifier id, @NonNull Charset charset) throws IOException {
        return new InputStreamReader(this.provideResourceAsStream(id), charset);
    }
}
