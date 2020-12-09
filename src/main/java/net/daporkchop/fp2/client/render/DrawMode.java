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

package net.daporkchop.fp2.client.render;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.mode.common.client.FarRenderIndex;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.string.PUnsafeStrings;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public enum DrawMode implements AutoCloseable {
    MULTIDRAW {
        @Override
        public DrawMode start(@NonNull FarRenderIndex index, @NonNull GLBuffer commandBuffer, @NonNull IShaderHolder shaders) {
            super.start(index, commandBuffer, shaders);
            commandBuffer.bind(GL_DRAW_INDIRECT_BUFFER);
            return this;
        }

        @Override
        public void close() {
            this.commandBuffer.close();
            super.close();
        }

        @Override
        public void draw(@NonNull RenderPass pass, int passNumber) {
            int tileCount = this.index.upload(passNumber, this.commandBuffer);
            if (tileCount > 0) {
                pass.render(this, tileCount);
            }
        }

        @Override
        void draw0(int tileCount) {
            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, FarRenderIndex.POSITION_SIZE_BYTES, tileCount, FarRenderIndex.ENTRY_SIZE_BYTES);
        }
    };

    protected FarRenderIndex index;
    protected GLBuffer commandBuffer;
    protected IShaderHolder shaders;

    DrawMode() {
        String name = PStrings.clone(this.name());
        PUnsafeStrings.titleFormat(name);
        PUnsafeStrings.setEnumName(this, name.intern());
    }

    public DrawMode start(@NonNull FarRenderIndex index, @NonNull GLBuffer commandBuffer, @NonNull IShaderHolder shaders) {
        this.index = index;
        this.commandBuffer = commandBuffer;
        this.shaders = shaders;
        return this;
    }

    @Override
    public void close() {
        this.index = null;
        this.commandBuffer = null;
        this.shaders = null;
    }

    public abstract void draw(@NonNull RenderPass pass, int passNumber);

    abstract void draw0(int tileCount);
}
