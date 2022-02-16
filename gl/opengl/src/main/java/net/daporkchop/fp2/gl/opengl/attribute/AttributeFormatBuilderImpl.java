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

package net.daporkchop.fp2.gl.opengl.attribute;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Data
public class AttributeFormatBuilderImpl<S> implements AttributeFormatBuilder<S> {
    @NonNull
    @EqualsAndHashCode.Exclude
    protected final OpenGL gl;
    @NonNull
    protected final Class<S> clazz;

    protected final Set<AttributeUsage> usages = EnumSet.noneOf(AttributeUsage.class);
    protected final Map<String, String> nameOverrides = new HashMap<>();

    @Override
    public AttributeFormatBuilder<S> rename(@NonNull String originalName, @NonNull String newName) {
        checkState(this.nameOverrides.putIfAbsent(originalName, newName) == null, "name %s cannot be overridden twice!", originalName);
        return this;
    }

    @Override
    public AttributeFormatBuilder<S> useFor(@NonNull AttributeUsage usage) {
        this.usages.add(usage);
        return this;
    }

    @Override
    public AttributeFormatBuilder<S> useFor(@NonNull AttributeUsage... usages) {
        this.usages.addAll(Arrays.asList(usages));
        return this;
    }

    @Override
    public AttributeFormat<S> build() {
        return uncheckedCast(this.gl.attributeFormatCache().getUnchecked(this));
    }

    public StructInfo<S> structInfo() {
        return new StructInfo<>(this.clazz, this.nameOverrides);
    }
}
