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

package net.daporkchop.fp2.gl.opengl.layout.binding;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BaseAttributeBuffer;
import net.daporkchop.fp2.gl.layout.binding.BaseBinding;
import net.daporkchop.fp2.gl.layout.binding.BaseBindingBuilder;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class BaseBindingBuilderImpl<BUILDER extends BaseBindingBuilder<BUILDER, B>, B extends BaseBinding, L extends BaseLayoutImpl> implements BaseBindingBuilder<BUILDER, B> {
    @NonNull
    protected final L layout;

    protected final ImmutableList.Builder<BindingEntry> entries = ImmutableList.builder();

    protected int index = 0;

    @Override
    public BUILDER with(@NonNull AttributeUsage usage, @NonNull BaseAttributeBuffer buffer) {
        LayoutEntry<?> entry = this.layout.entries().get(this.index++);
        checkArg(usage == entry.usage(), "cannot bind buffer %s as %s when the layout declares it as %s!", buffer, usage, entry.usage());
        checkArg(buffer.format() == entry.format(), "cannot bind buffer %s with format %s when the layout declares it as %s!", buffer, buffer.format(), entry.format());

        this.entries.add(new BindingEntry(entry, (BaseAttributeBufferImpl<?>) buffer));
        return uncheckedCast(this);
    }

    @Override
    public BUILDER with(@NonNull B binding) {
        ((BaseBindingImpl<?>) binding).entries.forEach(entry -> this.with(entry.layoutEntry().usage(), entry.buffer()));
        return uncheckedCast(this);
    }
}
