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

package net.daporkchop.fp2.gl.opengl.vertex;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.vertex.VertexAttribute;
import net.daporkchop.fp2.gl.vertex.VertexAttributeBuilder;
import net.daporkchop.fp2.gl.vertex.VertexAttributeInterpretation;
import net.daporkchop.fp2.gl.vertex.VertexAttributeType;

import java.util.function.Function;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VertexAttributeBuilderImpl implements VertexAttributeBuilder.NameSelectionStage, VertexAttributeBuilder.TypeSelectionStage, VertexAttributeBuilder.InterpretationSelectionStage<VertexAttribute>, VertexAttributeBuilder<VertexAttribute> {
    @NonNull
    protected final VertexFormatBuilderImpl formatBuilder;

    protected String name;
    protected VertexAttributeType type;
    protected Function<VertexAttributeBuilderImpl, VertexAttributeImpl> finisher;
    protected VertexAttributeInterpretation interpretation;

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
    public VertexAttributeBuilder.InterpretationSelectionStage<VertexAttribute.Int1> int1(@NonNull VertexAttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = VertexAttributeImpl.ByteInt1::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = VertexAttributeImpl.ShortInt1::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = VertexAttributeImpl.IntInt1::new;
                break;
        }

        return uncheckedCast(this);
    }

    @Override
    public VertexAttributeBuilder.InterpretationSelectionStage<VertexAttribute.Int2> int2(@NonNull VertexAttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = VertexAttributeImpl.ByteInt2::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = VertexAttributeImpl.ShortInt2::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = VertexAttributeImpl.IntInt2::new;
                break;
        }

        return uncheckedCast(this);
    }

    @Override
    public VertexAttributeBuilder.InterpretationSelectionStage<VertexAttribute.Int3> int3(@NonNull VertexAttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = VertexAttributeImpl.ByteInt3::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = VertexAttributeImpl.ShortInt3::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = VertexAttributeImpl.IntInt3::new;
                break;
        }

        return uncheckedCast(this);
    }

    @Override
    public VertexAttributeBuilder.InterpretationSelectionStage<VertexAttribute.Int4> int4(@NonNull VertexAttributeType.Integer type) {
        this.type = type;

        switch (type) {
            case BYTE:
            case UNSIGNED_BYTE:
                this.finisher = VertexAttributeImpl.ByteInt4::new;
                break;
            case SHORT:
            case UNSIGNED_SHORT:
                this.finisher = VertexAttributeImpl.ShortInt4::new;
                break;
            case INT:
            case UNSIGNED_INT:
                this.finisher = VertexAttributeImpl.IntInt4::new;
                break;
        }

        return uncheckedCast(this);
    }

    //
    // InterpretationSelectionStage
    //

    @Override
    public VertexAttributeBuilder<VertexAttribute> interpretation(@NonNull VertexAttributeInterpretation interpretation) {
        this.interpretation = interpretation;
        return uncheckedCast(this);
    }

    //
    // VertexAttributeBuilder
    //

    @Override
    public VertexAttribute build() {
        return this.finisher.apply(this);
    }
}
