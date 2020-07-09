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

package net.daporkchop.fp2.strategy.heightmap;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.util.Constants;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A "chunk" containing the data used by the heightmap rendering strategy.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class HeightmapChunk implements IMessage {
    public static void checkCoords(int x, int z) {
        checkArg(x >= 0 && x < HEIGHT_VERTS && z >= 0 && z < HEIGHT_VERTS, "coordinates out of bounds (x=%d, z=%d)", x, z);
    }

    protected final int x;
    protected final int z;

    protected final IntBuffer height = Constants.createIntBuffer(HEIGHT_VERTS * HEIGHT_VERTS);
    protected final IntBuffer color = Constants.createIntBuffer(HEIGHT_VERTS * HEIGHT_VERTS);
    protected final ByteBuffer biome = Constants.createByteBuffer(HEIGHT_VERTS * HEIGHT_VERTS);

    public int height(int x, int z) {
        checkCoords(x, z);
        return this.height.get(x * HEIGHT_VERTS + z);
    }

    public int color(int x, int z) {
        checkCoords(x, z);
        return this.color.get(x * HEIGHT_VERTS + z);
    }

    public int biome(int x, int z) {
        checkCoords(x, z);
        return this.biome.get(x * HEIGHT_VERTS + z) & 0xFF;
    }

    public HeightmapChunk height(int x, int z, int height) {
        checkCoords(x, z);
        this.height.put(x * HEIGHT_VERTS + z, height);
        return this;
    }

    public HeightmapChunk color(int x, int z, int color) {
        checkCoords(x, z);
        this.color.put(x * HEIGHT_VERTS + z, color);
        return this;
    }

    public HeightmapChunk biome(int x, int z, int biome) {
        checkCoords(x, z);
        this.biome.put(x * HEIGHT_VERTS + z, (byte) biome);
        return this;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        for (int i = 0, capacity = this.height.capacity(); i < capacity; i++) {
            this.height.put(i, buf.readInt());
        }
        for (int i = 0, capacity = this.color.capacity(); i < capacity; i++) {
            this.color.put(i, buf.readInt());
        }
        for (int i = 0, capacity = this.biome.capacity(); i < capacity; i++) {
            this.biome.put(i, buf.readByte());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.ensureWritable((this.height.capacity() + this.color.capacity()) * 4 + this.biome.capacity());
        for (int i = 0, capacity = this.height.capacity(); i < capacity; i++) {
            buf.writeInt(this.height.get(i));
        }
        for (int i = 0, capacity = this.color.capacity(); i < capacity; i++) {
            buf.writeInt(this.color.get(i));
        }
        for (int i = 0, capacity = this.biome.capacity(); i < capacity; i++) {
            buf.writeByte(this.biome.get(i));
        }
    }
}
