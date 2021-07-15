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

package net.daporkchop.fp2.mode.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.IReusablePersistent;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.lib.compression.zstd.Zstd;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A wrapper around a compressed {@link IReusablePersistent} value.
 *
 * @author DaPorkchop_
 */
@Getter
public final class Compressed<POS extends IFarPos, V extends IReusablePersistent> {
    /**
     * Timestamp indicating that the tile does not contain any data.
     */
    public static final long VALUE_BLANK = Long.MIN_VALUE;

    /**
     * Timestamp indicating that the tile's rough generation has been completed.
     */
    public static final long TIMESTAMP_GENERATED = 0L;

    protected final POS pos;
    protected long extra;
    protected byte[] data;

    protected long timestamp = VALUE_BLANK;

    /**
     * Creates a new compressed value with no data at the given tile position.
     *
     * @param pos the position of the value
     */
    public Compressed(@NonNull POS pos) {
        this.pos = pos;
    }

    /**
     * Reads a value prefixed with the tile position from the given {@link ByteBuf}.
     *
     * @param src  the {@link ByteBuf} to read from
     * @param mode the {@link IFarRenderMode} that the value is used by
     */
    public Compressed(@NonNull ByteBuf src, @NonNull IFarRenderMode<POS, ?> mode) {
        this(uncheckedCast(mode.readPos(src)), src);
    }

    /**
     * Reads a value with no prefix from the given {@link ByteBuf}.
     *
     * @param pos the tile position
     * @param src the {@link ByteBuf} to read from
     */
    public Compressed(@NonNull POS pos, @NonNull ByteBuf src) {
        this(pos);

        this.extra = readVarLong(src);
        this.timestamp = readVarLongZigZag(src);

        int len = readVarInt(src) - 1;
        if (len >= 0) { //if length is -1 (0 on disk) there is no data
            src.readBytes(this.data = new byte[len]);
        }
    }

    /**
     * Writes this value to the given {@link ByteBuf}, prefixed with the tile position.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    public void writeWithPos(@NonNull ByteBuf dst) {
        this.pos.writePos(dst);
        this.write(dst);
    }

    /**
     * Writes this value to the given {@link ByteBuf} with no prefix.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    public void write(@NonNull ByteBuf dst) {
        long extra;
        long timestamp;
        byte[] data;

        synchronized (this) { //lock to ensure we get a consistent view of the data
            extra = this.extra;
            timestamp = this.timestamp;
            data = this.data;
        }

        writeVarLong(dst, extra);
        writeVarLongZigZag(dst, timestamp);

        if (data == null) {
            writeVarInt(dst, 0);
        } else {
            writeVarInt(dst, data.length + 1);
            dst.writeBytes(data);
        }
    }

    /**
     * @return whether or not this value is blank (i.e. has not been generated)
     * @deprecated use the inverted result of {@link #isGenerated()}
     */
    @Deprecated
    public boolean isBlank() {
        return this.timestamp() == Compressed.VALUE_BLANK;
    }

    /**
     * @return whether or not this value is done (i.e. has been fully generated and may be saved)
     */
    public boolean isGenerated() {
        return this.timestamp() != VALUE_BLANK;
    }

    /**
     * @return whether or not this value is empty, or has been generated but still has no contents
     */
    public boolean isEmpty() {
        return this.data == null;
    }

    private V inflateValue0(@NonNull SimpleRecycler<V> recycler, byte[] data) {
        if (data != null) {
            ByteBuf compressed = Unpooled.wrappedBuffer(data);
            ByteBuf uncompressed = Constants.allocateByteBufExactly(Zstd.PROVIDER.frameContentSize(compressed));
            try {
                checkState(ZSTD_INF.get().decompress(compressed, uncompressed));
                V value = recycler.allocate();
                value.read(uncompressed);
                return value;
            } finally {
                uncompressed.release();
                //no reason to release the wrapped buffer lmao
            }
        } else {
            return null;
        }
    }

    /**
     * @return the inflated, parsed value
     */
    public V inflateValue(@NonNull SimpleRecycler<V> recycler) {
        return this.inflateValue0(recycler, this.data);
    }

    /**
     * @return the inflated, parsed value
     */
    public Handle<POS, V> inflateHandle(@NonNull SimpleRecycler<V> recycler) {
        long extra;
        long timestamp;
        byte[] data;

        synchronized (this) { //lock to ensure we get a consistent view of the data
            checkState(this.isGenerated(), "value hasn't been generated!");

            extra = this.extra;
            timestamp = this.timestamp;
            data = this.data;
        }

        return new Handle<>(this.pos, this.inflateValue0(recycler, data), extra, timestamp, recycler);
    }

    public boolean set(long timestamp, @NonNull V value, long extra) throws IllegalArgumentException {
        if (this.timestamp >= timestamp) { //break out early in the event of a timestamp mismatch, to avoid wasting CPU time on unneeded compression
            return false;
        }

        //compress the value data into a byte[]
        byte[] data = null;
        ByteBuf compressed = null;
        COMPRESS:
        try {
            ByteBuf uncompressed = ByteBufAllocator.DEFAULT.buffer();
            try {
                if (value.write(uncompressed)) { //write value
                    break COMPRESS; //skip compression if value is empty
                }
                compressed = Constants.allocateByteBufExactly(Zstd.PROVIDER.compressBound(uncompressed.readableBytes())); //allocate dst buffer
                checkState(ZSTD_DEF.get().compress(uncompressed, compressed)); //compress value
            } finally {
                uncompressed.release(); //discard uncompressed data
            }

            compressed.readBytes(data = new byte[compressed.readableBytes()]);
        } finally {
            if (compressed != null) {
                compressed.release();
            }
        }

        synchronized (this) { //lock to ensure we get a consistent view of the data
            if (timestamp > this.timestamp) {
                this.timestamp = timestamp;
                this.extra = extra;
                this.data = data;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * A snapshot of the decompressed data in a {@link Compressed}.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    public static final class Handle<POS extends IFarPos, V extends IReusablePersistent> implements AutoCloseable {
        @NonNull
        protected final POS pos;
        protected final V value;
        protected final long extra;
        protected final long timestamp;

        @Getter(AccessLevel.NONE)
        protected final SimpleRecycler<V> recycler;

        @Override
        public void close() {
            if (this.value != null && this.recycler != null) {
                this.recycler.release(this.value);
            }
        }
    }
}
