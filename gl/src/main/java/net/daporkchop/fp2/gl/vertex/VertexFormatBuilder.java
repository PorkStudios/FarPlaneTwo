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

package net.daporkchop.fp2.gl.vertex;

import lombok.NonNull;

/**
 * A builder for a {@link VertexFormat}.
 *
 * @author DaPorkchop_
 */
public interface VertexFormatBuilder {
    /**
     * @return a builder for constructing a new {@link VertexAttribute} which, when built, will belong to the constructed {@link VertexFormat}
     */
    VertexAttributeBuilder.NameSelectionStage attrib();

    /**
     * @return the constructed {@link VertexFormat}
     */
    VertexFormat build();

    /**
     * @author DaPorkchop_
     */
    interface NameSelectionStage {
        /**
         * Configures the {@link VertexAttribute}'s name.
         *
         * @param name the name
         */
        LayoutSelectionStage name(@NonNull String name);
    }

    /**
     * @author DaPorkchop_
     */
    interface LayoutSelectionStage {
        /**
         * Configures the {@link VertexFormat} to use interleaved attributes.
         */
        AlignmentSelectionStage interleaved();

        /**
         * Configures the {@link VertexFormat} to use one buffer per attribute.
         */
        AlignmentSelectionStage separate();
    }

    /**
     * @author DaPorkchop_
     */
    interface AlignmentSelectionStage {
        /**
         * Configures the {@link VertexFormat} to align each attribute to multiples of the given byte count.
         *
         * @param alignment the target attribute alignment (in bytes)
         */
        VertexFormatBuilder alignedTo(int alignment);

        /**
         * Configures the {@link VertexFormat} not to do any specific alignment of vertex attributes.
         */
        VertexFormatBuilder notAligned();
    }
}
