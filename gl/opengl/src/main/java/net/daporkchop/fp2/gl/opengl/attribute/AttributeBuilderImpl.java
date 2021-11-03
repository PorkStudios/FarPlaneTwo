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
    protected Function<AttributeBuilderImpl, AttributeImpl> finisher;
    protected AttributeInterpretation interpretation;

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

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = AttributeImpl.ByteInt1::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = AttributeImpl.ShortInt1::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = AttributeImpl.IntInt1::new;
                break;
        }

        return uncheckedCast(this);
    }

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int2> int2(@NonNull AttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = AttributeImpl.ByteInt2::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = AttributeImpl.ShortInt2::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = AttributeImpl.IntInt2::new;
                break;
        }

        return uncheckedCast(this);
    }

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int3> int3(@NonNull AttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = AttributeImpl.ByteInt3::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = AttributeImpl.ShortInt3::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = AttributeImpl.IntInt3::new;
                break;
        }

        return uncheckedCast(this);
    }

    @Override
    public AttributeBuilder.InterpretationSelectionStage<Attribute.Int4> int4(@NonNull AttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = AttributeImpl.ByteInt4::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = AttributeImpl.ShortInt4::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = AttributeImpl.IntInt4::new;
                break;
        }

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
        return this.finisher.apply(this);
    }
}
