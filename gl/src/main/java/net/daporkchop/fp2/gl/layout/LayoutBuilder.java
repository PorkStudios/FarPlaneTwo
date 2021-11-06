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
import net.daporkchop.fp2.gl.attribute.AttributeFormat;

/**
 * Builder for {@link BaseLayout}s.
 *
 * @param <L> the type of {@link BaseLayout} to build
 * @author DaPorkchop_
 */
public interface LayoutBuilder<L extends BaseLayout> {
    /**
     * Adds a {@link AttributeFormat} which contains uniform attributes.
     *
     * @param uniforms the formats of the uniform attributes
     */
    LayoutBuilder<L> withUniforms(@NonNull AttributeFormat uniforms);
    
    /**
     * Adds a {@link AttributeFormat} which contains global attributes.
     *
     * @param globals the formats of the global attributes
     */
    LayoutBuilder<L> withGlobals(@NonNull AttributeFormat globals);
    
    /**
     * Adds a {@link AttributeFormat} which contains local attributes.
     *
     * @param locals the formats of the local attributes
     */
    LayoutBuilder<L> withLocals(@NonNull AttributeFormat locals);

    /**
     * Adds a {@link AttributeFormat} which contains output attributes.
     *
     * @param outputs the formats of the output attributes
     */
    LayoutBuilder<L> withOutputs(@NonNull AttributeFormat outputs);
    
    /**
     * @return the constructed {@link L}
     */
    L build();
}
