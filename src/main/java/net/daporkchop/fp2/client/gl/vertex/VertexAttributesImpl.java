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

package net.daporkchop.fp2.client.gl.vertex;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.type.Int2_10_10_10_Rev;

import java.util.Map;
import java.util.function.Function;

import static net.daporkchop.fp2.client.gl.OpenGL.*;

/**
 * Dummy container class for all {@link IVertexAttribute} implementations.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class VertexAttributesImpl {
    public static final Map<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int1>, IVertexAttribute.Int1>> FACTORIES_INT1
            = ImmutableMap.<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int1>, IVertexAttribute.Int1>>builder()
            .put(VertexAttributeType.BYTE, ByteInt1::new)
            .put(VertexAttributeType.UNSIGNED_BYTE, ByteInt1::new)
            .put(VertexAttributeType.SHORT, ShortInt1::new)
            .put(VertexAttributeType.UNSIGNED_SHORT, ShortInt1::new)
            .put(VertexAttributeType.INT, IntInt1::new)
            .put(VertexAttributeType.UNSIGNED_INT, IntInt1::new)
            .build();

    public static final Map<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int2>, IVertexAttribute.Int2>> FACTORIES_INT2
            = ImmutableMap.<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int2>, IVertexAttribute.Int2>>builder()
            .put(VertexAttributeType.BYTE, ByteInt2::new)
            .put(VertexAttributeType.UNSIGNED_BYTE, ByteInt2::new)
            .put(VertexAttributeType.SHORT, ShortInt2::new)
            .put(VertexAttributeType.UNSIGNED_SHORT, ShortInt2::new)
            .put(VertexAttributeType.INT, IntInt2::new)
            .put(VertexAttributeType.UNSIGNED_INT, IntInt2::new)
            .build();

    public static final Map<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int3>, IVertexAttribute.Int3>> FACTORIES_INT3
            = ImmutableMap.<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int3>, IVertexAttribute.Int3>>builder()
            .put(VertexAttributeType.BYTE, ByteInt3::new)
            .put(VertexAttributeType.UNSIGNED_BYTE, ByteInt3::new)
            .put(VertexAttributeType.SHORT, ShortInt3::new)
            .put(VertexAttributeType.UNSIGNED_SHORT, ShortInt3::new)
            .put(VertexAttributeType.INT, IntInt3::new)
            .put(VertexAttributeType.UNSIGNED_INT, IntInt3::new)
            .build();

    public static final Map<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int4>, IVertexAttribute.Int4>> FACTORIES_INT4
            = ImmutableMap.<VertexAttributeType, Function<VertexAttributeBuilder<IVertexAttribute.Int4>, IVertexAttribute.Int4>>builder()
            .put(VertexAttributeType.BYTE, ByteInt4::new)
            .put(VertexAttributeType.UNSIGNED_BYTE, ByteInt4::new)
            .put(VertexAttributeType.SHORT, ShortInt4::new)
            .put(VertexAttributeType.UNSIGNED_SHORT, ShortInt4::new)
            .put(VertexAttributeType.INT, IntInt4::new)
            .put(VertexAttributeType.UNSIGNED_INT, IntInt4::new)
            .put(VertexAttributeType.INT_2_10_10_10_REV, Int_2_10_10_10_RevInt4::new)
            .put(VertexAttributeType.UNSIGNED_INT_2_10_10_10_REV, Int_2_10_10_10_RevInt4::new)
            .build();

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt1 extends AbstractVertexAttribute<IVertexAttribute.Int1> implements IVertexAttribute.Int1 {
        public ByteInt1(@NonNull VertexAttributeBuilder<Int1> builder) {
            super(builder.ensureType(VertexAttributeType.BYTE, VertexAttributeType.UNSIGNED_BYTE));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0) {
            buf.setByte(vertexBase + this.offset, v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt1 extends AbstractVertexAttribute<IVertexAttribute.Int1> implements IVertexAttribute.Int1 {
        public ShortInt1(@NonNull VertexAttributeBuilder<Int1> builder) {
            super(builder.ensureType(VertexAttributeType.SHORT, VertexAttributeType.UNSIGNED_SHORT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0) {
            buf.setShortLE(vertexBase + this.offset, v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt1 extends AbstractVertexAttribute<IVertexAttribute.Int1> implements IVertexAttribute.Int1 {
        public IntInt1(@NonNull VertexAttributeBuilder<Int1> builder) {
            super(builder.ensureType(VertexAttributeType.INT, VertexAttributeType.UNSIGNED_INT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0) {
            buf.setIntLE(vertexBase + this.offset, v0);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt2 extends AbstractVertexAttribute<IVertexAttribute.Int2> implements IVertexAttribute.Int2 {
        public ByteInt2(@NonNull VertexAttributeBuilder<Int2> builder) {
            super(builder.ensureType(VertexAttributeType.BYTE, VertexAttributeType.UNSIGNED_BYTE));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1) {
            buf.setShortLE(vertexBase + this.offset, (v0 & 0xFF) | (v1 << 8));
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt2 extends AbstractVertexAttribute<IVertexAttribute.Int2> implements IVertexAttribute.Int2 {
        public ShortInt2(@NonNull VertexAttributeBuilder<Int2> builder) {
            super(builder.ensureType(VertexAttributeType.SHORT, VertexAttributeType.UNSIGNED_SHORT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1) {
            buf.setIntLE(vertexBase + this.offset, (v0 & 0xFFFF) | (v1 << 16));
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt2 extends AbstractVertexAttribute<IVertexAttribute.Int2> implements IVertexAttribute.Int2 {
        public IntInt2(@NonNull VertexAttributeBuilder<Int2> builder) {
            super(builder.ensureType(VertexAttributeType.INT, VertexAttributeType.UNSIGNED_INT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1) {
            int idx = vertexBase + this.offset;
            buf.setIntLE(idx, v0).setIntLE(idx + INT_SIZE, v1);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt3 extends AbstractVertexAttribute<IVertexAttribute.Int3> implements IVertexAttribute.Int3 {
        public ByteInt3(@NonNull VertexAttributeBuilder<Int3> builder) {
            super(builder.ensureType(VertexAttributeType.BYTE, VertexAttributeType.UNSIGNED_BYTE));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2) {
            //this weird syntax should allow optimal efficiency due to out-of-order execution
            buf.setMediumLE(vertexBase + this.offset, ((v0 & 0xFF) | (v2 << 16)) | ((v1 & 0xFF) << 8));
        }

        @Override
        public void setRGB(@NonNull ByteBuf buf, int vertexBase, int val) {
            //big-endian write here is intentional
            buf.setMedium(vertexBase + this.offset, val);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt3 extends AbstractVertexAttribute<IVertexAttribute.Int3> implements IVertexAttribute.Int3 {
        public ShortInt3(@NonNull VertexAttributeBuilder<Int3> builder) {
            super(builder.ensureType(VertexAttributeType.SHORT, VertexAttributeType.UNSIGNED_SHORT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2) {
            int idx = vertexBase + this.offset;
            buf.setIntLE(idx, (v0 & 0xFFFF) | (v1 << 16)).setShortLE(idx + INT_SIZE, v2);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt3 extends AbstractVertexAttribute<IVertexAttribute.Int3> implements IVertexAttribute.Int3 {
        public IntInt3(@NonNull VertexAttributeBuilder<Int3> builder) {
            super(builder.ensureType(VertexAttributeType.INT, VertexAttributeType.UNSIGNED_INT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2) {
            int idx = vertexBase + this.offset;
            buf.setIntLE(idx, v0).setIntLE(idx + INT_SIZE, v1).setIntLE(idx + 2 * INT_SIZE, v2);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt4 extends AbstractVertexAttribute<IVertexAttribute.Int4> implements IVertexAttribute.Int4 {
        public ByteInt4(@NonNull VertexAttributeBuilder<Int4> builder) {
            super(builder.ensureType(VertexAttributeType.BYTE, VertexAttributeType.UNSIGNED_BYTE));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2, int v3) {
            //this weird syntax should allow optimal efficiency due to out-of-order execution
            buf.setIntLE(vertexBase + this.offset, ((v0 & 0xFF) | (v3 << 24)) | ((v1 & 0xFF) << 8) | ((v2 & 0xFF) << 16));
        }

        @Override
        public void setARGB(@NonNull ByteBuf buf, int vertexBase, int val) {
            //big-endian write here is intentional
            buf.setInt(vertexBase + this.offset, val);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShortInt4 extends AbstractVertexAttribute<IVertexAttribute.Int4> implements IVertexAttribute.Int4 {
        public ShortInt4(@NonNull VertexAttributeBuilder<Int4> builder) {
            super(builder.ensureType(VertexAttributeType.SHORT, VertexAttributeType.UNSIGNED_SHORT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2, int v3) {
            int idx = vertexBase + this.offset;
            buf.setIntLE(idx, (v0 & 0xFFFF) | (v1 << 16)).setIntLE(idx + INT_SIZE, (v2 & 0xFFFF) | (v3 << 16));
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class IntInt4 extends AbstractVertexAttribute<IVertexAttribute.Int4> implements IVertexAttribute.Int4 {
        public IntInt4(@NonNull VertexAttributeBuilder<Int4> builder) {
            super(builder.ensureType(VertexAttributeType.INT, VertexAttributeType.UNSIGNED_INT));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2, int v3) {
            int idx = vertexBase + this.offset;
            buf.setIntLE(idx, v0).setIntLE(idx + INT_SIZE, v1).setIntLE(idx + 2 * INT_SIZE, v2).setIntLE(idx + 3 * INT_SIZE, v3);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Int_2_10_10_10_RevInt4 extends AbstractVertexAttribute<IVertexAttribute.Int4> implements IVertexAttribute.Int4 {
        public Int_2_10_10_10_RevInt4(@NonNull VertexAttributeBuilder<Int4> builder) {
            super(builder.ensureType(VertexAttributeType.INT_2_10_10_10_REV, VertexAttributeType.UNSIGNED_INT_2_10_10_10_REV));
        }

        @Override
        public void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2, int v3) {
            this.setInt2_10_10_10_rev(buf, vertexBase, Int2_10_10_10_Rev.packUnsignedXYZWUnsafe(v0, v1, v2, v3));
        }

        @Override
        public void setInt2_10_10_10_rev(@NonNull ByteBuf buf, int vertexBase, int val) {
            buf.setIntLE(vertexBase + this.offset, val);
        }

        @Override
        public void setUnsignedInt2_10_10_10_rev(@NonNull ByteBuf buf, int vertexBase, int val) {
            buf.setIntLE(vertexBase + this.offset, val);
        }
    }
}
