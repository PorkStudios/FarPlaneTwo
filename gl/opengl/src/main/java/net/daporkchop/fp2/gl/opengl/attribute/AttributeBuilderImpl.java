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

package net.daporkchop.fp2.gl.opengl.attribute;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeBuilder;
import net.daporkchop.fp2.gl.attribute.AttributeInterpretation;
import net.daporkchop.fp2.gl.attribute.AttributeType;

import java.util.function.Function;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class AttributeBuilderImpl implements AttributeBuilder.NameSelectionStage, AttributeBuilder.TypeSelectionStage, AttributeBuilder.InterpretationSelectionStage<Attribute>, AttributeBuilder<Attribute> {
    @NonNull
    protected final AttributeFormatBuilderImpl formatBuilder;

    protected String name;
    protected AttributeType type;
    protected AttributeInterpretation interpretation;

    protected int components = -1;

    protected int reportedComponents = -1;

    //
    // NameSelectionStage
    //

    @Override
    public TypeSelectionStage name(@NonNull String name) {
        this.name = name;
        return this;
    }

    //
    // TypeSelectionStage
    //

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int1> int1(@NonNull AttributeType.Integer type) {
        this.type = type;
        this.components = 1;
        return uncheckedCast(this);
    }

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int2> int2(@NonNull AttributeType.Integer type) {
        this.type = type;
        this.components = 2;
        return uncheckedCast(this);
    }

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int3> int3(@NonNull AttributeType.Integer type) {
        this.type = type;
        this.components = 3;
        return uncheckedCast(this);
    }

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int4> int4(@NonNull AttributeType.Integer type) {
        this.type = type;
        this.components = 4;
        return uncheckedCast(this);
    }

    //
    // InterpretationSelectionStage
    //

    @Override
    public AttributeBuilder<Attribute> interpretation(@NonNull AttributeInterpretation interpretation) {
        this.interpretation = interpretation;
        return uncheckedCast(this);
    }

    //
    // VertexAttributeBuilder
    //

    @Override
    public Attribute build() {
        return this.formatBuilder.gl.attributeGenerator().get(this);
    }
}
