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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.NonNull;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public final class VertexArrayObject extends GLObject<VertexArrayObject> {
    private final Int2ObjectMap<GLObject> dependencies = new Int2ObjectOpenHashMap<>();

    public VertexArrayObject() {
        this(glGenVertexArrays());
    }

    public VertexArrayObject(int id) {
        super(id);
    }

    public VertexArrayObject bind() {
        glBindVertexArray(this.id);
        return this;
    }

    @Override
    public void close() {
        glBindVertexArray(0);
    }

    @Override
    protected Runnable delete(int id) {
        return () -> glDeleteVertexArrays(id);
    }

    public VertexArrayObject putDependency(int location, @NonNull GLBuffer dependency) {
        this.dependencies.put(notNegative(location, "location"), dependency);
        return this;
    }

    public VertexArrayObject removeDependency(int location) {
        this.dependencies.remove(notNegative(location, "location"));
        return this;
    }

    public VertexArrayObject putElementArray(@NonNull GLBuffer dependency) {
        this.dependencies.put(-1, dependency);
        return this;
    }

    public VertexArrayObject removeElementArray() {
        this.dependencies.remove(-1);
        return this;
    }
}
