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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.vertex.VertexAttribute;
import net.daporkchop.fp2.gl.vertex.VertexAttributeInterpretation;
import net.daporkchop.fp2.gl.vertex.VertexAttributeType;
import net.daporkchop.fp2.gl.vertex.VertexWriter;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class VertexAttributeImpl implements VertexAttribute {
    protected final String name;
    protected final VertexAttributeType type;
    protected final VertexAttributeInterpretation interpretation;

    @Getter(AccessLevel.NONE)
    protected final int index;
    @Getter(AccessLevel.NONE)
    protected final int offset;
    protected final int size;

    protected final int components;
    @Getter(AccessLevel.NONE)
    protected final int reportedComponents;

    public VertexAttributeImpl(@NonNull VertexAttributeBuilderImpl builder, int components) {
        this.name = builder.name;
        this.type = builder.type;
        this.interpretation = builder.interpretation;

        this.components = components;
        this.reportedComponents = builder.reportedComponents >= 0 ? builder.reportedComponents : components;

        VertexFormatBuilderImpl formatBuilder = builder.formatBuilder;
        this.index = formatBuilder.addAttribute(this);
        this.size = formatBuilder.computeSize(this);
        this.offset = formatBuilder.computeOffset(this);
    }

    public void bind(@NonNull GLAPI api, int bindingIndex, int offset, int stride) {
        int type = GLEnumUtil.from(this.type);

        switch (this.interpretation) {
            case INTEGER:
                api.glVertexAttribIPointer(bindingIndex, this.reportedComponents, type, stride, offset);
                return;
            case FLOAT:
            case NORMALIZED_FLOAT:
                api.glVertexAttribPointer(bindingIndex, this.reportedComponents, type, this.interpretation == VertexAttributeInterpretation.NORMALIZED_FLOAT, stride, offset);
                return;
            default:
                throw new IllegalArgumentException(this.interpretation.toString());
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static abstract class Int1 extends VertexAttributeImpl implements VertexAttribute.Int1 {
        public Int1(@NonNull VertexAttributeBuilderImpl builder, @NonNull VertexAttributeType.Integer... acceptableTypes) {
            super(builder, 1);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see VertexWriter#set(VertexAttribute.Int1, int)
         */
        public abstract void set(Object base, long offset, int v0);
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt1 extends Int1 {
        public ByteInt1(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.BYTE, VertexAttributeType.Integer.UNSIGNED_BYTE);
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
        public ShortInt1(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.SHORT, VertexAttributeType.Integer.UNSIGNED_SHORT);
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
        public IntInt1(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.INT, VertexAttributeType.Integer.UNSIGNED_INT);
        }

        @Override
        public void set(Object base, long offset, int v0) {
            PUnsafe.putInt(base, offset, v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static abstract class Int2 extends VertexAttributeImpl implements VertexAttribute.Int2 {
        public Int2(@NonNull VertexAttributeBuilderImpl builder, @NonNull VertexAttributeType.Integer... acceptableTypes) {
            super(builder, 2);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see VertexWriter#set(VertexAttribute.Int2, int, int)
         */
        public abstract void set(Object base, long offset, int v0, int v1);
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt2 extends Int2 {
        public ByteInt2(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.BYTE, VertexAttributeType.Integer.UNSIGNED_BYTE);
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
        public ShortInt2(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.SHORT, VertexAttributeType.Integer.UNSIGNED_SHORT);
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
        public IntInt2(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.INT, VertexAttributeType.Integer.UNSIGNED_INT);
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
    protected static abstract class Int3 extends VertexAttributeImpl implements VertexAttribute.Int3 {
        public Int3(@NonNull VertexAttributeBuilderImpl builder, @NonNull VertexAttributeType.Integer... acceptableTypes) {
            super(builder, 3);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see VertexWriter#set(VertexAttribute.Int3, int, int, int)
         */
        public abstract void set(Object base, long offset, int v0, int v1, int v2);

        /**
         * @see VertexWriter#setARGB(VertexAttribute.Int3, int)
         */
        public void setARGB(Object base, long offset, int argb) {
            this.set(base, offset, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt3 extends Int3 {
        public ByteInt3(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.BYTE, VertexAttributeType.Integer.UNSIGNED_BYTE);
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
        public ShortInt3(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.SHORT, VertexAttributeType.Integer.UNSIGNED_SHORT);
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
        public IntInt3(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.INT, VertexAttributeType.Integer.UNSIGNED_INT);
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
    protected static abstract class Int4 extends VertexAttributeImpl implements VertexAttribute.Int4 {
        public Int4(@NonNull VertexAttributeBuilderImpl builder, @NonNull VertexAttributeType.Integer... acceptableTypes) {
            super(builder, 4);

            checkState(Stream.of(acceptableTypes).anyMatch(type -> type == builder.type), "tried to construct %s with invalid type %s", this.getClass().getTypeName(), builder.type);
        }

        /**
         * @see VertexWriter#set(VertexAttribute.Int4, int, int, int, int)
         */
        public abstract void set(Object base, long offset, int v0, int v1, int v2, int v3);

        /**
         * @see VertexWriter#setARGB(VertexAttribute.Int4, int)
         */
        public void setARGB(Object base, long offset, int argb) {
            this.set(base, offset, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt4 extends Int4 {
        public ByteInt4(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.BYTE, VertexAttributeType.Integer.UNSIGNED_BYTE);
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
        public ShortInt4(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.SHORT, VertexAttributeType.Integer.UNSIGNED_SHORT);
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
        public IntInt4(@NonNull VertexAttributeBuilderImpl builder) {
            super(builder, VertexAttributeType.Integer.INT, VertexAttributeType.Integer.UNSIGNED_INT);
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
