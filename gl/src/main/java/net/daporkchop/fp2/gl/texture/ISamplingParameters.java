/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.gl.texture;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.OpenGL;

import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Common interface for OpenGL objects which contain settable texture sampling parameters.
 *
 * @author DaPorkchop_
 */
public interface ISamplingParameters {
    /**
     * Sets the texture filtering mode.
     *
     * @param minFilter the texture minification filter
     * @param magFilter the texture magnification filter
     */
    default void filter(@NonNull TextureMinFilter minFilter, @NonNull TextureMagFilter magFilter) {
        this.setParameters(parameterSetter -> {
            parameterSetter.set(GL_TEXTURE_MIN_FILTER, minFilter.id());
            parameterSetter.set(GL_TEXTURE_MAG_FILTER, magFilter.id());
        });
    }

    /**
     * Sets the given sampling parameter to the given value.
     *
     * @param pname the sampling parameter
     * @param param the parameter value
     */
    void setParameter(int pname, int param);

    /**
     * Sets the given sampling parameter to the given value.
     *
     * @param pname the sampling parameter
     * @param param the parameter value
     */
    void setParameter(int pname, float param);

    /**
     * Sets multiple sampling parameters.
     *
     * @param action a function which will be called with a {@link ParameterSetter} which may be used to set sampling parameters
     */
    void setParameters(@NonNull Consumer<ParameterSetter> action);

    /**
     * A handle for setting sampling parameter values.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    abstract class ParameterSetter {
        protected final OpenGL gl;

        //@formatter:off
        public abstract void set(int pname, int param);
        public abstract void set(int pname, float param);
        //@formatter:on
    }
}
