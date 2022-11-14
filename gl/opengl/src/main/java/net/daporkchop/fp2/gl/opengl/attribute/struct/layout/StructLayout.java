/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.struct.layout;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.AttributeType;

/**
 * @author DaPorkchop_
 */
@SuperBuilder
@Data
public abstract class StructLayout<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
    private final StructInfo<?> structInfo;
    private final String layoutName;

    @NonNull
    private final M member;

    public AttributeType structProperty() {
        return this.structInfo().property();
    }

    /**
     * @author DaPorkchop_
     */
    public interface Member<M extends Member<M, C>, C extends Component> {
        int components();

        C component(int componentIndex);

        int children();

        M child(int childIndex);
    }

    /**
     * @author DaPorkchop_
     */
    public interface Component {
        long offset();

        LayoutComponentStorage storage();
    }
}
