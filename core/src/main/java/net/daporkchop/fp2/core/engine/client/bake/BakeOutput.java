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

import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.fp2.gl.draw.index.NewIndexWriter;

import java.util.Arrays;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class BakeOutput<VertexType extends AttributeStruct> implements AutoCloseable {
    public final NewAttributeWriter<VertexType> verts;
    public final NewIndexWriter[] indicesPerPass;

    public boolean isEmpty() {
        return this.verts.size() == 0 || Arrays.stream(this.indicesPerPass).allMatch(writer -> writer.size() == 0);
    }

    @Override
    public void close() {
        this.verts.close();
        for (val indices : this.indicesPerPass) {
            indices.close();
        }
    }
}
