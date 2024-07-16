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

package net.daporkchop.fp2.core.engine.client.index.postable;

import lombok.val;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.Objects;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;

/**
 * Implementation of {@link RenderPosTable} which delegates to a different render position table instance at each detail level.
 *
 * @author DaPorkchop_
 */
public final class PerLevelRenderPosTable extends RenderPosTable {
    private final RenderPosTable[] tables = new RenderPosTable[MAX_LODS];

    public PerLevelRenderPosTable(IntFunction<? extends RenderPosTable> tableFactory) {
        try {
            for (int level = 0; level < MAX_LODS; level++) {
                this.tables[level] = Objects.requireNonNull(tableFactory.apply(level));
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(this.tables);
    }

    @Override
    public int add(TilePos pos) {
        return this.tables[pos.level()].add(pos);
    }

    @Override
    public int remove(TilePos pos) {
        return this.tables[pos.level()].remove(pos);
    }

    @Override
    public void flush() {
        for (val table : this.tables) {
            table.flush();
        }
    }

    @Override
    public AttributeBuffer<VoxelGlobalAttributes> vertexBuffer(int level) {
        return this.tables[level].vertexBuffer(level);
    }
}
