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

package net.daporkchop.fp2.core.network.packet.standard.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;

import java.io.IOException;

/**
 * Sent by the server to tell the client to get ready to initiate a new session.
 *
 * @author DaPorkchop_
 */
@Getter
@Setter
public class SPacketSessionBegin implements IPacket {
    @NonNull
    protected IntAxisAlignedBB[] coordLimits;

    @Override
    public void read(@NonNull DataIn in) throws IOException {
        int len = in.readIntLE();

        this.coordLimits = new IntAxisAlignedBB[len];
        for (int i = 0; i < len; i++) {
            this.coordLimits[i] = new IntAxisAlignedBB(in.readIntLE(), in.readIntLE(), in.readIntLE(), in.readIntLE(), in.readIntLE(), in.readIntLE());
        }
    }

    @Override
    public void write(@NonNull DataOut out) throws IOException {
        out.writeIntLE(this.coordLimits.length);

        for (IntAxisAlignedBB bb : this.coordLimits) {
            out.writeIntLE(bb.minX());
            out.writeIntLE(bb.minY());
            out.writeIntLE(bb.minZ());
            out.writeIntLE(bb.maxX());
            out.writeIntLE(bb.maxY());
            out.writeIntLE(bb.maxZ());
        }
    }
}
