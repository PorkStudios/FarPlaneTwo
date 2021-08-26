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

package net.daporkchop.fp2.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.command.elements.DrawElementsCommand;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.util.alloc.Allocator;

import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class IndexedMultidrawRenderStrategy<POS extends IFarPos, T extends IFarTile> extends IndexedRenderStrategy<POS, T> implements IIndexedMultipassRenderStrategy<POS, T> {
    public IndexedMultidrawRenderStrategy(@NonNull Allocator alloc, @NonNull VertexFormat vertexFormat) {
        super(alloc, vertexFormat);
    }

    @Override
    public void toDrawCommands(long renderData, @NonNull DrawElementsCommand[] commands) {
        int baseVertex = toInt(_renderdata_vertexOffset(renderData));
        int firstIndex = toInt(_renderdata_indexOffset(renderData));

        for (int i = 0; i < RENDER_PASS_COUNT; i++) {
            int count = _renderdata_indexCount(renderData, i);
            commands[i].baseVertex(baseVertex)
                    .firstIndex(firstIndex)
                    .count(count);
            firstIndex += count;
        }
    }
}
