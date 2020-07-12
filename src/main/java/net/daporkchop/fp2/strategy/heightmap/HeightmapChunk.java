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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.strategy.RenderStrategy;
import net.daporkchop.fp2.strategy.common.IFarChunk;
import net.daporkchop.fp2.strategy.common.IFarChunkPos;
import net.daporkchop.fp2.util.Constants;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public class HeightmapChunk implements IFarChunk {
    public static void checkCoords(int x, int z) {
        checkArg(x >= 0 && x < HEIGHT_VERTS && z >= 0 && z < HEIGHT_VERTS, "coordinates out of bounds (x=%d, z=%d)", x, z);
    }

    protected final int x;
    protected final int z;

    @Getter(AccessLevel.NONE)
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected final IntBuffer height = Constants.createIntBuffer(HEIGHT_VERTS * HEIGHT_VERTS);
    protected final IntBuffer color = Constants.createIntBuffer(HEIGHT_VERTS * HEIGHT_VERTS);
    protected final ByteBuffer biome = Constants.createByteBuffer(HEIGHT_VERTS * HEIGHT_VERTS);
    protected final ShortBuffer block = Constants.createShortBuffer(HEIGHT_VERTS * HEIGHT_VERTS);
    protected final IntBuffer light = Constants.createIntBuffer(HEIGHT_VERTS * HEIGHT_VERTS);

    public HeightmapChunk(@NonNull ByteBuf buf) {
        this(buf.readInt(), buf.readInt());
        for (int i = 0, capacity = this.height.capacity(); i < capacity; i++) {
            this.height.put(i, buf.readInt());
        }
        for (int i = 0, capacity = this.color.capacity(); i < capacity; i++) {
            this.color.put(i, buf.readInt());
        }
        for (int i = 0, capacity = this.biome.capacity(); i < capacity; i++) {
            this.biome.put(i, buf.readByte());
        }
        for (int i = 0, capacity = this.block.capacity(); i < capacity; i++) {
            this.block.put(i, buf.readShort());
        }
        for (int i = 0, capacity = this.light.capacity(); i < capacity; i++) {
            this.light.put(i, buf.readInt());
        }
    }

    @Override
    public void write(@NonNull ByteBuf buf) {
        buf.writeInt(this.x).writeInt(this.z);
        for (int i = 0, capacity = this.height.capacity(); i < capacity; i++) {
            buf.writeInt(this.height.get(i));
        }
        for (int i = 0, capacity = this.color.capacity(); i < capacity; i++) {
            buf.writeInt(this.color.get(i));
        }
        for (int i = 0, capacity = this.biome.capacity(); i < capacity; i++) {
            buf.writeByte(this.biome.get(i));
        }
        for (int i = 0, capacity = this.block.capacity(); i < capacity; i++) {
            buf.writeShort(this.block.get(i));
        }
        for (int i = 0, capacity = this.light.capacity(); i < capacity; i++) {
            buf.writeInt(this.light.get(i));
        }
    }

    @Override
    public RenderStrategy strategy() {
        return RenderStrategy.HEIGHTMAP;
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

    public HeightmapChunk block(int x, int z, int block) {
        checkCoords(x, z);
        this.block.put(x * HEIGHT_VERTS + z, (short) block);
        return this;
    }

    public HeightmapChunk light(int x, int z, int light) {
        checkCoords(x, z);
        this.light.put(x * HEIGHT_VERTS + z, light);
        return this;
    }

    @Override
    public HeightmapChunkPos pos() {
        return new HeightmapChunkPos(this.x, this.z);
    }

    @Override
    public Lock readLock() {
        return this.lock.readLock();
    }

    @Override
    public Lock writeLock() {
        return this.lock.writeLock();
    }
}
