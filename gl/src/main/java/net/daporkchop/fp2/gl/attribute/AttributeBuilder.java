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

package net.daporkchop.fp2.gl.attribute;

import lombok.NonNull;

/**
 * Builder for a {@link Attribute}.
 *
 * @param <V> the type of vertex attribute that will be built
 * @author DaPorkchop_
 */
public interface AttributeBuilder<V extends Attribute> {
    /**
     * @return the built {@link V}
     */
    V build();

    /**
     * @author DaPorkchop_
     */
    interface NameSelectionStage {
        /**
         * Configures the {@link Attribute}'s name.
         * <p>
         * The name must be unique across all {@link Attribute}s belonging to the parent {@link AttributeFormatBuilder}.
         *
         * @param name the name
         */
        TypeSelectionStage name(@NonNull String name);
    }

    /**
     * @author DaPorkchop_
     */
    interface TypeSelectionStage {
        InterpretationSelectionStage<Attribute.Int1> int1(@NonNull AttributeType.Integer type);

        InterpretationSelectionStage<Attribute.Int2> int2(@NonNull AttributeType.Integer type);

        InterpretationSelectionStage<Attribute.Int3> int3(@NonNull AttributeType.Integer type);

        InterpretationSelectionStage<Attribute.Int4> int4(@NonNull AttributeType.Integer type);
    }

    /**
     * @author DaPorkchop_
     */
    interface InterpretationSelectionStage<V extends Attribute> {
        /**
         * Configures the type that this vertex attribute will be interpreted as.
         *
         * @param interpretation the interpretation
         */
        AttributeBuilder<V> interpretation(@NonNull AttributeInterpretation interpretation);
    }
}
