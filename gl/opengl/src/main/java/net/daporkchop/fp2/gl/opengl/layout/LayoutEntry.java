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
 *
 */

package net.daporkchop.fp2.gl.opengl.layout;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;

import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Data
public final class LayoutEntry<F extends BaseAttributeFormatImpl<?>> {
    @NonNull
    private final AttributeUsage usage;
    @NonNull
    private final F format;

    @NonNull
    private final String prefix;
    @NonNull
    private final String suffix;

    private BindingLocation<?> location;

    public LayoutEntry<F> location(@NonNull BindingLocation<?> location) {
        checkState(this.location == null, "location already set!");
        this.location = location;
        return this;
    }

    public String name() {
        return this.prefix + this.format.rawName() + this.suffix;
    }

    public Stream<GLSLField> attributeFields() {
        return this.format.rawAttributeFields().stream()
                .map(field -> new GLSLField(field.type(), this.prefix + field.name() + this.suffix));
    }
}
