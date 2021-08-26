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

package net.daporkchop.fp2.client.gl.command;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * @author DaPorkchop_
 */
public interface IDrawCommandBuffer<C extends IDrawCommand> extends RefCounted {
    // general methods

    /**
     * @return a new {@link C} compatible with the draw commands stored in this command buffer
     */
    C newCommand();

    /**
     * @return the concrete class for type {@link C}
     */
    Class<C> commandClass();

    // contents

    /**
     * @return the number of commands in this buffer
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
     * Loads the command at the given index into the given {@link C} instance.
     *
     * @param command the instance for the command to be loaded into
     * @param index   the index to load the command from
     */
    void load(@NonNull C command, int index);

    /**
     * Stores the command from the given {@link C} instance at the given index.
     *
     * @param command the instance for the command to be stored
     * @param index   the index to store the command at
     */
    void store(@NonNull C command, int index);

    /**
     * Clears the given number of commands starting at the given index.
     *
     * @param index the index of the first command to clear
     * @param count the number of commands to be cleared
     */
    void clearRange(int index, int count);

    /**
     * @return an OpenGL buffer containing the tightly packed draw commands
     */
    default GLBuffer openglBuffer() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName());
    }

    // drawing

    /**
     * Executes the draw commands in this command buffer.
     */
    void draw();

    @Override
    int refCnt();

    @Override
    IDrawCommandBuffer<C> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
