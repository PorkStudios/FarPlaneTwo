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

package net.daporkchop.fp2.core.client.shader;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
@ToString
public final class ShaderMacros {
    /**
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    private final ImmutableMap<String, Object> defines;

    private boolean isEmpty() {
        return this.defines.isEmpty();
    }

    /**
     * @return an {@link ImmutableMap} containing the actual macro values
     */
    public ImmutableMap<String, Object> defines() {
        return this.defines;
    }

    /**
     * @return a {@link Builder} for constructing a new {@link ShaderMacros} instance derived from this instance
     */
    public Builder toBuilder() {
        return builder().defineAll(this.defines);
    }

    /**
     * Equivalent to {@code this.toBuilder().defineAll(defines).build()}.
     *
     * @param defines the macro values to define
     * @return an instance of {@link ShaderMacros} containing all macro values defined by this instance merged with the given macro values
     */
    public ShaderMacros withDefined(@NonNull Map<String, Object> defines) {
        if (defines.isEmpty()) {
            return this;
        }

        return this.toBuilder().defineAll(defines).build();
    }

    /**
     * Equivalent to {@code this.toBuilder().defineAll(defines).build()}.
     *
     * @param macros the macro values to define
     * @return an instance of {@link ShaderMacros} containing all macro values defined by this instance merged with the given macro values
     */
    public ShaderMacros withDefined(@NonNull ShaderMacros macros) {
        if (macros.isEmpty()) {
            return this;
        }

        return this.toBuilder().defineAll(macros).build();
    }

    /**
     * Builder class for constructing
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Builder {
        private final ImmutableMap.Builder<String, Object> delegate = ImmutableMap.builder();

        /**
         * Equivalent to {@code define(key, true)}.
         *
         * @see #define(String, Object)
         */
        public Builder define(@NonNull String key) {
            return this.define(key, true);
        }

        /**
         * Defines a macro value, replacing any existing macro values for the given key.
         *
         * @param key   the macro key
         * @param value the macro value
         */
        public Builder define(@NonNull String key, @NonNull Object value) {
            this.delegate.put(key, value);
            return this;
        }

        /**
         * Defines a macro value, replacing any existing macro values for the given key.
         *
         * @param defines the macro values to define
         */
        public Builder defineAll(@NonNull Map<String, Object> defines) {
            this.delegate.putAll(defines);
            return this;
        }

        /**
         * Defines a macro value, replacing any existing macro values for the given key.
         *
         * @param macros the macro values to define
         */
        public Builder defineAll(@NonNull ShaderMacros macros) {
            return this.defineAll(macros.defines());
        }

        /**
         * @return a {@link ShaderMacros} containing the built result
         */
        public ShaderMacros build() {
            return new ShaderMacros(this.delegate.build());
        }
    }
}
