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

package net.daporkchop.fp2.gl.command.buffer;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLResource;
import net.daporkchop.fp2.gl.command.DrawCommand;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.shader.ShaderProgram;

import java.util.function.IntPredicate;

/**
 * @author DaPorkchop_
 */
public interface DrawCommandBuffer<C extends DrawCommand> extends GLResource {
    /**
     * @return the number of commands that this buffer can store
     */
    int capacity();

    /**
     * Sets the capacity of this command buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with empty commands.
     *
     * @param capacity the new capacity
     */
    void resize(int capacity);

    /**
     * Sets the command at the given index to the given command.
     *
     * @param index   the command index
     * @param command the command
     */
    void set(int index, @NonNull C command);

    /**
     * Gets the command at the given index.
     *
     * @param index the command index
     * @return the command
     */
    C get(int index);

    /**
     * Sets the command at the given index to an empty command.
     *
     * @param index the command index
     */
    void clear(int index);

    /**
     * Executes every command in this buffer.
     */
    void execute(@NonNull DrawMode mode, @NonNull ShaderProgram shader);

    /**
     * Executes every command in this buffer for which the given {@link IntPredicate} returns {@code true}.
     * <p>
     * The {@link IntPredicate} will be called with the index of each command. Empty commands may be skipped.
     *
     * @param selector the {@link IntPredicate}
     */
    void execute(@NonNull DrawMode mode, @NonNull ShaderProgram shader, @NonNull IntPredicate selector);
}
