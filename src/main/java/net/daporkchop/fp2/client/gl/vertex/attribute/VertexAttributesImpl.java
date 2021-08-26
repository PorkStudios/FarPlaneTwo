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

package net.daporkchop.fp2.client.gl.vertex.attribute;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;
import java.util.function.Function;

import static net.daporkchop.fp2.client.gl.OpenGL.*;

/**
 * Dummy container class for all {@link IVertexAttribute} implementations.
 *
 * @author DaPorkchop_
 */
@UtilityClass
//TODO: i'm not certain about a number of the big-endian implementations here
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
            .build();

    /**
     * @author DaPorkchop_
     */
    public static class ByteInt1 extends AbstractVertexAttribute<IVertexAttribute.Int1> implements IVertexAttribute.Int1 {
        public ByteInt1(@NonNull VertexAttributeBuilder<Int1> builder) {
            super(builder.ensureType(VertexAttributeType.BYTE, VertexAttributeType.UNSIGNED_BYTE));
        }

        @Override
        public void set(long addr, int v0) {
            PUnsafe.putByte(addr, (byte) v0);
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
        public void set(long addr, int v0) {
            PUnsafe.putShort(addr, (short) v0);
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
        public void set(long addr, int v0) {
            PUnsafe.putInt(addr, v0);
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
        public void set(long addr, int v0, int v1) {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                PUnsafe.putShort(addr, (short) ((v0 & 0xFF) | (v1 << 8)));
            } else {
                PUnsafe.putShort(addr, (short) ((v1 & 0xFF) | (v0 << 8)));
            }
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
        public void set(long addr, int v0, int v1) {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                PUnsafe.putInt(addr, (v0 & 0xFFFF) | (v1 << 16));
            } else {
                PUnsafe.putInt(addr, (v1 & 0xFFFF) | (v0 << 16));
            }
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
        public void set(long addr, int v0, int v1) {
            PUnsafe.putInt(addr + 0 * INT_SIZE, v0);
            PUnsafe.putInt(addr + 1 * INT_SIZE, v1);
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
        public void set(long addr, int v0, int v1, int v2) {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                PUnsafe.putShort(addr, (short) ((v0 & 0xFF) | (v1 << 8)));
                PUnsafe.putByte(addr + SHORT_SIZE, (byte) v2);
            } else {
                PUnsafe.putShort(addr, (short) ((v1 & 0xFF) | (v0 << 8)));
                PUnsafe.putByte(addr + SHORT_SIZE, (byte) v2);
            }
        }

        @Override
        public void setRGB(long addr, int val) {
            //this is conceptually executed in big-endian order
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                PUnsafe.putShort(addr, Short.reverseBytes((short) (val >>> 8)));
                PUnsafe.putByte(addr + SHORT_SIZE, (byte) val);
            } else {
                PUnsafe.putShort(addr, (short) val);
                PUnsafe.putByte(addr + SHORT_SIZE, (byte) (val >>> 16));
            }
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
        public void set(long addr, int v0, int v1, int v2) {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                PUnsafe.putInt(addr, (v0 & 0xFFFF) | (v1 << 16));
                PUnsafe.putShort(addr + INT_SIZE, (short) v2);
            } else {
                PUnsafe.putInt(addr, (v1 & 0xFFFF) | (v0 << 16));
                PUnsafe.putShort(addr + INT_SIZE, (short) v2);
            }
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
        public void set(long addr, int v0, int v1, int v2) {
            PUnsafe.putInt(addr + 0 * INT_SIZE, v0);
            PUnsafe.putInt(addr + 1 * INT_SIZE, v1);
            PUnsafe.putInt(addr + 2 * INT_SIZE, v2);
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
        public void set(long addr, int v0, int v1, int v2, int v3) {
            PUnsafe.putInt(addr, PlatformInfo.IS_LITTLE_ENDIAN
                    ? ((v0 & 0xFF) | (v3 << 24)) | ((v1 & 0xFF) << 8) | ((v2 & 0xFF) << 16)
                    : ((v3 & 0xFF) | (v0 << 24)) | ((v2 & 0xFF) << 8) | ((v1 & 0xFF) << 16));
        }

        @Override
        public void setARGB(long addr, int val) {
            //this is conceptually executed in big-endian order
            PUnsafe.putInt(addr, PlatformInfo.IS_LITTLE_ENDIAN ? Integer.reverseBytes(val) : val);
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
        public void set(long addr, int v0, int v1, int v2, int v3) {
            if (PlatformInfo.IS_LITTLE_ENDIAN) {
                PUnsafe.putInt(addr, (v0 & 0xFFFF) | (v1 << 16));
                PUnsafe.putInt(addr + INT_SIZE, (v2 & 0xFFFF) | (v3 << 16));
            } else {
                PUnsafe.putInt(addr, (v1 & 0xFFFF) | (v0 << 16));
                PUnsafe.putInt(addr + INT_SIZE, (v3 & 0xFFFF) | (v2 << 16));
            }
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
        public void set(long addr, int v0, int v1, int v2, int v3) {
            PUnsafe.putInt(addr + 0 * INT_SIZE, v0);
            PUnsafe.putInt(addr + 1 * INT_SIZE, v1);
            PUnsafe.putInt(addr + 2 * INT_SIZE, v2);
            PUnsafe.putInt(addr + 3 * INT_SIZE, v3);
        }
    }
}
