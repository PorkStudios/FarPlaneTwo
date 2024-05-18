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

package net.daporkchop.fp2.core.engine.client.bake;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.fp2.gl.draw.index.NewIndexWriter;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Post-processes an {@link IRenderBaker}'s output by duplicating the vertices in the stream, so that the index buffers are a sequence of uniformly ascending integers, and
 * no vertex is referenced by more than one index entry.
 * <p>
 * The resulting vertex buffer could also be used for array rendering.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class UnindexingBaker<VertexType extends AttributeStruct> implements IRenderBaker<VertexType> {
    @NonNull
    private final IRenderBaker<VertexType> delegate;
    @NonNull
    private final DirectMemoryAllocator alloc;

    @Override
    public Stream<TilePos> bakeOutputs(@NonNull TilePos pos) {
        return this.delegate.bakeOutputs(pos);
    }

    @Override
    public Stream<TilePos> bakeInputs(@NonNull TilePos pos) {
        return this.delegate.bakeInputs(pos);
    }

    @Override
    public void bake(@NonNull TilePos pos, @NonNull Tile[] srcs, @NonNull BakeOutput<VertexType> output) {
        this.delegate.bake(pos, srcs, output);
        if (output.isEmpty()) { //we don't need to do anything
            return;
        }

        val origVerts = output.verts;
        try (NewAttributeWriter<VertexType> copiedVerts = origVerts.format().createWriter(this.alloc);
             NewIndexWriter copiedIndices = output.indicesPerPass[0].format().createWriter(this.alloc)) {

            copiedVerts.appendUninitialized(origVerts.size());
            origVerts.copyTo(0, copiedVerts, 0, origVerts.size());
            origVerts.clear();
            origVerts.reserve(Arrays.stream(output.indicesPerPass).mapToInt(NewIndexWriter::size).sum());

            int outputIndex = 0;
            for (val origIndices : output.indicesPerPass) {
                if (origIndices.size() == 0) {
                    continue;
                }

                copiedIndices.clear();
                copiedIndices.appendUninitialized(origIndices.size());
                origIndices.copyTo(0, copiedIndices, 0, origIndices.size());
                origIndices.clear();

                for (int i = 0; i < copiedIndices.size(); i++) {
                    origIndices.append(outputIndex);

                    origVerts.appendUninitialized();
                    copiedVerts.copyTo(copiedIndices.get(i), origVerts, outputIndex);
                    outputIndex++;
                }
            }
        }
    }
}
