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

package net.daporkchop.fp2.client.gl.object;

import static net.daporkchop.lib.common.util.PorkUtil.*;
import static net.minecraft.client.renderer.OpenGlHelper.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

/**
 * @author DaPorkchop_
 */
public abstract class GLBufferObject<T extends GLBufferObject<T>> extends GLObject<T> {
    protected final int target = this.target();

    public GLBufferObject() {
        this(glGenBuffers());
    }

    public GLBufferObject(int id) {
        super(id);
    }

    protected abstract int target();

    @Override
    public T bind() {
        glBindBuffer(this.target, this.id);
        return uncheckedCast(this);
    }

    @Override
    public void close() {
        glBindBuffer(this.target, 0);
    }

    @Override
    protected Runnable delete(int id) {
        return () -> glDeleteBuffers(id);
    }

    public void bindSSBO(int index) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, this.id);
    }
}
