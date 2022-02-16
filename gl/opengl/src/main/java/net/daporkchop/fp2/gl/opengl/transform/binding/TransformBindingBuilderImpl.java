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

package net.daporkchop.fp2.gl.opengl.transform.binding;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BaseAttributeBuffer;
import net.daporkchop.fp2.gl.opengl.layout.binding.BaseBindingBuilderImpl;
import net.daporkchop.fp2.gl.opengl.transform.TransformLayoutImpl;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TransformBindingBuilderImpl extends BaseBindingBuilderImpl<TransformBindingBuilder, TransformBinding, TransformLayoutImpl> implements TransformBindingBuilder {
    private final Set<BaseAttributeBuffer> inputs = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<BaseAttributeBuffer> outputs = Collections.newSetFromMap(new IdentityHashMap<>());

    public TransformBindingBuilderImpl(@NonNull TransformLayoutImpl layout) {
        super(layout);
    }

    @Override
    public TransformBindingBuilder with(@NonNull AttributeUsage usage, @NonNull BaseAttributeBuffer buffer) {
        switch (usage) {
            case TRANSFORM_INPUT:
                checkState(!this.outputs.contains(buffer), "cannot register %s as input when it is already used as an output!", buffer);
                break;
            case TRANSFORM_OUTPUT:
                checkState(!this.inputs.contains(buffer), "cannot register %s as output when it is already used as an input!", buffer);
                break;
        }

        super.with(usage, buffer);

        switch (usage) {
            case TRANSFORM_INPUT:
                this.inputs.add(buffer);
                break;
            case TRANSFORM_OUTPUT:
                this.outputs.add(buffer);
                break;
        }

        return this;
    }

    @Override
    public TransformBinding build() {
        return new TransformBindingImpl(this);
    }
}
