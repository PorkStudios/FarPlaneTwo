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

package net.daporkchop.fp2.gl.shader;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.gl.layout.BaseLayout;

import java.util.Map;

/**
 * Builder for {@link BaseShader}s.
 *
 * @param <S> the type of {@link BaseShader} to compile
 * @author DaPorkchop_
 */
public interface ShaderBuilder<S extends BaseShader> {
    /**
     * @return the compiled {@link S}
     * @throws ShaderCompilationException if shader compilation fails
     */
    S build() throws ShaderCompilationException;

    /**
     * @author DaPorkchop_
     */
    interface LayoutStage<S extends BaseShader, L extends BaseLayout> {
        /**
         * Sets the {@link L} which will be used by the shader.
         *
         * @param layout the layout
         */
        SourceStage<S> forLayout(@NonNull L layout);
    }

    /**
     * @author DaPorkchop_
     */
    interface SourceStage<S extends BaseShader> {
        /**
         * Loads the source code from the resource with the given {@link Identifier} and appends it to the final code.
         *
         * @param id the source code's resource ID
         */
        AdditionalSourceStage<S> include(@NonNull Identifier id);
    }

    /**
     * @author DaPorkchop_
     */
    interface AdditionalSourceStage<S extends BaseShader> extends SourceStage<S> {
        /**
         * Indicates that no further source files will be included.
         */
        DefinesStage<S> endSource();
    }

    /**
     * @author DaPorkchop_
     */
    interface DefinesStage<S extends BaseShader> {
        /**
         * Defines the given preprocessor macro.
         *
         * @param key   the macro key
         * @param value the macro value
         */
        AdditionalDefinesStage<S> define(@NonNull String key, @NonNull Object value);

        /**
         * Defines multiple preprocessor macros.
         *
         * @param macros a mapping of macro keys to values
         */
        AdditionalDefinesStage<S> defineAll(@NonNull Map<String, Object> macros);
    }

    /**
     * @author DaPorkchop_
     */
    interface AdditionalDefinesStage<S extends BaseShader> extends DefinesStage<S> {
        /**
         * Indicates that no further preprocessor macros will be defined.
         */
        ShaderBuilder<S> endDefines();
    }
}
