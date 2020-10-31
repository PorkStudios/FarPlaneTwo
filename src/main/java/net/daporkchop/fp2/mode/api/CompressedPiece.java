/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.compression.zstd.Zstd;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A wrapper around the compressed body of an {@link IFarPiece}.
 *
 * @author DaPorkchop_
 */
@Getter
public class CompressedPiece<POS extends IFarPos, P extends IFarPiece, B extends IFarPieceBuilder> extends ReentrantReadWriteLock {
    /**
     * Timestamp indicating that the piece does not contain any data.
     */
    public static final long PIECE_BLANK = Long.MIN_VALUE;

    /**
     * Timestamp indicating that the piece's rough generation has been completed.
     */
    public static final long PIECE_ROUGH_COMPLETE = -1L;

    /**
     * Gets a timestamp indicating that the piece contains rough data generated at the given level.
     *
     * @param level the lowest level that has been generated roughly
     * @return a timestamp indicating that the piece contains rough data generated at the given level
     */
    public static long pieceRough(int level) {
        return -level - 1L;
    }

    protected final POS pos;
    protected long extra;
    protected byte[] data;

    protected long timestamp = PIECE_BLANK;

    /**
     * Creates a new compressed piece with no data at the given position.
     *
     * @param pos the position of the piece
     */
    public CompressedPiece(@NonNull POS pos) {
        this.pos = pos;
    }

    /**
     * Reads a piece prefixed with the render mode and the piece position from the given {@link ByteBuf}.
     *
     * @param src the {@link ByteBuf} to read from
     */
    public CompressedPiece(@NonNull ByteBuf src) {
        this(src, RenderMode.fromOrdinal(src.readUnsignedByte()));
    }

    /**
     * Reads a piece prefixed with the piece position from the given {@link ByteBuf}.
     *
     * @param src  the {@link ByteBuf} to read from
     * @param mode the {@link RenderMode} that the piece belongs to
     */
    public CompressedPiece(@NonNull ByteBuf src, @NonNull RenderMode mode) {
        this(uncheckedCast(mode.readPos(src)), src);
    }

    /**
     * Reads a piece with no prefix from the given {@link ByteBuf}.
     *
     * @param pos the piece position
     * @param src the {@link ByteBuf} to read from
     */
    public CompressedPiece(@NonNull POS pos, @NonNull ByteBuf src) {
        this(pos);

        this.extra = readVarLong(src);
        this.timestamp = readVarLongZigZag(src);

        int len = readVarInt(src) - 1;
        if (len >= 0) { //if length is -1 (0 on disk) there is no data
            src.readBytes(this.data = new byte[len]);
        }
    }

    /**
     * Writes this piece to the given {@link ByteBuf}, prefixed with the render mode and piece position.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    public void writeWithModeAndPos(@NonNull ByteBuf dst) {
        dst.writeByte(this.pos.mode().ordinal());
        this.writeWithPos(dst);
    }

    /**
     * Writes this piece to the given {@link ByteBuf}, prefixed with the piece position.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    public void writeWithPos(@NonNull ByteBuf dst) {
        this.pos.writePos(dst);
        this.write(dst);
    }

    /**
     * Writes this piece to the given {@link ByteBuf} with no prefix.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    public void write(@NonNull ByteBuf dst) {
        this.readLock().lock();
        try {
            writeVarLong(dst, this.extra);
            writeVarLongZigZag(dst, this.timestamp);

            byte[] data = this.data;
            if (data == null) {
                writeVarInt(dst, 0);
            } else {
                writeVarInt(dst, data.length + 1);
                dst.writeBytes(data);
            }
        } finally {
            this.readLock().unlock();
        }
    }

    /**
     * @return whether or not this piece is blank (i.e. has not been generated)
     */
    public boolean isBlank() {
        return this.timestamp() == CompressedPiece.PIECE_BLANK;
    }

    /**
     * @return whether or not this piece is done (i.e. has been fully generated and may be saved)
     */
    public boolean isDone() {
        return this.timestamp() >= CompressedPiece.PIECE_ROUGH_COMPLETE;
    }

    /**
     * @return whether or not this piece is empty, or has been generated but still has no contents
     */
    public boolean todo_renameTo_isEmpty() {
        return this.data == null;
    }

    /**
     * @return the inflated, parsed piece
     */
    public P inflate() {
        this.readLock().lock();
        try {
            checkState(!this.isBlank(), "piece hasn't been generated!");

            byte[] data = this.data;
            if (data == null) {
                return null;
            }

            ByteBuf compressed = Unpooled.wrappedBuffer(data);
            ByteBuf uncompressed = Constants.allocateByteBufExactly(Zstd.PROVIDER.frameContentSize(compressed));
            try {
                checkState(ZSTD_INF.get().decompress(compressed, uncompressed));
                return uncheckedCast(this.pos.mode().readPiece(uncompressed));
            } finally {
                uncompressed.release();
                //no reason to release the wrapped buffer lmao
            }
        } finally {
            this.readLock().unlock();
        }
    }

    public void set(long timestamp, @NonNull B builder) throws IllegalArgumentException {
        //before doing anything we compress the piece data into an array
        byte[] data = null;
        ByteBuf compressed = null;
        COMPRESS:
        try {
            ByteBuf uncompressed = ByteBufAllocator.DEFAULT.buffer();
            try {
                if (builder.write(uncompressed))    { //write piece
                    break COMPRESS; //skip compression if builder is empty
                }
                compressed = Constants.allocateByteBufExactly(Zstd.PROVIDER.compressBound(uncompressed.readableBytes())); //allocate dst buffer
                checkState(ZSTD_DEF.get().compress(uncompressed, compressed)); //compress piece
            } finally {
                uncompressed.release(); //discard uncompressed data
            }

            compressed.readBytes(data = new byte[compressed.readableBytes()]);
        } finally {
            if (compressed != null) {
                compressed.release();
            }
        }

        this.writeLock().lock();
        try {
            long current = this.timestamp;
            checkArg(timestamp > current, "new timestamp (%d) must be greater than current timestamp (%d)!", timestamp, current);
            this.timestamp = timestamp;
            this.extra = builder.extra();
            this.data = data;
        } finally {
            this.writeLock().unlock();
        }
    }
}
