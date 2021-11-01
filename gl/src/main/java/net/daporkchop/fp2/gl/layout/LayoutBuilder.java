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

package net.daporkchop.fp2.gl.layout;

import lombok.NonNull;
import net.daporkchop.fp2.gl.vertex.VertexFormat;

/**
 * Builder for {@link BaseLayout}s.
 *
 * @param <L> the type of {@link BaseLayout} to build
 * @author DaPorkchop_
 */
public interface LayoutBuilder<L extends BaseLayout> {
    /**
     * @return the constructed {@link L}
     */
    L build();

    /**
     * @author DaPorkchop_
     */
    interface UniformsStage<L extends BaseLayout> {
        /**
         * Defines the {@link VertexFormat}(s) used for the uniform vertex attributes.
         *
         * @param uniforms the formats of the uniform vertex attributes
         */
        //GlobalsStage<L> withUniforms(@NonNull VertexFormat... uniforms);
        GlobalsStage<L> withUniforms(); //TODO: uniforms, somehow
    }

    /**
     * @author DaPorkchop_
     */
    interface GlobalsStage<L extends BaseLayout> {
        /**
         * Defines the {@link VertexFormat}(s) used for the global vertex attributes.
         *
         * @param globals the formats of the global vertex attributes
         */
        LocalsStage<L> withGlobals(@NonNull VertexFormat... globals);
    }

    /**
     * @author DaPorkchop_
     */
    interface LocalsStage<L extends BaseLayout> {
        /**
         * Defines the {@link VertexFormat}(s) used for the local vertex attributes.
         *
         * @param locals the local vertex attributes
         */
        LayoutBuilder<L> withLocals(@NonNull VertexFormat... locals);
    }
}
