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

package net.daporkchop.fp2.mode.common;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.CompressedPiece;
import net.daporkchop.fp2.mode.api.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarPiece<POS extends IFarPos> implements IFarPiece<POS> {
    protected static final long TIMESTAMP_OFFSET = PUnsafe.pork_getOffset(AbstractFarPiece.class, "timestamp");
    protected static final long DIRTY_OFFSET = PUnsafe.pork_getOffset(AbstractFarPiece.class, "dirty");

    protected final POS pos;
    protected final RenderMode mode;

    protected final Lock readLock;
    protected final Lock writeLock;

    protected volatile long timestamp = CompressedPiece.PIECE_EMPTY;
    @Getter(AccessLevel.NONE)
    protected transient volatile int dirty = 0;

    public AbstractFarPiece(@NonNull POS pos, @NonNull RenderMode mode) {
        this.pos = pos;
        this.mode = mode;

        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    public AbstractFarPiece(@NonNull ByteBuf src, @NonNull RenderMode mode) {
        this(PorkUtil.<POS>uncheckedCast(mode.readPos(src)), mode);

        this.timestamp = src.readLong();
    }

    @Override
    public void writePiece(@NonNull ByteBuf dst) {
        long timestamp = this.timestamp;
        checkState(timestamp != CompressedPiece.PIECE_EMPTY, "piece (%s) does not contain any data! %d", this.pos, timestamp);
        this.pos.writePos(dst);

        dst.writeLong(timestamp);

        this.writeBody(dst);
    }

    protected abstract void writeBody(@NonNull ByteBuf dst);

    @Override
    public void updateTimestamp(long timestamp) throws IllegalArgumentException {
        long current;
        do {
            current = PUnsafe.getLongVolatile(this, TIMESTAMP_OFFSET);
            checkArg(timestamp > current, "new timestamp (%d) must be greater than current timestamp (%d)!", timestamp, current);
        } while (!PUnsafe.compareAndSwapLong(this, TIMESTAMP_OFFSET, current, timestamp));
    }

    @Override
    public boolean isDirty() {
        return this.dirty != 0;
    }

    @Override
    public void markDirty() {
        this.dirty = 1;
    }

    @Override
    public boolean clearDirty() {
        return PUnsafe.compareAndSwapInt(this, DIRTY_OFFSET, 1, 0);
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("%s(pos=%s, timestamp=%d, dirty=%b)", this.getClass().getName(), this.pos, this.timestamp, this.isDirty());
    }
}
