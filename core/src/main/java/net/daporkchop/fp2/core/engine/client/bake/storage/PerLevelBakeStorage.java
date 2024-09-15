/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.client.bake.storage;

import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.BakeOutput;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.primitive.lambda.IntBoolObjFunction;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Implementation of {@link BakeStorage} which delegates to a different bake storage instance at each detail level.
 *
 * @author DaPorkchop_
 */
public final class PerLevelBakeStorage<VertexType extends AttributeStruct> extends BakeStorage<VertexType> {
    private final BakeStorage<VertexType>[] storages = uncheckedCast(new BakeStorage[MAX_LODS]);

    public PerLevelBakeStorage(OpenGL gl, BufferUploader bufferUploader, AttributeFormat<VertexType> vertexFormat, IndexFormat indexFormat, boolean absoluteIndices,
                               IntBoolObjFunction<? extends BakeStorage<VertexType>> storageFactory) {
        super(gl, bufferUploader, vertexFormat, indexFormat, absoluteIndices);

        try {
            for (int level = 0; level < MAX_LODS; level++) {
                this.storages[level] = Objects.requireNonNull(storageFactory.apply(level, absoluteIndices));
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(this.storages);
    }

    @Override
    public void update(Map<TilePos, BakeOutput<VertexType>> changes) {
        if (changes.isEmpty()) {
            return;
        }

        //group all the changes by detail level
        Map<TilePos, BakeOutput<VertexType>>[] grouped = uncheckedCast(new Map[MAX_LODS]);
        changes.forEach((pos, output) -> {
            int level = pos.level();
            if (grouped[level] == null) {
                grouped[level] = DirectTilePosAccess.newPositionKeyedHashMap();
            }
            grouped[level].put(pos, output);
        });

        //for each detail level with changes, forward the corresponding updates to the corresponding storage
        for (int level = 0; level < MAX_LODS; level++) {
            if (grouped[level] != null) {
                this.storages[level].update(grouped[level]);
                grouped[level] = null;
            }
        }
    }

    @Override
    public Location[] find(TilePos pos) {
        return this.storages[pos.level()].find(pos);
    }

    @Override
    public AttributeBuffer<VertexType> vertexBuffer(int level, int pass) {
        return this.storages[level].vertexBuffer(level, pass);
    }

    @Override
    public IndexBuffer indexBuffer(int level, int pass) {
        return this.storages[level].indexBuffer(level, pass);
    }

    @Override
    public DebugStats.Renderer stats() {
        return Arrays.stream(this.storages).map(BakeStorage::stats).reduce(DebugStats.Renderer::add).get();
    }
}
