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

package net.daporkchop.fp2.client.gl.vertex;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import net.daporkchop.lib.common.misc.string.PStrings;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Abstract class for a builder type for {@link IVertexAttribute} implementations.
 *
 * @author DaPorkchop_
 */
@Setter
@Getter
@ToString
public abstract class VertexAttributeBuilder<T extends IVertexAttribute> {
    /**
     * Obtains a new {@link VertexAttributeBuilder} which will use the factories in the given map to create instances based on the vertex data type.
     *
     * @param map the factory map
     * @param <T> the vertex attribute type
     * @return a new {@link VertexAttributeBuilder}
     */
    public static <T extends IVertexAttribute> VertexAttributeBuilder<T> fromMap(@NonNull Map<VertexAttributeType, Function<VertexAttributeBuilder<T>, T>> map) {
        return new VertexAttributeBuilder<T>() {
            @Override
            protected T build0() {
                Function<VertexAttributeBuilder<T>, T> factory = map.get(this.type);
                checkArg(factory != null, "unable to create vertex attribute for data type: %s", this.type);
                return factory.apply(this);
            }
        };
    }

    protected IVertexAttribute parent;
    @NonNull
    protected VertexAttributeInterpretation interpretation;
    @NonNull
    protected VertexAttributeType type;

    protected int divisor = 0;
    protected int components = -1;
    protected int reportedComponents = -1;
    @Setter(AccessLevel.NONE)
    protected int alignment = 1;
    @Setter(AccessLevel.NONE)
    protected int padding = 1;

    public VertexAttributeBuilder<T> divisor(int divisor) {
        this.divisor = notNegative(divisor, "divisor");
        return this;
    }

    protected VertexAttributeBuilder<T> components(int components) {
        this.components = positive(components, "components");
        return this;
    }

    public VertexAttributeBuilder<T> reportedComponents(int reportedComponents) {
        this.reportedComponents = positive(reportedComponents, "reportedComponents");
        return this;
    }

    public VertexAttributeBuilder<T> alignTo(int size) {
        this.alignment = positive(size, "size");
        return this;
    }

    public VertexAttributeBuilder<T> padTo(int size) {
        this.padding = positive(size, "size");
        return this;
    }

    public VertexAttributeBuilder<T> alignAndPadTo(int size) {
        return this.alignTo(size).padTo(size);
    }

    /**
     * @return the built vertex attribute
     */
    public T build() {
        checkArg(this.interpretation != null, "interpretation must be set!");
        checkArg(this.type != null, "type must be set!");
        checkArg(this.components >= 0, "component count must be set!");

        return this.build0();
    }

    protected abstract T build0();

    protected VertexAttributeBuilder<T> ensureType(@NonNull VertexAttributeType... expectedTypes) {
        for (VertexAttributeType expectedType : expectedTypes) {
            if (this.type == expectedType) {
                return this;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("unable to build vertex attribute %s: expected type to be one of %s", this, Arrays.toString(expectedTypes)));
    }
}
