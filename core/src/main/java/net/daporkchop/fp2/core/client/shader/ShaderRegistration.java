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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;

import java.util.ServiceLoader;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class ShaderRegistration {
    /**
     * Returns an {@link Iterable} each of the supported {@link ShaderRegistration}s.
     *
     * @param fp2 the current {@link FP2Core FP2} instance
     * @return an {@link Iterable} each of the supported {@link ShaderRegistration}s
     */
    public static Iterable<? extends ShaderRegistration> getTypes(@NonNull FP2Core fp2) {
        return ServiceLoader.load(ShaderRegistration.class, fp2.getClass().getClassLoader());
    }

    /**
     * The set of {@link GLExtension OpenGL extensions} which are required to register these shaders.
     */
    private final @NonNull GLExtensionSet requiredExtensions;

    /**
     * Registers the shaders defined by this {@link ShaderRegistration} instance.
     *
     * @param globalRenderer the {@link GlobalRenderer} instance
     * @param shaderRegistry the {@link ReloadableShaderRegistry} to register the shaders to
     * @param shaderMacros   the base {@link ShaderMacros} which will probably be inherited by all shaders
     * @param client         the {@link FP2Core} instance
     * @param gl             the {@link OpenGL} context
     * @throws UnsupportedOperationException if this {@link ShaderRegistration} isn't supported
     */
    public abstract void registerShaders(@NonNull GlobalRenderer globalRenderer, @NonNull ReloadableShaderRegistry shaderRegistry, @NonNull ShaderMacros shaderMacros, @NonNull FP2Client client, @NonNull OpenGL gl);
}
