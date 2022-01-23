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
 *
 */

package net.daporkchop.fp2.mode.common.client.bake.indexed;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.mode.common.client.bake.AbstractBakeOutput;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;

import java.util.stream.Stream;

/**
 * Implementation of {@link IBakeOutput} which contains indexed geometry in multiple render passes.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class IndexedBakeOutput<SG, SL> extends AbstractBakeOutput {
    //TODO: this will only store a single value, there's no reason to use an AttributeWriter...
    @NonNull
    protected final AttributeWriter<SG> globals;

    @NonNull
    protected final AttributeWriter<SL> verts;

    @NonNull
    protected final IndexWriter[] indices;

    @Override
    protected void doRelease() {
        this.globals.close();
        this.verts.close();
        for (IndexWriter writer : this.indices) {
            writer.close();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.verts.size() == 0 || Stream.of(this.indices).allMatch(writer -> writer.size() == 0);
    }
}
