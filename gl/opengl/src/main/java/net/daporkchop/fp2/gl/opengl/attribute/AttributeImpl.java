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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeInterpretation;
import net.daporkchop.fp2.gl.attribute.AttributeType;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AttributeImpl implements Attribute {
    protected final String name;
    protected final AttributeType type;
    protected final AttributeInterpretation interpretation;

    protected AttributeFormatImpl format;

    protected final int index;

    @Getter(AccessLevel.NONE)
    protected final int reportedComponents;

    public AttributeImpl(@NonNull AttributeBuilderImpl builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.interpretation = builder.interpretation;

        this.reportedComponents = builder.reportedComponents >= 0 ? builder.reportedComponents : this.components();

        this.index = builder.formatBuilder.addAttribute(this);
    }

    /**
     * @return the glsl type name of this attribute
     */
    public String glslName() {
        String type;

        switch (this.interpretation) {
            case INTEGER:
                type = this.components() == 1 ? "int" : "ivec" + this.components();
                break;
            case FLOAT:
            case NORMALIZED_FLOAT:
                type = this.components() == 1 ? "float" : "vec" + this.components();
                break;
            default:
                throw new IllegalArgumentException(this.interpretation.toString());
        }

        return type + ' ' + this.name;
    }

    /**
     * @return the packed size of this attribute
     */
    public abstract int packedSize();

    /**
     * @return the unpacked size of this attribute
     */
    public abstract int unpackedSize();

    public void configureVertexAttribute(@NonNull GLAPI api, int bindingIndex, int offset, int stride) {
        int type = GLEnumUtil.from(this.type);

        switch (this.interpretation) {
            case INTEGER:
                api.glVertexAttribIPointer(bindingIndex, this.reportedComponents, type, stride, offset);
                return;
            case FLOAT:
            case NORMALIZED_FLOAT:
                api.glVertexAttribPointer(bindingIndex, this.reportedComponents, type, this.interpretation == AttributeInterpretation.NORMALIZED_FLOAT, stride, offset);
                return;
            default:
                throw new IllegalArgumentException(this.interpretation.toString());
        }
    }

    public abstract void setPacked(Object base, long offset, int v0);

    public abstract void setPacked(Object base, long offset, int v0, int v1);

    public abstract void setPacked(Object base, long offset, int v0, int v1, int v2);

    public abstract void setPacked(Object base, long offset, int v0, int v1, int v2, int v3);

    public abstract void setPackedARGB(Object base, long offset, int argb);

    public abstract void setUnpacked(Object base, long offset, int v0);

    public abstract void setUnpacked(Object base, long offset, int v0, int v1);

    public abstract void setUnpacked(Object base, long offset, int v0, int v1, int v2);

    public abstract void setUnpacked(Object base, long offset, int v0, int v1, int v2, int v3);

    public abstract void setUnpackedARGB(Object base, long offset, int argb);

    public abstract void unpack(Object srcBase, long srcOffset, Object dstBase, long dstOffset);

    public abstract void setUniformFromUnpacked(GLAPI api, int location, Object base, long offset);
}
