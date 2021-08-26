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

package net.daporkchop.fp2.client.gl.object;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.NonNull;

import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * @author DaPorkchop_
 */
public final class VertexArrayObject extends GLObject {
    private static VertexArrayObject CURRENTLY_BOUND = null;

    private final Int2ObjectMap<IGLBuffer> dependencies = new Int2ObjectOpenHashMap<>();
    private boolean changed;

    public VertexArrayObject() {
        this(glGenVertexArrays());
    }

    public VertexArrayObject(int id) {
        super(id);
    }

    public VertexArrayObject bind() {
        checkState(CURRENTLY_BOUND == null, "a vertex array object is already bound!");
        CURRENTLY_BOUND = this;

        glBindVertexArray(this.id);
        return this;
    }

    public VertexArrayObject bindForChange() {
        this.bind();

        this.dependencies.clear();
        this.changed = true;

        return this;
    }

    @Override
    public void close() {
        checkState(CURRENTLY_BOUND == this, "not bound!");
        CURRENTLY_BOUND = null;

        glBindVertexArray(0);

        if (this.changed) {
            this.changed = false;

            if (this.dependencies.containsKey(-1)) {
                this.dependencies.get(-1).close();
            }

            for (int i : this.dependencies.keySet()) {
                if (i >= 0) {
                    glDisableVertexAttribArray(i);
                }
            }
        }
    }

    @Override
    protected Runnable delete(int id) {
        return () -> glDeleteVertexArrays(id);
    }

    public void attrI(@NonNull IGLBuffer buffer, int size, int type, int stride, long offset, int divisor) {
        this.nextIndex(buffer, idx -> {
            glVertexAttribIPointer(idx, size, type, stride, offset);
            if (divisor != 0) {
                glVertexAttribDivisor(idx, divisor);
            }
        });
    }

    public void attrF(@NonNull IGLBuffer buffer, int size, int type, boolean normalized, int stride, long offset, int divisor) {
        this.nextIndex(buffer, idx -> {
            glVertexAttribPointer(idx, size, type, normalized, stride, offset);
            if (divisor != 0) {
                glVertexAttribDivisor(idx, divisor);
            }
        });
    }

    public VertexArrayObject putElementArray(@NonNull IGLBuffer dependency) {
        checkState(CURRENTLY_BOUND == this, "not bound!");

        this.dependencies.put(-1, dependency.bind(GL_ELEMENT_ARRAY_BUFFER));
        return this;
    }

    protected void nextIndex(@NonNull IGLBuffer bufferIn, @NonNull IntConsumer callback) {
        checkState(CURRENTLY_BOUND == this, "not bound!");

        try (IGLBuffer buffer = bufferIn.bind(GL_ARRAY_BUFFER)) {
            for (int i = 0; ; i++) {
                if (this.dependencies.putIfAbsent(i, buffer) == null) {
                    glEnableVertexAttribArray(i);
                    callback.accept(i);
                    return;
                }
            }
        }
    }
}
