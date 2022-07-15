/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.mode.api.tile;

import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.util.recycler.Recycler;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;

import java.io.IOException;

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

    public TileSnapshot(@NonNull DataIn in, @NonNull POS pos) throws IOException {
        this.pos = pos;
        this.timestamp = in.readVarLongZigZag();

        int len = in.readVarIntZigZag();
        if (len < 0) { //no data!
            this.data = null;
        } else {
            this.data = new byte[len];
            in.readFully(this.data);
        }
    }

    public void write(@NonNull DataOut out) throws IOException {
        out.writeVarLongZigZag(this.timestamp);

        if (this.data == null) { //no data!
            out.writeVarIntZigZag(-1);
        } else { //tile data is present, write it
            out.writeVarIntZigZag(this.data.length);
            out.write(this.data);
        }
    }

    @Override
    public T loadTile(@NonNull Recycler<T> recycler) {
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
