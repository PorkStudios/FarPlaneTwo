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

package net.daporkchop.fp2.gl.opengl.layout;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import net.daporkchop.fp2.gl.draw.DrawBindingBuilder;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.draw.DrawBindingBuilderImpl;
import net.daporkchop.fp2.gl.opengl.vertex.VertexAttributeImpl;
import net.daporkchop.fp2.gl.opengl.vertex.VertexBufferImpl;
import net.daporkchop.fp2.gl.opengl.vertex.VertexFormatImpl;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;

import java.util.Map;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class DrawLayoutImpl extends BaseLayoutImpl implements DrawLayout {
    protected final Map<String, AttributeBinding> attributeBindings = new ObjObjOpenHashMap<>();

    public DrawLayoutImpl(@NonNull DrawLayoutBuilderImpl builder) {
        super(builder);

        //register all locals as standard vertex attributes
        for (VertexFormatImpl format : builder.locals) {
            format.attribs().forEach((name, attrib) -> this.registerAttributeBinding(new AttributeBindingStandard(format, (VertexAttributeImpl) attrib)));
        }
    }

    protected void registerAttributeBinding(@NonNull DrawLayoutImpl.AttributeBindingStandard binding) {
        checkArg(binding.bindingIndex < 0, binding);

        int index = this.attributeBindings.size();
        binding.bindingIndex = index;
        int max = this.api.glGetInteger(GL_MAX_VERTEX_ATTRIBS);
        checkIndex(index < max, "cannot use more than %d vertex attributes!", max);

        String name = binding.attrib().name();
        checkArg(this.attributeBindings.putIfAbsent(name, binding) != null, "vertex attribute name registered more than once: %s", name);
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public void configureProgramPreLink(int program) {
        this.attributeBindings.forEach((name, binding) -> binding.bindAttribLocation(this.api, program));
    }

    @Override
    public void configureProgramPostLink(int program) {
    }

    @Override
    public DrawBindingBuilder.UniformsStage createBinding() {
        return new DrawBindingBuilderImpl(this);
    }

    /**
     * @author DaPorkchop_
     */
    public interface AttributeBinding {
        VertexFormatImpl format();

        void enableAndBind(@NonNull GLAPI api, @NonNull VertexBufferImpl buffer);

        void bindAttribLocation(@NonNull GLAPI api, int program);
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    protected static class AttributeBindingStandard implements AttributeBinding {
        @NonNull
        protected final VertexFormatImpl format;
        @NonNull
        protected final VertexAttributeImpl attrib;

        protected int bindingIndex = -1;

        @Override
        public void enableAndBind(@NonNull GLAPI api, @NonNull VertexBufferImpl buffer) {
            api.glEnableVertexAttribArray(this.bindingIndex);
            buffer.bindAttribute(api, this.bindingIndex, this.attrib);
        }

        @Override
        public void bindAttribLocation(@NonNull GLAPI api, int program) {
            api.glBindAttribLocation(program, this.bindingIndex, this.attrib.name());
        }
    }

    /**
     * @author DaPorkchop_
     */
    @ToString(callSuper = true)
    protected static class AttributeBindingInstanced extends AttributeBindingStandard {
        public AttributeBindingInstanced(@NonNull VertexFormatImpl format, @NonNull VertexAttributeImpl attrib) {
            super(format, attrib);
        }

        @Override
        public void enableAndBind(@NonNull GLAPI api, @NonNull VertexBufferImpl buffer) {
            super.enableAndBind(api, buffer);

            api.glVertexAttribDivisor(this.bindingIndex, 1);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Instanced extends DrawLayoutImpl {
        public Instanced(@NonNull DrawLayoutBuilderImpl builder) {
            super(builder);

            //register all globals as instanced vertex attributes
            for (VertexFormatImpl format : builder.globals) {
                format.attribs().forEach((name, attrib) -> this.registerAttributeBinding(new AttributeBindingInstanced(format, (VertexAttributeImpl) attrib)));
            }
        }
    }
}
