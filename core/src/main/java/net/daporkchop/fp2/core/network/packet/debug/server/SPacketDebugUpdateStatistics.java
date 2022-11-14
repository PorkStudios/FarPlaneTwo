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

package net.daporkchop.fp2.core.network.packet.debug.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
public class SPacketDebugUpdateStatistics implements IPacket {
    protected DebugStats.Tracking tracking;

    @Override
    public void read(@NonNull DataIn in) throws IOException {
        this.tracking = DebugStats.Tracking.builder()
                .tilesLoaded(in.readLongLE())
                .tilesLoading(in.readLongLE())
                .tilesQueued(in.readLongLE())
                .tilesTrackedGlobal(in.readLongLE())
                .avgUpdateDuration(in.readLongLE())
                .lastUpdateDuration(in.readLongLE())
                .build();
    }

    @Override
    public void write(@NonNull DataOut out) throws IOException {
        out.writeLongLE(this.tracking.tilesLoaded());
        out.writeLongLE(this.tracking.tilesLoading());
        out.writeLongLE(this.tracking.tilesQueued());
        out.writeLongLE(this.tracking.tilesTrackedGlobal());
        out.writeLongLE(this.tracking.avgUpdateDuration());
        out.writeLongLE(this.tracking.lastUpdateDuration());
    }
}
