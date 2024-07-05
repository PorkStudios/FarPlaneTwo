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

package net.daporkchop.fp2.core.client.shader.variant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A simple implementation of a shader variant which simply wraps a map of macro keys to macro values.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, staticName = "from")
@EqualsAndHashCode
@ToString
public final class SimpleShaderVariant implements ShaderVariant {
    /**
     * @return a new builder for a set of {@link SimpleShaderVariant}s
     */
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    private final ImmutableMap<String, Object> defines;

    @Override
    public ImmutableMap<String, Object> defines() {
        return this.defines;
    }

    /**
     * Builder for a set of {@link SimpleShaderVariant}s representing every permutation of a set of properties with possible values.
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Builder {
        private final ImmutableMap.Builder<String, Object[]> delegate = ImmutableMap.builder();

        private Builder add(@NonNull String key, Object... values) {
            checkArg(values.length > 0, "expected at least 1 value for \"%s\" (got %s)", key, values.length);
            this.delegate.put(key, values);
            return this;
        }

        /**
         * Adds a boolean property with the given key.
         *
         * @param key the property key
         */
        public Builder addBoolean(String key) {
            return this.add(key, false, true);
        }

        /**
         * Adds an integer property with the given key.
         *
         * @param key   the property key
         * @param start the minimum integer value for the property (inclusive)
         * @param end   the maximum integer value for the property (exclusive)
         */
        public Builder addIntRange(String key, int start, int end) {
            return this.add(key, IntStream.range(start, end).boxed().toArray());
        }

        /**
         * Adds an integer property with the given key.
         *
         * @param key    the property key
         * @param values all possible values for the property
         */
        public Builder addIntValues(String key, int... values) {
            return this.add(key, IntStream.of(values).boxed().toArray());
        }

        /**
         * Builds a list of all possible combinations of the configured properties.
         *
         * @return a list of all possible combinations of the configured properties
         */
        public ImmutableList<SimpleShaderVariant> build() {
            List<Object2ObjectArrayMap<String, Object>> variants = Collections.singletonList(new Object2ObjectArrayMap<>());
            for (Map.Entry<String, Object[]> property : this.delegate.build().entrySet()) {
                String key = property.getKey();
                Object[] values = property.getValue();

                val oldVariants = variants;
                variants = new ArrayList<>(oldVariants.size() * values.length);
                for (val oldVariant : oldVariants) {
                    for (Object value : values) {
                        Object2ObjectArrayMap<String, Object> newVariant = new Object2ObjectArrayMap<>(oldVariant.size() + 1);
                        newVariant.putAll(oldVariant);
                        newVariant.put(key, value);
                        variants.add(newVariant);
                    }
                }
            }

            //noinspection UnstableApiUsage
            return variants.stream()
                    .map(defines -> from(ImmutableMap.copyOf(defines)))
                    .collect(ImmutableList.toImmutableList());
        }
    }
}
