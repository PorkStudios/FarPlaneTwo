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

package net.daporkchop.fp2.core.engine.client.index.attribdivisor;

import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractMultiDrawIndirectRenderIndex<VertexType extends AttributeStruct> extends AbstractBaseInstanceRenderIndex<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_multi_draw_indirect);

    public AbstractMultiDrawIndirectRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        super(gl, bakeStorage, alloc, sharedVertexFormat);
    }

    /**
     * @author DaPorkchop_
     */
    protected static final class DirectCommandList implements AutoCloseable {
        private final DirectMemoryAllocator alloc;

        public long address;
        public int size;
        public int capacity = 16;

        public DirectCommandList(DirectMemoryAllocator alloc) {
            this.alloc = alloc;
            this.address = this.alloc.alloc(this.capacity * DrawElementsIndirectCommand._SIZE);
        }

        @Override
        public void close() {
            this.alloc.free(this.address);
            this.address = 0L;
            this.size = this.capacity = 0;
        }

        public void add(DrawElementsIndirectCommand command) {
            if (this.size == this.capacity) {
                this.address = this.alloc.realloc(this.address, (this.capacity <<= 1) * DrawElementsIndirectCommand._SIZE);
            }
            DrawElementsIndirectCommand._set(this.address + (this.size++ * DrawElementsIndirectCommand._SIZE), command);
        }
    }
}
