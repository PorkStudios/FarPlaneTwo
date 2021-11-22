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

package net.daporkchop.fp2.mode.api.tile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.debug.util.DebugStats;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.core.util.SimpleRecycler;
import net.daporkchop.fp2.util.annotation.DebugOnly;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class TileSnapshot<POS extends IFarPos, T extends IFarTile> implements ITileSnapshot<POS, T> {
    @NonNull
    protected final POS pos;
    protected final long timestamp;

    @Getter(AccessLevel.NONE)
    protected final byte[] data;

    public TileSnapshot(@NonNull ByteBuf src, @NonNull IFarRenderMode<POS, T> mode) {
        this.pos = mode.readPos(src);
        this.timestamp = src.readLongLE();

        int len = src.readIntLE();
        if (len < 0) { //no data!
            this.data = null;
        } else { //tile data is non-empty, read it
            this.data = new byte[len];
            src.readBytes(this.data);
        }
    }

    public void write(@NonNull ByteBuf dst) {
        this.pos.writePos(dst);
        dst.writeLongLE(this.timestamp);

        if (this.data == null) { //no data!
            dst.writeIntLE(-1);
        } else { //tile data is present, write it to the buffer
            dst.writeIntLE(this.data.length).writeBytes(this.data);
        }
    }

    @Override
    public T loadTile(@NonNull SimpleRecycler<T> recycler) {
        if (this.data != null) {
            T tile = recycler.allocate();
            tile.read(Unpooled.wrappedBuffer(this.data));
            return tile;
        } else {
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.data == null;
    }

    @Override
    public ITileSnapshot<POS, T> compressed() {
        return new CompressedTileSnapshot<>(this);
    }

    @Override
    public ITileSnapshot<POS, T> uncompressed() {
        return this; //we're already uncompressed!
    }

    @DebugOnly
    @Override
    public DebugStats.TileSnapshot stats() {
        if (this.data == null) { //this tile is empty!
            return DebugStats.TileSnapshot.ZERO;
        } else {
            return DebugStats.TileSnapshot.builder()
                    .allocatedSpace(this.data.length)
                    .totalSpace(this.data.length)
                    .uncompressedSize(this.data.length)
                    .build();
        }
    }
}
