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

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.core.client.shader.NewReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.function.plain.TriFunction;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A collection of variants of a single shader program.
 *
 * @author DaPorkchop_
 */
public final class ShaderVariants<Variant, P extends ShaderProgram> implements AutoCloseable {
    private static ImmutableMap<String, Object> defines(Object variant) {
        if (variant instanceof ShaderVariant) {
            return ((ShaderVariant) variant).defines();
        } else {
            return uncheckedCast(variant);
        }
    }

    private final ImmutableMap<Variant, NewReloadableShaderProgram<P>> shaderVariants;

    public ShaderVariants(ReloadableShaderRegistry registry, ShaderMacros macros, BiFunction<ReloadableShaderRegistry, ShaderMacros, NewReloadableShaderProgram<P>> compileFunction, Stream<Variant> variants) {
        Map<Variant, NewReloadableShaderProgram<P>> shaderVariants = new HashMap<>();
        try {
            variants.forEach(variant -> {
                val variantMacros = macros.toBuilder().defineAll(defines(variant)).build();
                NewReloadableShaderProgram<P> shader = Objects.requireNonNull(compileFunction.apply(registry, variantMacros));
                if (shaderVariants.putIfAbsent(variant, shader) != null) {
                    shader.close();
                    throw new IllegalArgumentException("duplicate shader variant key: " + variant);
                }
            });
            this.shaderVariants = ImmutableMap.copyOf(shaderVariants);
        } catch (Throwable t) {
            throw PResourceUtil.closeAllSuppressed(t, shaderVariants.values());
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(this.shaderVariants.values());
    }

    /**
     * Gets the compiled shader program for the given variant key.
     *
     * @param variant the key describing the shader variant to get
     * @return the corresponding shader program variant
     */
    public NewReloadableShaderProgram<P> get(Variant variant) {
        NewReloadableShaderProgram<P> shader = this.shaderVariants.get(variant);
        checkArg(shader != null, "unknown shader variant: %s", variant);
        return shader;
    }

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface VariantSetupFunction<Variant, B extends ShaderProgram.Builder<?, B>> {
        void setup(Variant variant, B builder);
    }
}
