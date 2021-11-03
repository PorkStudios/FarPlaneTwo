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
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.AttributeInterpretation;
import net.daporkchop.fp2.gl.attribute.AttributeType;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeWriter;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AttributeImpl implements Attribute {
    protected final String name;
    protected final AttributeType type;
    protected final AttributeInterpretation interpretation;

    protected final int index;
    protected final int size;

    protected final int components;
    @Getter(AccessLevel.NONE)
    protected final int reportedComponents;

    public AttributeImpl(@NonNull AttributeBuilderImpl builder, int components) {
        this.name = builder.name;
        this.type = builder.type;
        this.interpretation = builder.interpretation;

        this.components = components;
        this.reportedComponents = builder.reportedComponents >= 0 ? builder.reportedComponents : components;

        AttributeFormatBuilderImpl formatBuilder = builder.formatBuilder;
        this.index = formatBuilder.addAttribute(this);
        this.size = this.type.size(this.components);
    }

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

    /**
     * @author DaPorkchop_
     */
    public static abstract class Int1 extends AttributeImpl implements Attribute.Int1 {
        public Int1(@NonNull AttributeBuilderImpl builder, @NonNull AttributeType.Integer... acceptableTypes) {
            super(builder, 1);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see LocalAttributeWriter#set(Attribute.Int1, int)
         */
        public abstract void set(Object base, long offset, int v0);
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt1 extends Int1 {
        public ByteInt1(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.BYTE, AttributeType.Integer.UNSIGNED_BYTE);
        }

        @Override
        public void set(Object base, long offset, int v0) {
            PUnsafe.putByte(base, offset, (byte) v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt1 extends Int1 {
        public ShortInt1(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.SHORT, AttributeType.Integer.UNSIGNED_SHORT);
        }

        @Override
        public void set(Object base, long offset, int v0) {
            PUnsafe.putShort(base, offset, (short) v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt1 extends Int1 {
        public IntInt1(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.INT, AttributeType.Integer.UNSIGNED_INT);
        }

        @Override
        public void set(Object base, long offset, int v0) {
            PUnsafe.putInt(base, offset, v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static abstract class Int2 extends AttributeImpl implements Attribute.Int2 {
        public Int2(@NonNull AttributeBuilderImpl builder, @NonNull AttributeType.Integer... acceptableTypes) {
            super(builder, 2);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see LocalAttributeWriter#set(Attribute.Int2, int, int)
         */
        public abstract void set(Object base, long offset, int v0, int v1);
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt2 extends Int2 {
        public ByteInt2(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.BYTE, AttributeType.Integer.UNSIGNED_BYTE);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1) {
            PUnsafe.putByte(base, offset + 0 * BYTE_SIZE, (byte) v0);
            PUnsafe.putByte(base, offset + 1 * BYTE_SIZE, (byte) v1);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt2 extends Int2 {
        public ShortInt2(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.SHORT, AttributeType.Integer.UNSIGNED_SHORT);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1) {
            PUnsafe.putShort(base, offset + 0 * SHORT_SIZE, (short) v0);
            PUnsafe.putShort(base, offset + 1 * SHORT_SIZE, (short) v1);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt2 extends Int2 {
        public IntInt2(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.INT, AttributeType.Integer.UNSIGNED_INT);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1) {
            PUnsafe.putInt(base, offset + 0 * INT_SIZE, v0);
            PUnsafe.putInt(base, offset + 1 * INT_SIZE, v1);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static abstract class Int3 extends AttributeImpl implements Attribute.Int3 {
        public Int3(@NonNull AttributeBuilderImpl builder, @NonNull AttributeType.Integer... acceptableTypes) {
            super(builder, 3);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see LocalAttributeWriter#set(Attribute.Int3, int, int, int)
         */
        public abstract void set(Object base, long offset, int v0, int v1, int v2);

        /**
         * @see LocalAttributeWriter#setARGB(Attribute.Int3, int)
         */
        public void setARGB(Object base, long offset, int argb) {
            this.set(base, offset, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt3 extends Int3 {
        public ByteInt3(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.BYTE, AttributeType.Integer.UNSIGNED_BYTE);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1, int v2) {
            PUnsafe.putByte(base, offset + 0 * BYTE_SIZE, (byte) v0);
            PUnsafe.putByte(base, offset + 1 * BYTE_SIZE, (byte) v1);
            PUnsafe.putByte(base, offset + 2 * BYTE_SIZE, (byte) v2);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt3 extends Int3 {
        public ShortInt3(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.SHORT, AttributeType.Integer.UNSIGNED_SHORT);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1, int v2) {
            PUnsafe.putShort(base, offset + 0 * SHORT_SIZE, (short) v0);
            PUnsafe.putShort(base, offset + 1 * SHORT_SIZE, (short) v1);
            PUnsafe.putShort(base, offset + 2 * SHORT_SIZE, (short) v2);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt3 extends Int3 {
        public IntInt3(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.INT, AttributeType.Integer.UNSIGNED_INT);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1, int v2) {
            PUnsafe.putInt(base, offset + 0 * INT_SIZE, v0);
            PUnsafe.putInt(base, offset + 1 * INT_SIZE, v1);
            PUnsafe.putInt(base, offset + 2 * INT_SIZE, v2);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static abstract class Int4 extends AttributeImpl implements Attribute.Int4 {
        public Int4(@NonNull AttributeBuilderImpl builder, @NonNull AttributeType.Integer... acceptableTypes) {
            super(builder, 4);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see LocalAttributeWriter#set(Attribute.Int4, int, int, int, int)
         */
        public abstract void set(Object base, long offset, int v0, int v1, int v2, int v3);

        /**
         * @see LocalAttributeWriter#setARGB(Attribute.Int4, int)
         */
        public void setARGB(Object base, long offset, int argb) {
            this.set(base, offset, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt4 extends Int4 {
        public ByteInt4(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.BYTE, AttributeType.Integer.UNSIGNED_BYTE);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1, int v2, int v3) {
            PUnsafe.putByte(base, offset + 0 * BYTE_SIZE, (byte) v0);
            PUnsafe.putByte(base, offset + 1 * BYTE_SIZE, (byte) v1);
            PUnsafe.putByte(base, offset + 2 * BYTE_SIZE, (byte) v2);
            PUnsafe.putByte(base, offset + 3 * BYTE_SIZE, (byte) v3);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt4 extends Int4 {
        public ShortInt4(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.SHORT, AttributeType.Integer.UNSIGNED_SHORT);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1, int v2, int v3) {
            PUnsafe.putShort(base, offset + 0 * SHORT_SIZE, (short) v0);
            PUnsafe.putShort(base, offset + 1 * SHORT_SIZE, (short) v1);
            PUnsafe.putShort(base, offset + 2 * SHORT_SIZE, (short) v2);
            PUnsafe.putShort(base, offset + 3 * SHORT_SIZE, (short) v3);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt4 extends Int4 {
        public IntInt4(@NonNull AttributeBuilderImpl builder) {
            super(builder, AttributeType.Integer.INT, AttributeType.Integer.UNSIGNED_INT);
        }

        @Override
        public void set(Object base, long offset, int v0, int v1, int v2, int v3) {
            PUnsafe.putInt(base, offset + 0 * INT_SIZE, v0);
            PUnsafe.putInt(base, offset + 1 * INT_SIZE, v1);
            PUnsafe.putInt(base, offset + 2 * INT_SIZE, v2);
            PUnsafe.putInt(base, offset + 3 * INT_SIZE, v3);
        }
    }
}
