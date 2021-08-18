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

package net.daporkchop.fp2.client.gl.indirect;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * @author DaPorkchop_
 */
public interface IDrawIndirectCommandBuffer<C extends IDrawIndirectCommand> extends RefCounted {
    // general methods

    /**
     * @return the size of a single command, in bytes
     */
    long commandSize();

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
    long capacity();

    /**
     * Sets the capacity of this command buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with empty commands.
     *
     * @param capacity the new capacity
     */
    void resize(long capacity);

    /**
     * Loads the command at the given index into the given {@link C} instance.
     *
     * @param command the instance for the command to be loaded into
     * @param index   the index to load the command from
     */
    void load(@NonNull C command, long index);

    /**
     * Stores the command from the given {@link C} instance at the given index.
     *
     * @param command the instance for the command to be stored
     * @param index   the index to store the command at
     */
    void store(@NonNull C command, long index);

    /**
     * Clears the given number of commands starting at the given index.
     *
     * @param index the index of the first command to clear
     * @param count the number of commands to be cleared
     */
    void clearRange(long index, long count);

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
    default void draw() {
        this.draw(0L, 1L, this.capacity());
    }

    /**
     * Executes some of the draw commands in this command buffer.
     * <p>
     * This will start with the {@code offset}-th command and continue advancing in intervals of {@code stride} until {@code count} commands have been executed.
     *
     * @param offset the index of the first command to be drawn
     * @param stride the number of commands to advance by between each executed command
     * @param count  the number of commands to draw
     */
    void draw(long offset, long stride, long count);

    @Override
    int refCnt();

    @Override
    IDrawIndirectCommandBuffer<C> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
